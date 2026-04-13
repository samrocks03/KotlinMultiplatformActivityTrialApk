# Integrating Supabase Edge Functions with Kotlin Multiplatform (KMP)

> Practical integration guide for adding Supabase Edge Functions to a KMP app with shared module architecture, Apollo GraphQL, SQLDelight, Koin DI, and kotlinx.serialization.

---

## 1. Supabase Kotlin SDK (`supabase-kt`)

### What Is supabase-kt?

[supabase-kt](https://github.com/supabase-community/supabase-kt) is the official Kotlin Multiplatform client for Supabase. It provides a unified API surface across Android, iOS, JVM, JS, and WASM targets. The SDK is modular -- you install only the pieces you need as separate Gradle dependencies, and each module is installed as a plugin on the `SupabaseClient` instance.

**Current stable version: 3.5.0** (Kotlin 2.3.20, Ktor 3.4.2)

### Available Modules

| Module | Artifact ID | Purpose |
|--------|-------------|---------|
| Auth | `auth-kt` | Authentication (signup, login, OAuth, session management) |
| PostgREST | `postgrest-kt` | Direct database queries via PostgREST API |
| Functions | `functions-kt` | Invoke Supabase Edge Functions |
| Storage | `storage-kt` | File upload/download/management |
| Realtime | `realtime-kt` | Real-time database subscriptions via WebSockets |
| Apollo GraphQL | `apollo-graphql` | Apollo Client integration for Supabase GraphQL |
| Compose Auth | `compose-auth` | Native Google and Apple Auth for Compose Multiplatform |
| Compose Auth UI | `compose-auth-ui` | Pre-built auth UI components for Compose |
| Coil3 Integration | `coil3-integration` | Image loading from Supabase Storage via Coil3 |

All artifacts live under the group `io.github.jan-tennert.supabase`.

### Gradle Setup for Your KMP Project

#### Step 1: Add to `libs.versions.toml`

```toml
[versions]
# ... existing versions ...
supabase = "3.5.0"
ktor = "3.4.2"

[libraries]
# Supabase BOM (manages versions for all modules)
supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }
supabase-functions = { module = "io.github.jan-tennert.supabase:functions-kt" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt" }
supabase-storage = { module = "io.github.jan-tennert.supabase:storage-kt" }

# Ktor engines (one per platform target)
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

Note: When using the BOM, individual module declarations omit `version.ref` -- the BOM controls versions.

#### Step 2: Update `shared/build.gradle.kts`

```kotlin
sourceSets {
    commonMain.dependencies {
        // Existing dependencies...
        implementation(libs.apollo.runtime)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)

        // Supabase (BOM + modules)
        implementation(platform(libs.supabase.bom))
        implementation(libs.supabase.auth)
        implementation(libs.supabase.functions)
        implementation(libs.supabase.postgrest)
    }
    androidMain.dependencies {
        implementation(libs.sqldelight.android.driver)
        implementation(libs.ktor.client.okhttp)
    }
    iosMain.dependencies {
        implementation(libs.sqldelight.native.driver)
        implementation(libs.ktor.client.darwin)
    }
}
```

The BOM ensures all supabase-kt modules share a consistent version. Each platform target needs its own Ktor HTTP engine: **OkHttp** for Android, **Darwin** (NSURLSession) for iOS.

---

## 2. Calling Edge Functions from KMP

### Approach A: Using the `Functions` Module (Recommended)

The `functions-kt` module provides a type-safe, serialization-aware API for invoking edge functions. It automatically handles auth headers when an authenticated session exists.

#### Basic Invocation

```kotlin
// In commonMain
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.invoke
import kotlinx.serialization.Serializable

@Serializable
data class ProcessRequest(
    val documentId: String,
    val action: String
)

@Serializable
data class ProcessResponse(
    val status: String,
    val resultUrl: String
)

