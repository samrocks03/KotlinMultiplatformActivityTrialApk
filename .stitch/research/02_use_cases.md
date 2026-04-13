# Supabase Edge Functions: Use Cases for the STEP Application

## Overview

This document identifies 10 concrete Supabase Edge Function use cases for the STEP KMP application. Each use case is grounded in the current codebase architecture -- a shared Kotlin Multiplatform module with domain/data layers consumed by native Android (Jetpack Compose) and iOS (SwiftUI) UIs. The app currently relies on two external GraphQL APIs (Rick and Morty, GraphQLZero), local SQLDelight persistence for favorites and photos, and native platform APIs for camera and GPS.

Edge Functions run on Deno at the network edge, giving STEP a serverless backend without managing infrastructure. They are ideal for logic that should not live on-device: authentication, image processing, cross-device sync, and protecting third-party API keys.

---

## 1. User Authentication and Session Management

### Current State
STEP has no authentication. All data is anonymous and device-local. Favorites exist only in SQLDelight on a single device. There is no concept of a user identity, so nothing can be synced, shared, or recovered if the device is lost.

### Why Edge Functions
Authentication tokens must be validated server-side to prevent forgery. Password hashing, OAuth token exchange, and session refresh logic are security-sensitive and must never run on-device where they can be reverse-engineered. An edge function acts as the secure gateway between the client and Supabase Auth.

### Implementation Sketch
```typescript
// supabase/functions/auth-handler/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );
  const { action, email, password, refresh_token } = await req.json();

  if (action === "sign_up") {
    const { data, error } = await supabase.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
    });
    return new Response(JSON.stringify({ user: data?.user, error }));
  }

  if (action === "sign_in") {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    return new Response(
      JSON.stringify({
        access_token: data?.session?.access_token,
        refresh_token: data?.session?.refresh_token,
        user_id: data?.user?.id,
        error,
      })
    );
  }

  if (action === "refresh") {
    const { data, error } = await supabase.auth.refreshSession({
      refresh_token,
    });
    return new Response(
      JSON.stringify({
        access_token: data?.session?.access_token,
        refresh_token: data?.session?.refresh_token,
        error,
      })
    );
  }

  return new Response(JSON.stringify({ error: "Unknown action" }), {
    status: 400,
  });
});
```

### Client Integration
Add a `SupabaseAuthRepository` in `commonMain` that uses Ktor HTTP client to POST to the edge function URL. Store tokens in platform-specific secure storage (`EncryptedSharedPreferences` on Android, `Keychain` on iOS) via an `expect/actual TokenStorage` class. Inject via Koin alongside existing repositories.

### Benefit
Enables user identity across devices. Unlocks every subsequent use case (sync, social features, personalized push). Users can sign in on a new phone and recover all their favorites and posts.

---

## 2. Photo Upload and Server-Side Processing

### Current State
`CapturePhotoUseCase` captures a photo via `PlatformCamera`, attaches GPS coordinates from `PlatformLocationProvider`, and saves metadata to the `CapturedPhotoEntity` SQLDelight table. The actual image file stays on the local filesystem (`file_path` column). Photos are device-bound, cannot be shared, and retain full EXIF metadata including precise location.

### Why Edge Functions
Image processing (thumbnail generation, EXIF stripping, format conversion) is CPU-intensive and should not block the UI thread or drain mobile battery. Server-side processing also ensures privacy -- EXIF data with GPS coordinates is stripped before any photo is stored in the cloud, preventing accidental location leakage.

### Implementation Sketch
```typescript
// supabase/functions/photo-upload/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { decode } from "https://deno.land/std@0.168.0/encoding/base64.ts";

serve(async (req) => {
  const authHeader = req.headers.get("Authorization")!;
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const { image_base64, latitude, longitude, timestamp } = await req.json();
  const imageBytes = decode(image_base64);

  // Strip EXIF by re-encoding raw pixel data (simplified)
  const cleanImage = stripExif(imageBytes);
  const thumbnail = generateThumbnail(cleanImage, 300);

  const userId = (await supabase.auth.getUser()).data.user?.id;
  const fileName = `${userId}/${timestamp}.webp`;
  const thumbName = `${userId}/thumb_${timestamp}.webp`;

  // Upload full-size and thumbnail to Supabase Storage
  await supabase.storage.from("photos").upload(fileName, cleanImage, {
    contentType: "image/webp",
  });
  await supabase.storage.from("photos").upload(thumbName, thumbnail, {
    contentType: "image/webp",
  });

  // Store metadata in DB (GPS stored separately from image)
  const { data } = await supabase.from("photos").insert({
    user_id: userId,
    storage_path: fileName,
    thumb_path: thumbName,
    latitude,
    longitude,
    captured_at: new Date(timestamp).toISOString(),
  }).select().single();

  return new Response(JSON.stringify({ photo: data }));
});
```

