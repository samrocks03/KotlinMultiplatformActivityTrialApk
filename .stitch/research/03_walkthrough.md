# Building Supabase Edge Functions from Scratch

A hands-on walkthrough covering environment setup, development, Supabase service integration, advanced patterns, testing, and deployment. By the end of this guide you will have a working Edge Function deployed to production.

Supabase Edge Functions are server-side TypeScript functions that run on Deno Deploy, distributed globally at the edge close to your users. They execute in a secure, sandboxed Deno runtime and are ideal for webhooks, API proxies, server-side business logic, and integrating with third-party services. Because they run on Deno, you get TypeScript support out of the box, top-level await, Web Standard APIs (fetch, Request, Response), and ESM imports by URL without a package.json.

---

## Part 1: Environment Setup

### Prerequisites

Before starting, make sure you have:
- A free Supabase account at [supabase.com](https://supabase.com)
- Node.js 18+ (for the npm install method) or Homebrew (macOS/Linux)
- A terminal and a code editor (VS Code with the Deno extension is recommended)

### Installing the Supabase CLI

Install via npm (cross-platform) or Homebrew (macOS/Linux):

```bash
# npm (recommended for CI/CD)
npm install -g supabase

# Homebrew
brew install supabase/tap/supabase

# Verify installation
supabase --version
```

### Project Initialization

```bash
mkdir my-edge-project && cd my-edge-project
supabase init
```

This generates a `supabase/` directory with `config.toml` — the local configuration file for your Supabase project. The `config.toml` file controls local development settings including ports, database configuration, auth settings, and per-function options like JWT verification and import maps. You will edit this file frequently as you add functions.

### Authentication

```bash
supabase login
```

This opens a browser window to authenticate with your Supabase account and stores an access token locally at `~/.supabase/access-token`. The token is used for all subsequent CLI operations against your remote projects. For CI/CD environments, you can set the `SUPABASE_ACCESS_TOKEN` environment variable instead of running the interactive login.

### Linking to a Remote Project

```bash
supabase link --project-ref <your-project-ref>
```

Find your project ref in the Supabase Dashboard under **Project Settings > General**. It is the alphanumeric string in your project URL (e.g., `abcdefghijklmnop`). Linking associates your local `supabase/` directory with the remote project so that commands like `deploy`, `secrets set`, and `db push` know which project to target. You will be prompted for your database password during linking.

### Directory Structure

After initialization and creating your first function, your project looks like this:

```
my-edge-project/
├── supabase/
│   ├── config.toml              # Local project configuration
│   ├── .env                     # Local environment variables (gitignored)
│   ├── functions/
│   │   ├── _shared/             # Shared modules across functions
│   │   │   └── cors.ts          # Reusable CORS headers
│   │   ├── hello-world/
│   │   │   └── index.ts         # Function entry point
│   │   └── process-order/
│   │       └── index.ts
│   └── migrations/              # Database migrations
```

The `supabase/functions/` directory is where all your Edge Functions live. Each function gets its own subdirectory with an `index.ts` entry point. The `_shared/` directory (prefixed with underscore) holds shared utilities and is not deployed as a standalone function.

---

## Part 2: Your First Edge Function

### Scaffold a New Function

```bash
supabase functions new hello-world
```

This creates `supabase/functions/hello-world/index.ts` with a starter template. The generated file contains a minimal `Deno.serve()` handler that you can immediately test locally. Each function lives in its own directory and can contain additional helper files, but `index.ts` is the required entry point (unless you override it in `config.toml`).

### Anatomy of an Edge Function

Every Edge Function uses the `Deno.serve()` pattern — a single handler that receives a `Request` and returns a `Response`:

```typescript
// supabase/functions/hello-world/index.ts

Deno.serve(async (req: Request) => {
  // 1. Read request details
  const { method, url } = req;
  const headers = req.headers;

  // 2. Parse the body (for POST/PUT/PATCH)
  let body = null;
  if (method === "POST") {
    body = await req.json();
  }

  // 3. Your business logic
  const data = {
    message: `Hello from Edge Functions!`,
    method,
    receivedBody: body,
    timestamp: new Date().toISOString(),
  };

  // 4. Return a JSON response
  return new Response(JSON.stringify(data), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
```

### Key Concepts

- **`Deno.serve()`** registers an HTTP handler. The Supabase Edge Runtime calls it for every incoming request.
- The **`Request`** object is the standard Web API Request — use `req.json()`, `req.text()`, `req.formData()`, or `req.arrayBuffer()` to parse the body.
- Return a standard Web API **`Response`**. Always set `Content-Type`.

### Response Patterns

```typescript
// JSON success
return new Response(JSON.stringify({ ok: true }), {
  status: 200,
  headers: { "Content-Type": "application/json" },
});

// Error response
return new Response(JSON.stringify({ error: "Not found" }), {
  status: 404,
  headers: { "Content-Type": "application/json" },
});

// Plain text
return new Response("OK", { status: 200 });

// Redirect
return new Response(null, {
  status: 302,
  headers: { Location: "https://example.com" },
});
```

### How It Works Under the Hood

When you deploy an Edge Function, the Supabase CLI bundles your TypeScript code and all its dependencies into an ESZip file — a compact format created by Deno that includes the complete module graph. This bundle is then distributed to Deno Deploy's global edge network. Each incoming HTTP request is routed to the nearest edge location, where the runtime boots your function in an isolated V8 context, executes the handler, and returns the response. Cold starts are typically under 200ms.

The Supabase Edge Runtime (used both locally via `functions serve` and in production) is distinct from the standard Deno CLI. It provides consistent behavior between environments and automatically injects Supabase-specific environment variables like `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY`.

### Local Development

Start the local Edge Runtime:

```bash
supabase start          # Start local Supabase stack (Postgres, Auth, etc.)
supabase functions serve # Serve all edge functions locally
```

Functions are available at `http://localhost:54321/functions/v1/<function-name>`. The local server supports hot reloading — when you save your file, changes are picked up instantly without restarting. The `supabase start` command is required first because it boots the full local Supabase stack (Postgres, Auth, Storage, etc.) that your functions may depend on.

### Testing with curl

```bash
# GET request
curl -i http://localhost:54321/functions/v1/hello-world \
  -H "Authorization: Bearer $(supabase status --output json | jq -r '.ANON_KEY')"

# POST request with JSON body
curl -i -X POST http://localhost:54321/functions/v1/hello-world \
  -H "Authorization: Bearer eyJhbGciOi..." \
  -H "Content-Type: application/json" \
  -d '{"name": "World"}'
```

> **Tip:** For local development, pass the local `anon` key from `supabase status` as the Bearer token.

---

## Part 3: Working with Supabase Services

### Creating a Supabase Client Inside Edge Functions

```typescript
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req: Request) => {
  // Client scoped to the requesting user (respects RLS)
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    {
      global: {
        headers: { Authorization: req.headers.get("Authorization")! },
      },
    }
  );

  // Admin client (bypasses RLS — use with care)
  const supabaseAdmin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  return new Response("ok");
});
```

The environment variables `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` are automatically available in deployed Edge Functions. You do not need to set them manually.

There are two client patterns you need to understand:

1. **User client** (with `anon` key + forwarded Authorization header): Respects Row Level Security (RLS). Use this when acting on behalf of the authenticated user.
2. **Admin/service client** (with `service_role` key): Bypasses RLS entirely. Use this for server-side operations that need full database access — but never expose this key to the browser.

### Reading and Writing to the Database

```typescript
Deno.serve(async (req: Request) => {
  const supabaseAdmin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  // INSERT a row
  const { data: inserted, error: insertError } = await supabaseAdmin
    .from("todos")
    .insert({ title: "Buy groceries", is_complete: false })
    .select()
    .single();

  // SELECT rows
  const { data: todos, error: selectError } = await supabaseAdmin
    .from("todos")
    .select("*")
    .eq("is_complete", false)
    .order("created_at", { ascending: false });

  return new Response(JSON.stringify({ inserted, todos }), {
    headers: { "Content-Type": "application/json" },
  });
});
```

### Accessing Supabase Auth (Verifying JWT Tokens)

```typescript
Deno.serve(async (req: Request) => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    {
      global: {
        headers: { Authorization: req.headers.get("Authorization")! },
      },
    }
  );

  // Get the authenticated user from the JWT
  const {
    data: { user },
    error,
  } = await supabase.auth.getUser();

  if (error || !user) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  return new Response(
    JSON.stringify({ userId: user.id, email: user.email }),
    { headers: { "Content-Type": "application/json" } }
  );
});
```

### Using Supabase Storage

```typescript
Deno.serve(async (req: Request) => {
  const supabaseAdmin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  // Upload a file
  const file = new Blob(["Hello, Storage!"], { type: "text/plain" });
  const { data: uploadData, error: uploadError } = await supabaseAdmin.storage
    .from("documents")
    .upload("reports/hello.txt", file, { contentType: "text/plain" });

  // Download a file
  const { data: downloadData, error: downloadError } =
    await supabaseAdmin.storage.from("documents").download("reports/hello.txt");

  const text = downloadData ? await downloadData.text() : null;

  return new Response(JSON.stringify({ uploaded: uploadData, content: text }), {
    headers: { "Content-Type": "application/json" },
  });
});
```

### Environment Variables and Secrets

Set secrets for your remote project:

```bash
# Set individual secrets
supabase secrets set STRIPE_SECRET_KEY=sk_live_abc123
supabase secrets set OPENAI_API_KEY=sk-xyz789

# Set multiple from a .env file
supabase secrets set --env-file ./supabase/.env

# List current secrets (names only)
supabase secrets list
```

For local development, create `supabase/.env`:

```
STRIPE_SECRET_KEY=sk_test_abc123
OPENAI_API_KEY=sk-test-xyz789
```

Then serve with: `supabase functions serve --env-file ./supabase/.env`

Access in code: `Deno.env.get("STRIPE_SECRET_KEY")`

**Default environment variables** available in all deployed Edge Functions without any configuration:
- `SUPABASE_URL` — Your project's API URL
- `SUPABASE_ANON_KEY` — The anonymous (public) API key
- `SUPABASE_SERVICE_ROLE_KEY` — The service role key (server-side only)
- `SUPABASE_DB_URL` — Direct Postgres connection string (useful for raw SQL with libraries like `postgres`)

---

## Part 4: Advanced Patterns

### CORS Configuration

Unlike Supabase's REST API (PostgREST) which handles CORS automatically, Edge Functions require you to configure CORS headers manually. This is because Edge Functions are custom HTTP handlers where you control the full response. If you skip this step, every browser-based client calling your function will get a CORS error. The recommended approach is a shared CORS module reused across all functions:

```typescript
// supabase/functions/_shared/cors.ts
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS, PUT, DELETE",
};
```

Use it in every function:

```typescript
import { corsHeaders } from "../_shared/cors.ts";

Deno.serve(async (req: Request) => {
  // Handle CORS preflight — this MUST be first
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const data = { message: "Hello with CORS!" };
    return new Response(JSON.stringify(data), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
```

### Shared Modules

Place reusable code in `_shared/`. It is not deployed as an independent function:

```typescript
// supabase/functions/_shared/supabase-client.ts
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

export function getServiceClient() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );
}

export function getUserClient(req: Request) {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    {
      global: {
        headers: { Authorization: req.headers.get("Authorization")! },
      },
    }
  );
}
```

### Error Handling Best Practices

```typescript
import { corsHeaders } from "../_shared/cors.ts";

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // Validate method
    if (req.method !== "POST") {
      throw Object.assign(new Error("Method not allowed"), { status: 405 });
    }

    // Validate and parse body
    const contentType = req.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) {
      throw Object.assign(new Error("Content-Type must be application/json"), {
        status: 400,
      });
    }

    const body = await req.json();
    if (!body.email) {
      throw Object.assign(new Error("email is required"), { status: 422 });
    }

    // ... business logic ...

    return new Response(JSON.stringify({ success: true }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    const status = (err as any).status || 500;
    console.error(`[ERROR] ${err.message}`);
    return new Response(JSON.stringify({ error: err.message }), {
      status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
```

### Calling External APIs

Edge Functions have full outbound network access via the standard `fetch` API. This makes them ideal as secure API proxies — your secret keys stay on the server while the client calls your Edge Function. Here is an example proxying to the OpenAI API:

```typescript
Deno.serve(async (req: Request) => {
  const apiKey = Deno.env.get("OPENAI_API_KEY")!;

  const openaiRes = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "gpt-4o-mini",
      messages: [{ role: "user", content: "Say hello in 3 languages." }],
    }),
  });

  const result = await openaiRes.json();

  return new Response(JSON.stringify(result), {
    headers: { "Content-Type": "application/json" },
  });
});
```

### Scheduled Functions (Cron)

Supabase does not have a built-in "scheduled function" primitive in the Edge Functions layer itself. Instead, you use the `pg_cron` Postgres extension (combined with `pg_net` for HTTP requests) to invoke Edge Functions on a schedule from within your database. This is powerful because it keeps scheduling logic in Postgres and requires no external scheduler. Configure this in the Supabase SQL Editor, via a migration file, or through the Dashboard under **Cron Jobs**:

```sql
-- Enable the required extensions
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Schedule an Edge Function to run every hour
SELECT cron.schedule(
  'invoke-cleanup',                            -- job name
  '0 * * * *',                                 -- every hour
  $$
  SELECT net.http_post(
    url    := 'https://<project-ref>.supabase.co/functions/v1/cleanup',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || '<ANON_KEY_OR_SERVICE_ROLE_KEY>'
    ),
    body   := '{"source": "cron"}'::jsonb
  ) AS request_id;
  $$
);

-- List scheduled jobs
SELECT * FROM cron.job;

-- Remove a scheduled job
SELECT cron.unschedule('invoke-cleanup');
```

> **Important:** Configure the cron function with `verify_jwt = false` in `config.toml`, or include a valid service role key in the Authorization header.

### Webhook Handlers (e.g., Stripe)

```typescript
import Stripe from "https://esm.sh/stripe@14?target=deno";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, {
  apiVersion: "2024-06-20",
});

const cryptoProvider = Stripe.createSubtleCryptoProvider();

Deno.serve(async (req: Request) => {
  const body = await req.text();
  const signature = req.headers.get("Stripe-Signature")!;

  try {
    const event = await stripe.webhooks.constructEventAsync(
      body,
      signature,
      Deno.env.get("STRIPE_WEBHOOK_SECRET")!,
      undefined,
      cryptoProvider
    );

    switch (event.type) {
      case "checkout.session.completed":
        console.log("Payment succeeded:", event.data.object.id);
        break;
      default:
        console.log(`Unhandled event type: ${event.type}`);
    }

    return new Response(JSON.stringify({ received: true }), { status: 200 });
  } catch (err) {
    return new Response(`Webhook Error: ${err.message}`, { status: 400 });
  }
});
```

Deploy this function without JWT verification since Stripe cannot send your JWT:

```toml
# supabase/config.toml
[functions.stripe-webhook]
verify_jwt = false
```

### Database Webhooks Triggering Edge Functions

Configure in the Supabase Dashboard under **Database > Webhooks**, or via SQL:

```sql
-- Create a webhook that fires on INSERT to the "orders" table
-- and calls your Edge Function
CREATE TRIGGER on_order_created
  AFTER INSERT ON orders
  FOR EACH ROW
  EXECUTE FUNCTION supabase_functions.http_request(
    'https://<project-ref>.supabase.co/functions/v1/process-order',
    'POST',
    '{"Content-Type": "application/json", "Authorization": "Bearer <SERVICE_ROLE_KEY>"}',
    '{}',
    '5000'
  );
```

Database Webhooks use `pg_net` under the hood, making them asynchronous and non-blocking for your database transactions. The HTTP request is queued and sent after the transaction commits, so a slow Edge Function will never block an INSERT or UPDATE. You can monitor webhook delivery by querying the `net._http_response` table to check for failed requests.

Note that when using Database Webhooks to trigger Edge Functions, you typically need either `verify_jwt = false` on the function or you must include a valid service role key in the webhook's Authorization header, since the database trigger itself does not have a user JWT.

---

## Part 5: Testing and Debugging

### Local Testing Setup

The local testing workflow requires two processes: the Supabase local stack (database, auth, storage) and the Edge Functions server. Start them in separate terminal windows:

```bash
# Terminal 1: Start the full local Supabase stack
supabase start

# Terminal 2: Serve Edge Functions with your environment variables
supabase functions serve --env-file ./supabase/.env
```

Once both are running, you can test functions by sending HTTP requests to `http://localhost:54321/functions/v1/<function-name>`. The local stack's anon key (shown in `supabase status` output) must be included as a Bearer token unless you have disabled JWT verification for that function.

```bash
# In a third terminal, run your Deno tests
deno test --allow-all supabase/functions/tests/
```

### Unit Testing Edge Functions

Create tests in `supabase/functions/tests/`:

```typescript
// supabase/functions/tests/hello-world-test.ts
import {
  assert,
  assertEquals,
} from "https://deno.land/std@0.224.0/assert/mod.ts";
import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "http://localhost:54321";
const supabaseKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "<local-anon-key>";

Deno.test("hello-world function returns expected response", async () => {
  const supabase: SupabaseClient = createClient(supabaseUrl, supabaseKey);

  const { data, error } = await supabase.functions.invoke("hello-world", {
    body: { name: "Test" },
  });

  assert(!error, `Function returned an error: ${error?.message}`);
  assertEquals(typeof data.message, "string");
  assert(data.message.includes("Hello"));
});
```

Run with:

```bash
deno test --allow-all --env=./supabase/.env supabase/functions/tests/hello-world-test.ts
```

### Logging and Monitoring

```typescript
// Structured logging inside your function
console.log(JSON.stringify({
  level: "info",
  event: "order_processed",
  orderId: "abc-123",
  duration_ms: 42,
}));

// Errors are captured automatically
console.error("Something went wrong:", error);
```

View logs for deployed functions:

```bash
supabase functions logs hello-world --project-ref <ref>
```

### Debugging with Chrome DevTools

The Supabase CLI supports the V8 inspector protocol, letting you use Chrome DevTools, VS Code, or IntelliJ IDEA to step through your Edge Function code. Three debug modes are available:

- **`run`** — Attaches inspector without pausing. Good for long-running functions where you want to set breakpoints on demand.
- **`brk`** — Pauses execution at the very first line. Ideal for debugging startup issues.
- **`wait`** — Pauses execution until a debugger session connects. Useful when you need to catch the very first request.

```bash
# Start with inspector (brk pauses at first line)
supabase functions serve --debug-mode brk

# Open chrome://inspect in Chrome and connect to the listed target
```

In VS Code, you can also create a launch configuration that attaches to the inspector port for a seamless debugging experience with breakpoints, watch expressions, and the call stack.

### Common Errors and Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `401 Unauthorized` | Missing or invalid JWT | Pass `Authorization: Bearer <anon-key>` header |
| CORS errors in browser | No CORS headers returned | Add shared `corsHeaders` and handle `OPTIONS` |
| `Boot Failure` in logs | Syntax error or missing import | Run `deno check supabase/functions/my-func/index.ts` |
| Function not found (404) | Function not deployed or typo | Check `supabase functions list` |
| `RelayError` locally | `supabase start` not running | Run `supabase start` before `functions serve` |
| Secrets not available | Not set or not linked | Run `supabase secrets list` to verify |

---

## Part 6: Deployment

When you are ready to go live, deployment is a single CLI command. The CLI bundles your TypeScript source and its dependency tree into an ESZip archive, uploads it to the Supabase platform, and distributes it across the global edge network. Deployments are atomic — the new version replaces the old one with zero downtime.

### Deploy a Single Function

```bash
supabase functions deploy hello-world
```

### Deploy All Functions

```bash
supabase functions deploy
```

Since Supabase CLI v1.62.0, running `deploy` without a function name deploys every function in `supabase/functions/`.

### Deploy Flags and Options

```bash
# Deploy without JWT verification
supabase functions deploy hello-world --no-verify-jwt

# Deploy to a specific project
supabase functions deploy hello-world --project-ref abcdefghijklmnop

# Deploy with debug output
supabase functions deploy hello-world --debug
```

### Function Configuration via config.toml

```toml
# supabase/config.toml

[functions.hello-world]
verify_jwt = true

[functions.stripe-webhook]
verify_jwt = false

[functions.image-processor]
verify_jwt = true
import_map = "./functions/image-processor/deno.json"
```

### Viewing Logs

```bash
# Tail logs in real time
supabase functions logs hello-world --project-ref <ref>

# Logs are also visible in the Supabase Dashboard under Edge Functions > Logs
```

### Updating Deployed Functions

Simply re-deploy. Each deployment creates a new version:

```bash
# Edit your function code, then:
supabase functions deploy hello-world
```

### Rollback Strategies

Supabase does not provide a built-in rollback command. Use these strategies instead:

1. **Git-based rollback:** Check out the previous commit and re-deploy.
2. **Versioned directories:** Keep tagged copies so you can deploy a known-good version.

```bash
git checkout <previous-commit> -- supabase/functions/hello-world/
supabase functions deploy hello-world
```

### CI/CD Integration with GitHub Actions

```yaml
# .github/workflows/deploy-edge-functions.yml
name: Deploy Edge Functions

on:
  push:
    branches: [main]
    paths:
      - "supabase/functions/**"
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest

    env:
      SUPABASE_ACCESS_TOKEN: ${{ secrets.SUPABASE_ACCESS_TOKEN }}
      PROJECT_REF: ${{ secrets.SUPABASE_PROJECT_REF }}

    steps:
      - uses: actions/checkout@v4

      - uses: supabase/setup-cli@v1
        with:
          version: latest

      - name: Link project
        run: supabase link --project-ref $PROJECT_REF

      - name: Deploy all functions
        run: supabase functions deploy --project-ref $PROJECT_REF

      - name: Set secrets from .env
        run: supabase secrets set --env-file ./supabase/.env.production --project-ref $PROJECT_REF
```

Store `SUPABASE_ACCESS_TOKEN` and `SUPABASE_PROJECT_REF` in your repository's GitHub Actions Secrets. The `paths` filter ensures the workflow only runs when function code changes, avoiding unnecessary deployments on unrelated commits.

For more complex setups, you can add a test step before deployment that runs `deno test` against your function tests, or deploy individual functions in parallel jobs. Bitbucket Pipelines and GitLab CI follow the same pattern — install the CLI, link the project, then run the deploy command.

---

## End-to-End Example: A Contact Form Function

Here is a complete, production-ready example that ties together everything covered above — CORS handling, request validation, database writes, and error handling:

```typescript
// supabase/functions/submit-contact/index.ts
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";

Deno.serve(async (req: Request) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // Validate method
    if (req.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), {
        status: 405,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Parse and validate body
    const { name, email, message } = await req.json();
    if (!name || !email || !message) {
      return new Response(
        JSON.stringify({ error: "name, email, and message are required" }),
        {
          status: 422,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // Create admin client to insert into the contacts table
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const { data, error } = await supabase
      .from("contacts")
      .insert({ name, email, message })
      .select()
      .single();

    if (error) throw error;

    console.log(JSON.stringify({ level: "info", event: "contact_submitted", id: data.id }));

    return new Response(JSON.stringify({ success: true, id: data.id }), {
      status: 201,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("[submit-contact] Error:", err.message);
    return new Response(JSON.stringify({ error: "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
```

Deploy it and test:

```bash
supabase functions deploy submit-contact

curl -X POST https://<project-ref>.supabase.co/functions/v1/submit-contact \
  -H "Authorization: Bearer <ANON_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane", "email": "jane@example.com", "message": "Hello!"}'
```

---

## Quick Reference

| Command | Description |
|---|---|
| `supabase init` | Initialize a local Supabase project |
| `supabase login` | Authenticate with Supabase |
| `supabase link --project-ref <ref>` | Link to a remote project |
| `supabase functions new <name>` | Scaffold a new Edge Function |
| `supabase functions serve` | Serve all functions locally |
| `supabase functions serve --env-file .env` | Serve with custom env vars |
| `supabase functions deploy` | Deploy all functions |
| `supabase functions deploy <name>` | Deploy a specific function |
| `supabase functions deploy --no-verify-jwt` | Deploy without JWT checks |
| `supabase functions logs <name>` | View function logs |
| `supabase secrets set KEY=VALUE` | Set a production secret |
| `supabase secrets list` | List all secrets |

---

## Sources

- [Edge Functions Overview](https://supabase.com/docs/guides/functions)
- [Getting Started (CLI)](https://supabase.com/docs/guides/functions/quickstart)
- [Architecture](https://supabase.com/docs/guides/functions/architecture)
- [Securing Edge Functions / Auth](https://supabase.com/docs/guides/functions/auth)
- [CORS Configuration](https://supabase.com/docs/guides/functions/cors)
- [Environment Variables & Secrets](https://supabase.com/docs/guides/functions/secrets)
- [Function Configuration](https://supabase.com/docs/guides/functions/function-configuration)
- [Deploy to Production](https://supabase.com/docs/guides/functions/deploy)
- [Testing Edge Functions](https://supabase.com/docs/guides/functions/unit-test)
- [Scheduling Functions](https://supabase.com/docs/guides/functions/schedule-functions)
- [Database Webhooks](https://supabase.com/docs/guides/database/webhooks)
- [GitHub Actions Deployment](https://supabase.com/docs/guides/functions/examples/github-actions)
- [CLI Reference](https://supabase.com/docs/reference/cli/supabase-functions-deploy)