// Invoke with a serializable body, decode the response
suspend fun processDocument(
    supabase: SupabaseClient,
    request: ProcessRequest
): ProcessResponse {
    val response = supabase.functions.invoke(
        function = "process-document",
        body = request
    )
    return response.body<ProcessResponse>()
}
```

#### Invocation with Headers and Region

```kotlin
suspend fun invokeWithOptions(supabase: SupabaseClient) {
    val response = supabase.functions.invoke(
        function = "my-function",
        body = mapOf("key" to "value"),
        headers = Headers.build {
            append("X-Custom-Header", "custom-value")
        },
        region = FunctionRegion.UsEast1
    )
    val data = response.body<MyResponseType>()
}
```

### Approach B: Direct Ktor Client (Alternative)

For cases where you need full HTTP control or want to call non-Supabase edge functions, you can use Ktor directly. This is useful during migration when some endpoints are still plain REST.

```kotlin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

class EdgeFunctionHttpClient(
    private val httpClient: HttpClient,
    private val supabaseUrl: String,
    private val anonKey: String
) {
    suspend inline fun <reified TReq, reified TRes> invoke(
        functionName: String,
        body: TReq,
        accessToken: String? = null
    ): TRes {
        val response = httpClient.post("$supabaseUrl/functions/v1/$functionName") {
            contentType(ContentType.Application.Json)
            header("apikey", anonKey)
            accessToken?.let {
                header("Authorization", "Bearer $it")
            }
            setBody(body)
        }
        return response.body()
    }
}
```

### Error Handling Patterns

```kotlin
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.*

sealed class EdgeFunctionResult<out T> {
    data class Success<T>(val data: T) : EdgeFunctionResult<T>()
    data class Error(val message: String, val code: Int? = null) : EdgeFunctionResult<Nothing>()
}

suspend inline fun <reified T> safeEdgeFunctionCall(
    crossinline block: suspend () -> T
): EdgeFunctionResult<T> {
    return try {
        EdgeFunctionResult.Success(block())
    } catch (e: ResponseException) {
        EdgeFunctionResult.Error(
            message = e.message ?: "HTTP error",
            code = e.response.status.value
        )
    } catch (e: HttpRequestException) {
        EdgeFunctionResult.Error(message = "Network error: ${e.message}")
    } catch (e: Exception) {
        EdgeFunctionResult.Error(message = "Unexpected error: ${e.message}")
    }
}
```

### Usage Pattern: Consuming Edge Function Results in a ViewModel

```kotlin
// androidApp or shared presentation layer
class DocumentViewModel(
    private val processDocumentUseCase: ProcessDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Idle)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    fun processDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.value = DocumentUiState.Loading
            val result = safeEdgeFunctionCall {
                processDocumentUseCase(documentId, "analyze")
            }
            _uiState.value = when (result) {
                is EdgeFunctionResult.Success -> DocumentUiState.Success(result.data)
                is EdgeFunctionResult.Error -> DocumentUiState.Error(result.message)
            }
        }
    }
}

sealed class DocumentUiState {
    data object Idle : DocumentUiState()
    data object Loading : DocumentUiState()
    data class Success(val document: ProcessedDocument) : DocumentUiState()
    data class Error(val message: String) : DocumentUiState()
}
```

---

## 3. Authentication Flow

### Supabase Auth in KMP

The `auth-kt` module handles the full authentication lifecycle across all KMP targets. It manages session persistence, automatic token refresh, and exposes a reactive `sessionStatus` flow. The module was renamed from `gotrue-kt` in version 3.0.0 and supports email/password, OAuth (Google, Apple, GitHub, etc.), magic links, phone OTP, and PKCE authentication flows.

#### Client Initialization with Auth

```kotlin
val supabase = createSupabaseClient(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseKey = "your-anon-key"
) {
    install(Auth) {
        // Session auto-refreshes by default
        // Tokens are persisted automatically on Android (SharedPreferences)
        // and iOS (Keychain)
    }
    install(Functions)
}
```

#### Observing Session State

```kotlin
import io.github.jan.supabase.auth.status.SessionStatus