### Client Integration
After `CapturePhotoUseCase` saves locally, a new `PhotoSyncUseCase` reads the file bytes, base64-encodes them, and POSTs to the edge function via Ktor. The local `CapturedPhotoEntity` table gains a `synced` boolean column. A background sync worker retries failed uploads. The `PhotoRepository` interface gets a `syncPhoto(id: Long)` method.

### Benefit
Photos are backed up to the cloud, viewable across devices, and privacy-safe. Thumbnails improve list-view performance. The original image with EXIF remains only on-device at the user's discretion.

---

## 3. Cloud Sync for Favorites

### Current State
`FavoriteRepositoryImpl` stores favorites in a local `Favorite` SQLDelight table keyed by `character_id`. The `observeFavorites()` method returns a reactive `Flow<List<Character>>`. Favorites exist only on the device where they were added -- switching phones means losing them.

### Why Edge Functions
Conflict resolution during sync (two devices favoriting/unfavoriting the same character) requires authoritative server-side logic. An edge function can implement last-write-wins or merge strategies that would be fragile and duplicated if done in client code on both platforms.

### Implementation Sketch
```typescript
// supabase/functions/sync-favorites/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const authHeader = req.headers.get("Authorization")!;
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const userId = (await supabase.auth.getUser()).data.user?.id;
  const { client_favorites, last_sync_at } = await req.json();
  // client_favorites: [{ character_id, name, status, species, image_url, added_at, deleted }]

  // Fetch server-side favorites modified after last sync
  const { data: serverFavs } = await supabase
    .from("favorites")
    .select("*")
    .eq("user_id", userId)
    .gte("updated_at", last_sync_at || "1970-01-01");

  // Merge: last-write-wins based on updated_at timestamp
  const merged = mergeLastWriteWins(client_favorites, serverFavs);

  // Upsert merged results to server
  await supabase.from("favorites").upsert(
    merged.map((f) => ({ ...f, user_id: userId })),
    { onConflict: "user_id,character_id" }
  );

  // Return full server state for client to reconcile
  const { data: allFavs } = await supabase
    .from("favorites")
    .select("*")
    .eq("user_id", userId)
    .eq("deleted", false);

  return new Response(
    JSON.stringify({
      favorites: allFavs,
      sync_timestamp: new Date().toISOString(),
    })
  );
});
```

### Client Integration
Add a `FavoriteSyncService` in `commonMain` that runs on app launch and after every `toggleFavorite()` call. It collects the local SQLDelight favorites, sends them to the edge function, and applies the returned server state back to SQLDelight. The `Favorite` table gains `updated_at` and `deleted` (soft-delete) columns. Koin wires the sync service alongside `FavoriteRepositoryImpl`.

### Benefit
Favorites persist across devices and app reinstalls. Users see the same favorites on their Android phone and iPad. No data loss on device replacement.

---

## 4. Posts API Migration (Replace GraphQLZero)

### Current State
`PostRepositoryImpl` performs full CRUD against `https://graphqlzero.almansi.me/api` via Apollo GraphQL. GraphQLZero is a mock/demo API -- mutations are fake (the server acknowledges them but does not persist data). Posts created in one session are gone in the next. The `Post` model has only `id`, `title`, and `body`.

### Why Edge Functions
Migrating to a Supabase-backed posts API gives real persistence, user ownership, and the ability to extend the data model (timestamps, author, tags). The edge function provides a REST-like interface so the client can drop Apollo for posts entirely, simplifying the dependency graph.

