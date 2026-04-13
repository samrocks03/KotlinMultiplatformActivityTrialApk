# Supabase Edge Functions: Fundamentals

## 1. What Are Supabase Edge Functions?

### Definition and Purpose

Supabase Edge Functions are server-side TypeScript functions distributed globally at the edge, close to your users. They allow you to run custom server-side logic without managing infrastructure, responding to HTTP requests, webhooks, and scheduled events. They serve as the "compute" layer in the Supabase ecosystem, complementing the database (Postgres), authentication, storage, and realtime services.

Edge Functions are ideal for tasks that cannot or should not run on the client, such as processing payments, calling third-party APIs with secret keys, performing server-side validation, or orchestrating complex workflows.

### How They Differ from Traditional Serverless Functions

Unlike traditional serverless platforms such as AWS Lambda or Google Cloud Functions, Supabase Edge Functions have several distinguishing characteristics:

| Aspect | Supabase Edge Functions | AWS Lambda / GCF |
|---|---|---|
| **Runtime** | Deno (V8 isolates) | Node.js, Python, Go, Java, etc. |
| **Deployment model** | Edge (multi-region, close to users) | Regional (single region by default) |
| **Language** | TypeScript/JavaScript first | Multi-language |
| **Cold starts** | Minimal (ESZip format, lightweight isolates) | Variable (seconds for some runtimes) |
| **Bundling** | ESZip (compact Deno module graph) | ZIP archives or container images |
| **Native Supabase integration** | Built-in (Auth, DB, Storage, Realtime) | Requires manual SDK setup |
| **Infrastructure** | Fully managed by Supabase | Managed but requires more configuration |

The key differentiator is the edge deployment model. Rather than running in a single cloud region, Edge Functions execute at locations geographically close to the end user, reducing latency for global audiences.

### The Deno Runtime