// Collect session status reactively
supabase.auth.sessionStatus.collect { status ->
    when (status) {
        is SessionStatus.Authenticated -> {
            val session = status.session
            // session.accessToken is the JWT
            // session.refreshToken for manual refresh
            // session.user contains user metadata
        }
        is SessionStatus.NotAuthenticated -> {
            // Redirect to login
        }
        is SessionStatus.LoadingFromStorage -> {
            // Show loading spinner
        }
        is SessionStatus.RefreshFailure -> {
            when (status.cause) {
                is RefreshFailureCause.NetworkError -> {
                    // Retry will happen automatically
                }
                is RefreshFailureCause.InternalServerError -> {
                    // Server issue, retry will happen automatically
                }
            }
        }
    }
}
```

#### Auth Headers and Edge Functions

When `Auth` is installed on the same `SupabaseClient` as `Functions`, the SDK **automatically injects** the current session's access token as an `Authorization: Bearer <token>` header on every edge function call. No manual header management is needed.

If you need to call functions without auth, or with a service role key, you can override headers explicitly in the `invoke` call.

#### Token Refresh Flow

The SDK handles token refresh automatically:

1. On initialization, the SDK loads the persisted session from platform storage.
2. It checks the `expiresAt` field of the JWT.
3. When the token is close to expiry, it calls `/auth/v1/token?grant_type=refresh_token`.
4. The new access + refresh token pair is persisted and emitted via `sessionStatus`.
5. If refresh fails due to network issues, the SDK retries and emits `SessionStatus.RefreshFailure` without clearing the session.

#### Platform-Specific Session Storage

- **Android**: Sessions are stored in `EncryptedSharedPreferences` by default.
- **iOS**: Sessions are stored in the Keychain via `NSUserDefaults` or Keychain APIs.
- **Custom**: You can provide a custom `SessionManager` implementation for either platform.

#### Handling Auth in Edge Function Requests Manually

In scenarios where you need to make authenticated calls outside the Supabase client (for example, calling a third-party API that validates Supabase JWTs), you can extract the current token:

```kotlin
// commonMain/data/remote/AuthTokenProvider.kt
class AuthTokenProvider(private val supabase: SupabaseClient) {

    /**
     * Returns the current valid access token, or null if not authenticated.
     * The SDK handles refresh internally, so this always returns a fresh token
     * as long as the session is valid.
     */
    fun currentToken(): String? = supabase.auth.currentAccessTokenOrNull()

    /**
     * Suspend function that waits for authentication before returning the token.
     * Useful at app startup when the session is still loading from storage.
     */
    suspend fun awaitToken(): String {
        return supabase.auth.sessionStatus
            .filterIsInstance<SessionStatus.Authenticated>()
            .first()
            .session
            .accessToken
    }
}
```

#### PKCE Flow for Secure Mobile Auth

For mobile apps, Supabase recommends the PKCE (Proof Key for Code Exchange) flow, which is more secure than the implicit flow for native applications. The `auth-kt` module supports PKCE by default when using OAuth providers:

```kotlin
// Trigger OAuth with PKCE (default for mobile)
suspend fun signInWithGoogle(supabase: SupabaseClient) {
    supabase.auth.signInWith(Google) {
        // PKCE is used automatically
        // On Android, this opens Chrome Custom Tabs
        // On iOS, this opens ASWebAuthenticationSession
    }
}
```

---

## 4. Architecture Patterns

### Where Supabase Client Fits in Clean Architecture

```
┌─────────────────────────────────────────────┐
│  Presentation Layer (Android/iOS)           │
│  ViewModels / SwiftUI ObservableObjects     │
│  Consumes Use Cases via Koin                │
├─────────────────────────────────────────────┤
│  Domain Layer (commonMain)                  │
│  Use Cases, Domain Models, Repository       │
│  Interfaces (no Supabase dependency)        │
├─────────────────────────────────────────────┤
│  Data Layer (commonMain)                    │
│  Repository Implementations                 │
│  SupabaseClient, Edge Function calls        │
│  Mappers (DTO -> Domain Model)              │
├─────────────────────────────────────────────┤
│  Framework Layer (androidMain / iosMain)    │
│  Ktor Engine, Platform Session Storage      │
└─────────────────────────────────────────────┘
```

Key principle: **The domain layer never imports `supabase-kt`**. Repository interfaces are defined in domain; implementations live in the data layer and depend on `SupabaseClient`.

### Repository Pattern Wrapping Supabase Calls

```kotlin
// Domain layer: commonMain/domain/repository/DocumentRepository.kt
interface DocumentRepository {
    suspend fun processDocument(documentId: String, action: String): Result<ProcessedDocument>
    suspend fun getDocument(documentId: String): Result<Document>
}