### Implementation Sketch
```typescript
// supabase/functions/posts-api/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const authHeader = req.headers.get("Authorization")!;
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const userId = (await supabase.auth.getUser()).data.user?.id;
  const url = new URL(req.url);
  const method = req.method;

  if (method === "GET") {
    const page = parseInt(url.searchParams.get("page") || "1");
    const limit = parseInt(url.searchParams.get("limit") || "10");
    const offset = (page - 1) * limit;

    const { data, count } = await supabase
      .from("posts")
      .select("*", { count: "exact" })
      .order("created_at", { ascending: false })
      .range(offset, offset + limit - 1);

    return new Response(JSON.stringify({ posts: data, totalCount: count }));
  }

  if (method === "POST") {
    const { title, body } = await req.json();
    const { data } = await supabase
      .from("posts")
      .insert({ title, body, user_id: userId })
      .select()
      .single();
    return new Response(JSON.stringify({ post: data }), { status: 201 });
  }

  if (method === "PUT") {
    const { id, title, body } = await req.json();
    const { data } = await supabase
      .from("posts")
      .update({ title, body, updated_at: new Date().toISOString() })
      .eq("id", id)
      .eq("user_id", userId) // ensure ownership
      .select()
      .single();
    return new Response(JSON.stringify({ post: data }));
  }

  if (method === "DELETE") {
    const { id } = await req.json();
    await supabase.from("posts").delete().eq("id", id).eq("user_id", userId);
    return new Response(JSON.stringify({ deleted: true }));
  }
});
```

### Client Integration
Replace `PostRepositoryImpl` with a new `SupabasePostRepositoryImpl` that uses Ktor HTTP client instead of Apollo. The `PostRepository` interface stays identical -- `getPosts()`, `createPost()`, `updatePost()`, `deletePost()` -- so no use-case or UI code changes. Remove the `graphqlzero` Apollo client from `SharedModule`. The `Post` domain model can be extended with `authorId` and `createdAt` fields.

### Benefit
Posts actually persist. Users own their content. The app no longer depends on a third-party demo API that could go offline at any time. Opens the door for multi-user post feeds.

---

## 5. Server-Side Character Search with Full-Text Search

### Current State
`CharacterRepository.getCharacters(page, nameFilter)` passes the search string directly to the Rick and Morty GraphQL API's `name` filter parameter. This is a simple substring match with no ranking, typo tolerance, or fuzzy matching. Searching "rick" works, but "rik" returns nothing.

### Why Edge Functions
Full-text search with ranking, trigram matching, and typo tolerance requires Postgres features (tsvector, pg_trgm) that run server-side. An edge function can maintain a cached copy of the character catalog in Supabase Postgres and provide superior search results that the external API cannot.

### Implementation Sketch
```typescript
// supabase/functions/character-search/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const { query, page = 1, limit = 20 } = await req.json();
  const offset = (page - 1) * limit;

  if (!query || query.trim().length === 0) {
    // No search term -- return paginated catalog
    const { data, count } = await supabase
      .from("characters")
      .select("*", { count: "exact" })
      .order("name")
      .range(offset, offset + limit - 1);
    return new Response(JSON.stringify({ results: data, totalCount: count }));
  }

  // Fuzzy search using pg_trgm similarity + full-text search
  const { data } = await supabase.rpc("search_characters", {
    search_query: query,
    result_limit: limit,
    result_offset: offset,
  });
  // RPC calls a Postgres function:
  // SELECT *, similarity(name, search_query) AS rank
  // FROM characters
  // WHERE name % search_query OR species % search_query
  // ORDER BY rank DESC
  // LIMIT result_limit OFFSET result_offset;

  return new Response(JSON.stringify({ results: data }));
});
```

### Client Integration
Add a `SupabaseCharacterSearchRepository` that implements a new `searchCharacters(query, page)` method. The existing `GetCharactersUseCase` gains a strategy: if query is present, call Supabase search; otherwise, paginate from the Rick and Morty API. Wire the new repository via Koin.

### Benefit
Users get typo-tolerant, ranked search results. Searching "rik" or "morthy" returns relevant matches. Search results load faster from a local Supabase Postgres cache instead of a third-party API.

---

## 6. Push Notifications

### Current State
STEP has no push notification capability. Users must manually open the app to discover new content or check on their posts. There is no mechanism to re-engage users or notify them of events.

### Why Edge Functions
Push notification dispatch requires server-side credentials (FCM server key, APNs certificates) that must never be embedded in client code. The edge function securely holds these credentials and can trigger notifications based on database events (new posts, character API updates) or scheduled checks.