[Deno](https://deno.land) is a modern JavaScript/TypeScript runtime built on V8 (the same engine powering Chrome and Node.js). Created by Ryan Dahl (the original creator of Node.js), Deno was designed to address Node.js shortcomings.

Supabase chose Deno for several reasons:

- **Security by default**: Deno runs with no file, network, or environment access unless explicitly granted. This sandboxed model is ideal for multi-tenant edge execution.
- **TypeScript first-class support**: No build step needed; Deno natively understands TypeScript.
- **Web-standard APIs**: Deno uses `fetch`, `Request`, `Response`, and other Web APIs, meaning code written for Edge Functions is portable.
- **ESZip bundling**: Deno's ESZip format produces compact bundles that include the full module dependency graph, enabling fast cold starts.
- **npm compatibility**: Deno supports importing npm packages via `npm:` specifiers and Node.js built-in APIs via `node:` specifiers, giving access to the vast Node ecosystem.

### TypeScript-First Approach

When you create a new Edge Function, it uses TypeScript by default. There is no separate compilation step; the Deno runtime handles TypeScript natively. This means you get type safety, better IDE support, and self-documenting code without additional tooling.

---

## 2. Architecture and How They Work

### Edge Deployment Model

Supabase Edge Functions run on a globally distributed network. When a request comes in, it is routed to the nearest edge location, where the function executes. This minimizes round-trip latency compared to traditional single-region deployments.

The high-level request flow is:

1. Client sends an HTTP request to `https://<project-ref>.supabase.co/functions/v1/<function-name>`
2. The request enters the **edge gateway**, which handles routing and auth header/JWT validation
3. Auth and policies are applied
4. The request is forwarded to an available **isolate** running the function
5. The function executes and returns a response

### Cold Starts vs Warm Execution

Edge Functions run in lightweight, transient servers called **isolates**. Each isolate is bound to a single function.

- **Cold start**: When no active isolate exists for a function, a new one must be booted. Thanks to Deno's ESZip format and minimal runtime overhead, cold starts are fast (milliseconds). Supabase has achieved up to **97% faster cold starts** through persistent storage optimizations.
- **Warm execution**: Isolates remain active for a period (plan-dependent) to handle subsequent requests without restarting. This eliminates cold start overhead for repeated calls.
- **Scaling**: Multiple isolates can run simultaneously in the same edge location to handle high traffic. When a request arrives, the runtime assigns it to a free isolate or spins up a new one if all are busy.
- **Resource management**: Once an isolate uses 50% of any resource limit, it finishes the current request and then shuts down, ensuring graceful degradation.

### Request/Response Lifecycle

Every Edge Function uses the `Deno.serve()` API to handle incoming requests:

```typescript
Deno.serve(async (req: Request) => {
  const { name } = await req.json()
  const data = { message: `Hello ${name}!` }
  return new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' },
  })
})
```

> **Important**: Do NOT use the legacy `import { serve } from "https://deno.land/std@0.168.0/http/server.ts"` pattern. Always use the built-in `Deno.serve`.

Edge Functions support GET, POST, PUT, PATCH, DELETE, and OPTIONS methods. A single function can be designed to perform different actions based on the request's HTTP method.

### Connection to Supabase Services

Edge Functions have built-in access to all Supabase services through environment variables that are automatically injected:

```typescript
import { createClient } from 'jsr:@supabase/supabase-js@2'

Deno.serve(async (req: Request) => {
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!
  )

  // Query the database
  const { data, error } = await supabase
    .from('users')
    .select('*')

  return new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' },
  })
})
```

The automatically available environment variables include:
- `SUPABASE_URL` -- Your project's API URL
- `SUPABASE_ANON_KEY` -- The public anonymous key
- `SUPABASE_SERVICE_ROLE_KEY` -- The service role key (full admin access)
- `SUPABASE_DB_URL` -- Direct database connection string

This means Edge Functions can interact with **Auth** (verify JWTs, manage users), **Database** (query Postgres via the client or direct connection), **Storage** (upload/download files), and **Realtime** (broadcast messages) without any manual configuration.

---

## 3. Key Features

### Built-in Supabase Client Access

As shown above, every Edge Function automatically has access to Supabase project credentials via environment variables. You can create an authenticated client in a single line, enabling seamless interaction with all Supabase services.

### Environment Variables and Secrets Management

Secrets and configuration values are managed through environment variables:

- **Local development**: Use a `.env` file at `supabase/functions/.env`, which is automatically loaded during local dev. Always add this path to `.gitignore`.
- **Production**: Set secrets via the Dashboard or CLI:
  ```bash
  supabase secrets set MY_API_KEY=sk-abc123
  ```
- **Bulk upload**: Push all secrets from your `.env` file to your remote project:
  ```bash
  supabase secrets set --env-file supabase/functions/.env
  ```
- **Access in code**: Use `Deno.env.get('MY_API_KEY')` to read secrets at runtime.

For security, never log full secret values. Log only truncated versions if you need to verify they are set.

### CORS Handling

To invoke Edge Functions from the browser, you must handle CORS preflight requests. For `@supabase/supabase-js` v2.95.0 and later, you can import CORS headers directly from the SDK:

```typescript
import { corsHeaders } from '@supabase/supabase-js/cors'

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  const data = { message: 'Hello from the edge!' }
  return new Response(JSON.stringify(data), {
    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
  })
})
```

This approach ensures that when new headers are added to the Supabase SDK, your functions automatically include them, preventing CORS errors.

### Middleware Patterns

Supabase Edge Functions support routing patterns that allow a single function to handle multiple endpoints. You can implement middleware-style logic for authentication checks, logging, rate limiting, and request transformation within your function handler.

### Scheduling (Cron Jobs)

Using the `pg_cron` extension combined with `pg_net`, you can invoke Edge Functions on a recurring schedule:

- Schedule via SQL or the Dashboard (Integrations > Cron)
- Supports cron syntax from every 1 second to once a year
- Jobs can run SQL snippets, database functions, or make HTTP requests to Edge Functions

```sql
SELECT cron.schedule(
  'daily-cleanup',
  '0 0 * * *',  -- Every day at midnight
  $$
  SELECT net.http_post(
    url := 'https://<project-ref>.supabase.co/functions/v1/cleanup',
    headers := jsonb_build_object(
      'Authorization', 'Bearer ' || '<service-role-key>'
    )
  );
  $$
);
```

### Webhooks

Edge Functions are excellent webhook receivers. They can process incoming payloads from services like Stripe, GitHub, Twilio, and others, validate signatures, and trigger downstream actions in your Supabase project.

### npm Package Compatibility

Supabase Edge Functions support npm packages and Node.js built-in APIs:

- **npm packages**: Import using the `npm:` specifier:
  ```typescript
  import Stripe from 'npm:stripe@13'
  ```
- **Node.js built-ins**: Import using the `node:` specifier:
  ```typescript
  import process from 'node:process'
  import { Buffer } from 'node:buffer'
  ```
- **Dependency management**: Use `deno.json` (recommended over import maps) to manage dependencies and aliases.

This bridges the gap between Deno and the vast Node.js ecosystem, making it straightforward to port existing Node.js code.

---

## 4. Limitations and Constraints

### Execution Time Limits

| Limit | Value |
|---|---|
| **CPU time per request** | 2 seconds (actual CPU computation, excludes async I/O) |
| **Wall clock time** | 400 seconds (total elapsed time including I/O waits) |
| **Request idle timeout** | 150 seconds (if no response is sent, returns 504) |

The distinction between CPU time and wall clock time is critical: an Edge Function can wait for external API responses (async I/O) for up to 400 seconds, but the actual computation must complete within 2 seconds.

### Memory Limits

- **Runtime memory**: 150 MB per isolate
- Avoid loading entire files or responses into memory; stream data to reduce memory footprint

### Payload and Bundle Size

- **Maximum function size**: 20 MB after bundling (ESZip via CLI)
- **Request/response payload**: Subject to standard HTTP limits; large payloads should be streamed

### No Persistent Filesystem

Edge Functions run in ephemeral isolates. There is no persistent filesystem across invocations. While Supabase has introduced ephemeral storage for temporary file operations within a single invocation, any data that must persist should be written to the database or Supabase Storage.

### Cold Start Considerations

While cold starts are fast, they do exist. Design your functions for short-lived, idempotent operations. Avoid heavy initialization logic at the module level.

### Other Constraints

- Outgoing connections to ports 25 and 587 (SMTP) are blocked
- Serving HTML content is only supported with custom domains (GET requests returning `text/html` are rewritten to `text/plain` otherwise)
- Web Worker API and Node `vm` API are not available
- Static files cannot be deployed using the API flag

---

## 5. Pricing Model

### Free Tier

- **Included invocations**: 500,000 per month
- **Cost**: $0 (included with the free Supabase plan)
- When the quota is exceeded on the Free plan, you receive a notification and enter a grace period

### Pro Tier

- **Plan cost**: $25/month
- **Included invocations**: Higher quota than Free (approximately 2 million per month based on documented overage calculations)
- Includes daily backups, higher limits across all services, and email support

### Overage Pricing

- **$2 per 1 million invocations** beyond the included quota
- Overages only apply when Spend Cap is disabled (Pro plan) or on Team/Enterprise plans
- With Spend Cap enabled, functions stop executing when the quota is exhausted

### Bandwidth Considerations

Edge Function egress (data transfer out) counts toward your project's overall egress quota:
- Free tier: 5 GB included
- Pro tier: 250 GB included
- Overage: $0.09 per GB

### Cost Example

For an application making 3 million Edge Function invocations per month on the Pro plan:
- Included: ~2 million invocations
- Overage: ~1 million invocations
- Overage cost: $2
- Total Edge Function cost: $25 (plan) + $2 (overage) = $27/month

---

## 6. When to Use vs When NOT to Use

### Good Use Cases

- **Webhook receivers**: Process incoming webhooks from Stripe, GitHub, SendGrid, etc.
- **API proxies**: Hide third-party API keys from clients while adding custom logic
- **Authentication flows**: Custom OAuth flows, token exchange, or multi-step verification
- **Payment processing**: Server-side Stripe/PayPal integration
- **AI/LLM orchestration**: Call OpenAI, Anthropic, or other LLM APIs with server-side secret keys
- **Image/OG generation**: On-demand image processing or Open Graph image generation
- **Email/notification dispatch**: Send transactional emails via SendGrid, Resend, etc.
- **Bot integrations**: Slack, Discord, or Telegram bot handlers
- **Data transformation**: Lightweight ETL or data enrichment before database insertion

### Anti-Patterns (When NOT to Use)

- **Heavy computation**: Tasks requiring more than 2 seconds of CPU time belong on dedicated compute (e.g., background workers, dedicated servers)
- **Long-running batch jobs**: While the wall clock limit is 400 seconds, sustained processing should use queues and background workers
- **Large file processing**: The 150 MB memory limit makes processing large files impractical; use Supabase Storage with presigned URLs instead
- **Persistent state**: Do not rely on isolate memory between requests; use the database or Storage
- **High-throughput SMTP**: Outbound email ports (25, 587) are blocked; use third-party email APIs instead
- **Static file serving**: Edge Functions are not a CDN; use Supabase Storage or a dedicated CDN for static assets

### Comparison with Alternatives in the Supabase Ecosystem

| Approach | Best For | Limitations |
|---|---|---|
| **Edge Functions** | Low-latency HTTP endpoints, external API calls, webhooks, complex TypeScript logic | CPU/memory limits, ephemeral, no persistent filesystem |
| **Database Functions (pg functions)** | Business logic that rarely changes, data transformations close to the data, triggers | SQL/PL-pgSQL only, no HTTP calls without `pg_net`, runs in a single region |
| **RLS Policies** | Row-level access control, authorization rules | SQL-only, not for business logic, can impact query performance if complex |
| **Database Webhooks** | Reacting to database changes (INSERT/UPDATE/DELETE) | Limited to database events, no custom HTTP endpoints |

**Decision framework**:

1. If your logic is **pure data access control**, use **RLS policies**.
2. If your logic is **data transformation that rarely changes** and lives close to the database, use **database functions (RPC)**.
3. If you need to **react to database changes**, use **database webhooks** (potentially calling an Edge Function).
4. If you need **custom HTTP endpoints, external API calls, or complex TypeScript logic**, use **Edge Functions**.

In practice, these approaches are complementary. A typical Supabase application uses RLS for authorization, database functions for data-centric logic, and Edge Functions for everything that requires external communication or complex server-side processing.

---

## Summary

Supabase Edge Functions provide a TypeScript-first, globally distributed serverless compute layer built on Deno. They integrate deeply with the Supabase ecosystem, offer fast cold starts via ESZip bundling, and support npm packages for broad compatibility. With generous free-tier allowances (500K invocations/month) and low overage costs ($2/million), they are a cost-effective choice for webhook processing, API proxying, and server-side orchestration. The key constraints to keep in mind are the 2-second CPU time limit, 150 MB memory cap, and ephemeral nature of isolates.

---

**Sources**:
- [Edge Functions Overview](https://supabase.com/docs/guides/functions)
- [Edge Functions Architecture](https://supabase.com/docs/guides/functions/architecture)
- [Edge Functions Limits](https://supabase.com/docs/guides/functions/limits)
- [Edge Functions Pricing](https://supabase.com/docs/guides/functions/pricing)
- [Environment Variables / Secrets](https://supabase.com/docs/guides/functions/secrets)
- [CORS Support](https://supabase.com/docs/guides/functions/cors)
- [Scheduling Edge Functions](https://supabase.com/docs/guides/functions/schedule-functions)
- [Managing Dependencies](https://supabase.com/docs/guides/functions/dependencies)
- [npm Compatibility](https://supabase.com/features/npm-compatibility)
- [Deno Edge Functions Feature Page](https://supabase.com/features/deno-edge-functions)
- [Supabase Pricing](https://supabase.com/pricing)
- [Manage Edge Function Invocations](https://supabase.com/docs/guides/platform/manage-your-usage/edge-function-invocations)
- [97% Faster Cold Starts Blog Post](https://supabase.com/blog/persistent-storage-for-faster-edge-functions)
- [Edge Functions Node/npm Compatibility Blog](https://supabase.com/blog/edge-functions-node-npm)