// Data layer: commonMain/data/repository/DocumentRepositoryImpl.kt
class DocumentRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : DocumentRepository {

    override suspend fun processDocument(
        documentId: String,
        action: String
    ): Result<ProcessedDocument> = runCatching {
        val response = supabaseClient.functions.invoke(
            function = "process-document",
            body = ProcessDocumentDto(documentId, action)
        )
        val dto = response.body<ProcessDocumentResponseDto>()
        dto.toDomain()
    }

    override suspend fun getDocument(documentId: String): Result<Document> = runCatching {
        val dto = supabaseClient.from("documents")
            .select {
                filter { eq("id", documentId) }
            }
            .decodeSingle<DocumentDto>()
        dto.toDomain()
    }
}
```

### Use Case Layer

```kotlin
// Domain layer: commonMain/domain/usecase/ProcessDocumentUseCase.kt
class ProcessDocumentUseCase(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(
        documentId: String,
        action: String
    ): Result<ProcessedDocument> {
        return documentRepository.processDocument(documentId, action)
    }
}
```

### Koin DI Setup

```kotlin
// commonMain/di/SupabaseModule.kt
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import org.koin.dsl.module

val supabaseModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Auth)
            install(Functions)
            install(Postgrest)
        }
    }
}

val repositoryModule = module {
    single<DocumentRepository> { DocumentRepositoryImpl(get()) }
    // Add other repositories here
}

val useCaseModule = module {
    factory { ProcessDocumentUseCase(get()) }
    factory { GetDocumentUseCase(get()) }
}

// Aggregate all modules
val sharedModules = listOf(supabaseModule, repositoryModule, useCaseModule)
```

#### Android Koin Initialization

```kotlin
// androidApp/src/main/kotlin/App.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            modules(sharedModules)
        }
    }
}
```

#### iOS Koin Initialization

```kotlin
// shared/src/iosMain/kotlin/KoinHelper.kt
fun initKoin() {
    startKoin {
        modules(sharedModules)
    }
}
```

```swift
// iosApp/iOSApp.swift
@main
struct iOSApp: App {
    init() {
        KoinHelperKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

---

## 5. Practical Code Examples

### Complete Supabase Client Configuration

```kotlin
// commonMain/data/remote/SupabaseConfig.kt
object SupabaseConfig {
    // In production, load these from BuildConfig or environment
    const val URL = "https://your-project.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### DTOs with kotlinx.serialization

```kotlin
// commonMain/data/dto/EdgeFunctionDtos.kt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessDocumentDto(
    @SerialName("document_id") val documentId: String,
    val action: String
)

@Serializable
data class ProcessDocumentResponseDto(
    val status: String,
    @SerialName("result_url") val resultUrl: String,
    @SerialName("processed_at") val processedAt: String
) {
    fun toDomain() = ProcessedDocument(
        status = status,
        resultUrl = resultUrl,
        processedAt = processedAt
    )
}
```

### Full Repository with Edge Function + PostgREST

```kotlin
// commonMain/data/repository/TaskRepositoryImpl.kt
class TaskRepositoryImpl(
    private val supabase: SupabaseClient
) : TaskRepository {

    // Edge Function call
    override suspend fun analyzeTask(taskId: String): Result<TaskAnalysis> = runCatching {
        val response = supabase.functions.invoke(
            function = "analyze-task",
            body = AnalyzeTaskRequest(taskId = taskId)
        )
        response.body<TaskAnalysisDto>().toDomain()
    }

    // Direct PostgREST call
    override suspend fun getTasks(): Result<List<Task>> = runCatching {
        supabase.from("tasks")
            .select()
            .decodeList<TaskDto>()
            .map { it.toDomain() }
    }

    // PostgREST insert
    override suspend fun createTask(task: Task): Result<Task> = runCatching {
        supabase.from("tasks")
            .insert(task.toDto())
            .decodeSingle<TaskDto>()
            .toDomain()
    }
}
```

### Auth Repository

```kotlin
// commonMain/data/repository/AuthRepositoryImpl.kt
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean>
        get() = supabase.auth.sessionStatus.map { it is SessionStatus.Authenticated }