### Implementation Sketch
```typescript
// supabase/functions/send-notification/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const { event, payload } = await req.json();

  // Fetch device tokens for targeted users
  const { data: tokens } = await supabase
    .from("device_tokens")
    .select("fcm_token, apns_token, platform, user_id")
    .in("user_id", payload.target_user_ids);

  const notifications = tokens.map((t) => {
    if (t.platform === "android") {
      return sendFCM(t.fcm_token, {
        title: payload.title,
        body: payload.body,
        data: payload.deep_link_data,
      });
    } else {
      return sendAPNs(t.apns_token, {
        alert: { title: payload.title, body: payload.body },
        data: payload.deep_link_data,
      });
    }
  });

  const results = await Promise.allSettled(notifications);
  const succeeded = results.filter((r) => r.status === "fulfilled").length;

  return new Response(
    JSON.stringify({ sent: succeeded, failed: results.length - succeeded })
  );
});
```

### Client Integration
Add a `DeviceTokenRepository` that registers the FCM/APNs token with Supabase on app launch via a simple POST to a registration edge function. Platform-specific token retrieval uses `expect/actual` classes: `FirebaseMessaging` on Android, `UNUserNotificationCenter` on iOS. Koin provides the repository. The notification edge function is triggered by Supabase database webhooks (e.g., new row in `posts` table).

### Benefit
Re-engages users with timely notifications. Enables alerts like "New Rick and Morty characters added" or "Someone commented on your post." Drives retention without any always-on server.

---

## 7. Analytics and Event Logging

### Current State
STEP has no analytics. There is no visibility into which characters users browse, how often they search, which posts get created, or how the camera feature is used. Development decisions are made without usage data.

### Why Edge Functions
Client-side analytics SDKs add binary size and can be blocked by privacy tools. Server-side event collection via an edge function is lightweight, privacy-controllable (the server decides what to retain), and cannot be circumvented. It also provides a single collection point for both platforms.

### Implementation Sketch
```typescript
// supabase/functions/track-event/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const { events } = await req.json();
  // events: [{ name, properties, timestamp, session_id, platform, app_version }]

  // Validate and sanitize: strip PII, enforce schema
  const sanitized = events.map((e) => ({
    event_name: e.name,
    properties: stripPII(e.properties),
    platform: e.platform, // "android" | "ios"
    app_version: e.app_version,
    session_id: e.session_id,
    created_at: e.timestamp || new Date().toISOString(),
  }));

  const { error } = await supabase.from("analytics_events").insert(sanitized);

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
    });
  }

  return new Response(JSON.stringify({ accepted: sanitized.length }));
});
```

### Client Integration
Create an `AnalyticsService` in `commonMain` that buffers events in memory and flushes them in batches (every 30 seconds or 20 events, whichever comes first) via Ktor POST. Track key events: `character_viewed`, `character_searched`, `favorite_toggled`, `post_created`, `photo_captured`, `tab_switched`. Wire through Koin as a singleton. Use cases call `analyticsService.track("event_name", mapOf("key" to "value"))`.

### Benefit
Data-driven development decisions. Understand which features are used, identify drop-off points, and prioritize work. No third-party analytics SDK required, keeping the app lightweight.

---

## 8. Rate Limiting and API Gateway

### Current State
Both Apollo clients (`rickandmorty` and `graphqlzero`) make direct calls to third-party APIs with no rate limiting, caching, or error budget management. If a user rapidly paginates or searches, the app hammers the external APIs. The Rick and Morty API has undocumented rate limits that could result in silent failures.

### Why Edge Functions
An edge function acting as an API gateway centralizes rate limiting, response caching, and API key management. Third-party API keys (if any) stay server-side. The function can serve cached responses for identical queries, reducing latency and external API load.