    override val currentAccessToken: String?
        get() = supabase.auth.currentAccessTokenOrNull()

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    override suspend fun refreshSession(): Result<Unit> = runCatching {
        supabase.auth.refreshCurrentSession()
    }
}
```

### Platform Considerations

**Android (`androidMain`):**
- Uses `ktor-client-okhttp` engine.
- Session storage uses `EncryptedSharedPreferences` out of the box.
- Token refresh works in the background automatically.
- Min SDK 26 required (or enable core library desugaring for lower).

**iOS (`iosMain`):**
- Uses `ktor-client-darwin` engine (NSURLSession under the hood).
- Session storage uses platform Keychain.
- Background refresh works with iOS background task scheduling.
- Full OAuth support on iOS and macOS; limited on watchOS/tvOS.

---

## 6. Migration Strategy: GraphQL to Supabase Edge Functions

Your app currently uses Apollo GraphQL with two services (Rick and Morty, GraphQLZero). Here is a phased migration strategy.

### Phase 1: Add Supabase Alongside GraphQL (Weeks 1-2)

Add supabase-kt dependencies without removing Apollo. New features use Supabase Edge Functions; existing features remain on GraphQL.

```kotlin
// Both clients coexist in Koin
val networkModule = module {
    // Existing Apollo client
    single { ApolloClient.Builder().serverUrl("...").build() }
    // New Supabase client
    single {
        createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.ANON_KEY) {
            install(Auth)
            install(Functions)
            install(Postgrest)
        }
    }
}
```

### Phase 2: Feature Flag Abstraction (Weeks 2-3)

Introduce a feature flag to route requests to either Apollo or Supabase per feature.

```kotlin
// commonMain/data/config/FeatureFlags.kt
object FeatureFlags {
    // Toggle per feature
    var useSupabaseForTasks: Boolean = false
    var useSupabaseForUsers: Boolean = false
}

// commonMain/data/repository/TaskRepositoryFactory.kt
class TaskRepositoryFactory(
    private val apolloTaskRepo: ApolloTaskRepositoryImpl,
    private val supabaseTaskRepo: SupabaseTaskRepositoryImpl
) : TaskRepository {

    private val activeRepo: TaskRepository
        get() = if (FeatureFlags.useSupabaseForTasks) supabaseTaskRepo else apolloTaskRepo

    override suspend fun getTasks() = activeRepo.getTasks()
    override suspend fun analyzeTask(taskId: String) = activeRepo.analyzeTask(taskId)
}
```

Register in Koin:

```kotlin
val repositoryModule = module {
    single { ApolloTaskRepositoryImpl(get()) }
    single { SupabaseTaskRepositoryImpl(get()) }
    single<TaskRepository> {
        TaskRepositoryFactory(
            apolloTaskRepo = get<ApolloTaskRepositoryImpl>(),
            supabaseTaskRepo = get<SupabaseTaskRepositoryImpl>()
        )
    }
}
```

### Phase 3: Migrate Feature by Feature (Weeks 3-6)

1. Write the Supabase Edge Function (Deno/TypeScript) for each feature.
2. Implement the corresponding `SupabaseXxxRepositoryImpl` in the shared module.
3. Enable the feature flag in testing/staging.
4. Validate parity with Apollo implementation using integration tests.
5. Flip the flag in production.

### Phase 4: Remove Apollo (Week 7+)

Once all features are migrated and stable:

1. Remove `FeatureFlags` and `RepositoryFactory` wrappers.
2. Remove Apollo dependencies from `libs.versions.toml` and `build.gradle.kts`.
3. Delete Apollo `.graphql` schema and operation files.
4. Remove the `apollo {}` block from the shared build script.
5. Clean up Koin modules to only register Supabase-backed repositories.

### Edge Function Example: Proxying an Existing GraphQL Query

During migration, you can write an Edge Function that wraps the same GraphQL API your app currently calls. This lets you move the network boundary without changing the response shape.

```typescript
// supabase/functions/get-characters/index.ts (Deno Edge Function)
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

serve(async (req) => {
  const { page } = await req.json()
  const query = `
    query GetCharacters($page: Int) {
      characters(page: $page) {
        results { id name status species image }
      }
    }
  `
  const response = await fetch("https://rickandmortyapi.com/graphql", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables: { page } }),
  })
  const data = await response.json()
  return new Response(JSON.stringify(data.data.characters.results), {
    headers: { "Content-Type": "application/json" },
  })
})
```

The corresponding KMP repository call:

```kotlin
// commonMain/data/repository/CharacterRepositoryImpl.kt
@Serializable
data class GetCharactersRequest(val page: Int)

@Serializable
data class CharacterDto(
    val id: String,
    val name: String,
    val status: String,
    val species: String,
    val image: String
)

class SupabaseCharacterRepository(
    private val supabase: SupabaseClient
) : CharacterRepository {
    override suspend fun getCharacters(page: Int): Result<List<Character>> = runCatching {
        val response = supabase.functions.invoke(
            function = "get-characters",
            body = GetCharactersRequest(page)
        )
        response.body<List<CharacterDto>>().map { it.toDomain() }
    }
}
```

### Backward Compatibility Checklist

- Domain models and use cases remain unchanged throughout migration.
- Repository interfaces never change -- only implementations swap.
- Koin bindings are the single switchover point.
- Feature flags allow instant rollback per feature.
- Edge Functions can proxy to the same upstream APIs during transition.
- Unit tests for use cases require zero changes since they mock repository interfaces.
- Integration tests should cover both Apollo and Supabase repository implementations during the transition period.

### Testing Strategy During Migration

```kotlin
// commonTest: Test the use case with a fake repository
class ProcessDocumentUseCaseTest {
    private val fakeRepo = object : DocumentRepository {
        override suspend fun processDocument(documentId: String, action: String) =
            Result.success(ProcessedDocument("done", "https://example.com/result", "2026-01-01"))
        override suspend fun getDocument(documentId: String) =
            Result.success(Document(documentId, "Test Doc"))
    }

    @Test
    fun processDocument_returnsSuccess() = runTest {
        val useCase = ProcessDocumentUseCase(fakeRepo)
        val result = useCase("doc-123", "analyze")
        assertTrue(result.isSuccess)
        assertEquals("done", result.getOrThrow().status)
    }
}
```

Because the domain layer has no Supabase imports, your existing tests remain valid regardless of which backend implementation is active.

---

## Quick Reference

| What | Where |
|------|-------|
| supabase-kt GitHub | [supabase-community/supabase-kt](https://github.com/supabase-community/supabase-kt) |
| Kotlin SDK Docs | [supabase.com/docs/reference/kotlin](https://supabase.com/docs/reference/kotlin/installing) |
| Functions.invoke API | [supabase.com/docs/reference/kotlin/functions-invoke](https://supabase.com/docs/reference/kotlin/functions-invoke) |
| Auth Reference | [supabase.com/docs/reference/kotlin/auth-signup](https://supabase.com/docs/reference/kotlin/auth-signup) |
| Edge Functions Guide | [supabase.com/docs/guides/functions](https://supabase.com/docs/guides/functions) |
| Latest Release | [3.5.0](https://github.com/supabase-community/supabase-kt/releases) |
| Ktor Engines | [ktor.io/docs/client-engines](https://ktor.io/docs/client-engines.html) |
| Edge Functions in Kotlin (write) | [manriif/supabase-edge-functions-kt](https://github.com/manriif/supabase-edge-functions-kt) |