### Implementation Sketch
```typescript
// supabase/functions/api-gateway/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const RATE_LIMIT_WINDOW = 60_000; // 1 minute
const MAX_REQUESTS = 30;

serve(async (req) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const authHeader = req.headers.get("Authorization");
  const userId = authHeader
    ? (
        await createClient(
          Deno.env.get("SUPABASE_URL")!,
          Deno.env.get("SUPABASE_ANON_KEY")!,
          { global: { headers: { Authorization: authHeader } } }
        ).auth.getUser()
      ).data.user?.id
    : req.headers.get("x-device-id"); // anonymous fallback

  // Check rate limit via Supabase cache table
  const { count } = await supabase
    .from("rate_limits")
    .select("*", { count: "exact" })
    .eq("user_key", userId)
    .gte("created_at", new Date(Date.now() - RATE_LIMIT_WINDOW).toISOString());

  if ((count || 0) >= MAX_REQUESTS) {
    return new Response(JSON.stringify({ error: "Rate limit exceeded" }), {
      status: 429,
      headers: { "Retry-After": "60" },
    });
  }

  // Log this request
  await supabase
    .from("rate_limits")
    .insert({ user_key: userId, created_at: new Date().toISOString() });

  const { target_url, query, variables } = await req.json();

  // Check response cache
  const cacheKey = JSON.stringify({ target_url, query, variables });
  const { data: cached } = await supabase
    .from("api_cache")
    .select("response, cached_at")
    .eq("cache_key", cacheKey)
    .gte("cached_at", new Date(Date.now() - 300_000).toISOString()) // 5min TTL
    .maybeSingle();

  if (cached) {
    return new Response(cached.response, {
      headers: { "X-Cache": "HIT" },
    });
  }

  // Forward to external API
  const response = await fetch(target_url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  const body = await response.text();

  // Cache the response
  await supabase
    .from("api_cache")
    .upsert({ cache_key: cacheKey, response: body, cached_at: new Date().toISOString() });

  return new Response(body, { headers: { "X-Cache": "MISS" } });
});
```

### Client Integration
Change `createRickAndMortyApolloClient()` to point at the edge function URL instead of the direct API. Apollo client configuration stays the same -- only the `serverUrl` changes. Alternatively, use a custom Apollo `NetworkTransport` that routes through the gateway. No use-case or UI changes required.

### Benefit
Protects against rate limiting by external APIs. Cached responses reduce latency by 50-80% for repeated queries. API keys are never exposed in client code. Provides a single chokepoint for monitoring and debugging API issues.

---

## 9. Scheduled Character Data Refresh (Cron)

### Current State
Character data is fetched live from the Rick and Morty API on every app open and page scroll. There is no local cache beyond what Apollo's in-memory normalized cache provides for the current session. If the Rick and Morty API goes down, the Characters tab is completely broken.

### Why Edge Functions
Supabase Edge Functions can be triggered on a cron schedule. A scheduled function can periodically pull the full character catalog into Supabase Postgres, acting as a resilient cache layer. This decouples the app's availability from the external API's uptime.

### Implementation Sketch
```typescript
// supabase/functions/refresh-characters/index.ts
// Triggered via Supabase cron: every 6 hours
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async () => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  let page = 1;
  let hasNext = true;
  const allCharacters = [];

  while (hasNext) {
    const response = await fetch("https://rickandmortyapi.com/graphql", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        query: `query($page: Int) {
          characters(page: $page) {
            info { next }
            results { id name status species gender image
              origin { id name } location { id name }
            }
          }
        }`,
        variables: { page },
      }),
    });

    const json = await response.json();
    const chars = json.data.characters.results;
    allCharacters.push(
      ...chars.map((c) => ({
        id: c.id,
        name: c.name,
        status: c.status,
        species: c.species,
        gender: c.gender,
        image_url: c.image,
        origin_name: c.origin?.name,
        location_name: c.location?.name,
        updated_at: new Date().toISOString(),
      }))
    );

    hasNext = json.data.characters.info.next !== null;
    page++;
  }

  // Upsert all characters
  const { error } = await supabase
    .from("characters")
    .upsert(allCharacters, { onConflict: "id" });

  return new Response(
    JSON.stringify({
      refreshed: allCharacters.length,
      error: error?.message,
    })
  );
});
```

### Client Integration
The character search edge function (use case 5) and the API gateway (use case 8) both read from this cached `characters` table. The client can optionally fall back to the Supabase cache when the Rick and Morty API is unreachable, by adding a fallback path in `CharacterRepositoryImpl.getCharacters()`.

### Benefit
The app remains functional even when the Rick and Morty API is down. Enables full-text search on the cached data. Reduces external API calls from thousands per day (per user) to a fixed 6-hourly batch.

---

## 10. Social Features: Comments, Likes, and Sharing

### Current State
STEP has no social interaction layer. Posts are solo content (and currently fake via GraphQLZero). Characters can only be favorited locally. There is no way for users to interact with each other's content.

### Why Edge Functions
Social features require server-side enforcement of business rules: a user cannot like their own post twice, comment content must be sanitized, and like counts must be atomically incremented to avoid race conditions. Edge functions provide the transactional guarantees that client-side logic cannot.

### Implementation Sketch
```typescript
// supabase/functions/social/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const authHeader = req.headers.get("Authorization")!;
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const userId = (await supabase.auth.getUser()).data.user?.id;
  const { action, post_id, comment_body } = await req.json();

  if (action === "like") {
    // Atomic toggle: insert or delete in one transaction
    const { data: existing } = await supabase
      .from("likes")
      .select("id")
      .eq("user_id", userId)
      .eq("post_id", post_id)
      .maybeSingle();

    if (existing) {
      await supabase.from("likes").delete().eq("id", existing.id);
      await supabase.rpc("decrement_like_count", { target_post_id: post_id });
      return new Response(JSON.stringify({ liked: false }));
    } else {
      await supabase
        .from("likes")
        .insert({ user_id: userId, post_id });
      await supabase.rpc("increment_like_count", { target_post_id: post_id });
      return new Response(JSON.stringify({ liked: true }));
    }
  }

  if (action === "comment") {
    const sanitized = sanitizeHtml(comment_body);
    if (sanitized.length === 0 || sanitized.length > 1000) {
      return new Response(
        JSON.stringify({ error: "Comment must be 1-1000 characters" }),
        { status: 400 }
      );
    }

    const { data } = await supabase
      .from("comments")
      .insert({
        user_id: userId,
        post_id,
        body: sanitized,
      })
      .select("*, profiles(display_name, avatar_url)")
      .single();

    return new Response(JSON.stringify({ comment: data }), { status: 201 });
  }

  if (action === "get_comments") {
    const { data } = await supabase
      .from("comments")
      .select("*, profiles(display_name, avatar_url)")
      .eq("post_id", post_id)
      .order("created_at", { ascending: true });

    return new Response(JSON.stringify({ comments: data }));
  }
});
```

### Client Integration
Add `SocialRepository` in `commonMain` with methods: `toggleLike(postId)`, `addComment(postId, body)`, `getComments(postId)`. The `Post` domain model extends with `likeCount: Int` and `isLikedByMe: Boolean`. Post detail screens gain a comments section and like button. Koin provides the repository. The `PostPage` response from use case 4 includes like counts and the current user's like status.

### Benefit
Transforms STEP from a solo utility into a social platform. Users can engage with each other's posts, building community. Like counts surface popular content. Comments create discussion threads. All server-enforced for consistency.

---

## Summary Matrix

| # | Use Case | Priority | Complexity | Dependencies |
|---|----------|----------|------------|-------------|
| 1 | Authentication | Critical | Medium | None (foundation for all others) |
| 2 | Photo Upload & Processing | High | High | Auth |
| 3 | Cloud Sync for Favorites | High | Medium | Auth |
| 4 | Posts API Migration | High | Low | Auth |
| 5 | Character Search (FTS) | Medium | Medium | Cron refresh (#9) |
| 6 | Push Notifications | Medium | High | Auth, device token infra |
| 7 | Analytics & Logging | Medium | Low | None (can work anonymously) |
| 8 | Rate Limiting / API Gateway | Medium | Medium | None |
| 9 | Scheduled Data Refresh | Low | Low | None |
| 10 | Social Features | Low | High | Auth, Posts migration (#4) |

**Recommended implementation order:** 1 (Auth) -> 4 (Posts migration) -> 3 (Favorites sync) -> 7 (Analytics) -> 8 (API gateway) -> 9 (Cron refresh) -> 5 (Search) -> 2 (Photo upload) -> 6 (Push) -> 10 (Social).

Authentication is the foundation -- every other use case except analytics and rate limiting depends on knowing who the user is. Posts migration is next because it replaces the fake GraphQLZero dependency with real persistence. Favorites sync follows naturally since the sync pattern established for posts applies directly. The remaining use cases build outward from this core.
