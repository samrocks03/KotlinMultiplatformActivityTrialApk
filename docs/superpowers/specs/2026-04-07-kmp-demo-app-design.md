# KMP Demo App — Design Specification

**Date:** 2026-04-07
**Status:** Approved
**Approach:** B — Multi-module with native SwiftUI for iOS, Jetpack Compose for Android, shared Kotlin business logic

---

## 1. Overview

A Kotlin Multiplatform demo application demonstrating clean architecture with:

- **Two GraphQL APIs** — Rick and Morty (read-only, rich data) + GraphQLZero (full CRUD)
- **Offline storage** — SQLDelight for favorites and cached data
- **Platform features** — Camera (CameraX / AVFoundation) and GPS (FusedLocation / CoreLocation)
- **Dependency injection** — Koin (KMP-native)
- **Native UI per platform** — Jetpack Compose (Android) and SwiftUI (iOS)

### GraphQL Endpoints

| API | URL | Auth | Purpose |
|-----|-----|------|---------|
| Rick and Morty | `https://rickandmortyapi.com/graphql` | None | Characters, locations, episodes (read-only) |
| GraphQLZero | `https://graphqlzero.almansi.me/api` | None | Posts CRUD (create, read, update, delete) |

---

## 2. Module Structure

```bash
KMP_Basic_App/
├── shared/                          <- KMP library (business logic + data)
│   ├── src/commonMain/kotlin/
│   │   ├── domain/
│   │   │   ├── model/               <- Character, Post, Location entities
│   │   │   ├── usecase/             <- GetCharacters, CreatePost, GetLocation...
│   │   │   └── repository/          <- Repository interfaces
│   │   ├── data/
│   │   │   ├── remote/              <- Apollo client impls of repositories
│   │   │   ├── local/               <- SQLDelight DAOs + offline storage
│   │   │   └── repository/          <- Repository implementations
│   │   ├── platform/                <- expect declarations (Camera, GPS)
│   │   └── di/                      <- Koin shared modules
│   ├── src/commonMain/graphql/
│   │   ├── rickandmorty/            <- R&M schema + operations
│   │   └── graphqlzero/             <- GQLZero schema + operations
│   ├── src/commonMain/sqldelight/   <- .sq files
│   ├── src/androidMain/kotlin/
│   │   └── platform/                <- actual Camera (CameraX), GPS (Fused)
│   ├── src/iosMain/kotlin/
│   │   └── platform/                <- actual Camera (AVFoundation), GPS (CoreLocation)
│   └── build.gradle.kts
│
├── composeApp/                      <- Android app (Jetpack Compose UI)
│   ├── src/androidMain/kotlin/
│   │   ├── ui/
│   │   │   ├── screens/             <- Compose screens per feature
│   │   │   ├── components/          <- Reusable Compose components
│   │   │   ├── navigation/          <- Navigation Compose setup
│   │   │   └── theme/               <- Material3 theme, colors, typography
│   │   ├── viewmodel/               <- Android ViewModels (consume shared UseCases)
│   │   └── di/                      <- Koin Android modules
│   └── build.gradle.kts
│
├── iosApp/                          <- iOS app (native SwiftUI)
│   ├── Sources/
│   │   ├── Screens/                 <- SwiftUI views per feature
│   │   ├── Components/              <- Reusable SwiftUI components
│   │   ├── ViewModels/              <- ObservableObjects (consume shared UseCases)
│   │   ├── Navigation/              <- NavigationStack + TabView
│   │   └── Theme/                   <- Colors, typography, SF Symbols
│   ├── iosApp.swift
│   └── Info.plist
│
└── build.gradle.kts                 <- Root build config
```

---

## 3. Data Flow Architecture

```
UI (Compose / SwiftUI)
  |  calls
ViewModel (platform-specific)
  |  calls
UseCase (shared Kotlin)
  |  calls
Repository (shared Kotlin)
  |  calls
GraphQL API  or  Local DB
(Apollo Kotlin)  (SQLDelight)
```

### State Pattern Per Platform

| Aspect | Android | iOS |
|--------|---------|-----|
| State holder | `ViewModel` + `MutableStateFlow` | `ObservableObject` + `@Published` |
| State collection | `collectAsStateWithLifecycle()` | `@StateObject` / `@ObservedObject` |
| Side effects | `SharedFlow` -> `LaunchedEffect` | `@Published` or Combine |
| Lifecycle | `viewModelScope` | SwiftUI view lifecycle |

### iOS-Kotlin Bridging

- **SKIE** (v0.10.0) bridges Kotlin `Flow` to Swift `AsyncSequence`
- Kotlin `suspend` functions become Swift `async` functions
- Koin provides DI from shared module into Swift ViewModels

---

## 4. Domain Models

### From Rick and Morty API

```kotlin
data class Character(
    val id: String,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val gender: String,
    val origin: LocationBrief,
    val location: LocationBrief,
    val imageUrl: String,
    val episodeIds: List<String>,
    val isFavorite: Boolean = false
)

enum class CharacterStatus { ALIVE, DEAD, UNKNOWN }

data class LocationBrief(val id: String?, val name: String)

data class Episode(
    val id: String,
    val name: String,
    val airDate: String,
    val episode: String
)

data class CharacterPage(
    val info: PageInfo,
    val results: List<Character>
)

data class PageInfo(
    val count: Int,
    val pages: Int,
    val next: Int?
)
```

### From GraphQLZero API

```kotlin
data class Post(
    val id: String,
    val title: String,
    val body: String,
    val userId: String
)
```

### Platform Features

```kotlin
data class CapturedPhoto(
    val filePath: String,
    val timestamp: Long,
    val location: GpsLocation? = null
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)
```

---

## 5. GraphQL Operations

### Rick and Morty (`src/commonMain/graphql/rickandmorty/`)

**GetCharactersQuery.graphql:**

```graphql
query GetCharacters($page: Int, $nameFilter: String) {
  characters(page: $page, filter: { name: $nameFilter }) {
    info { count pages next }
    results {
      id name status species gender image
      origin { id name }
      location { id name }
      episode { id }
    }
  }
}
```

**GetCharacterDetailQuery.graphql:**

```graphql
query GetCharacterDetail($id: ID!) {
  character(id: $id) {
    id name status species gender image
    origin { id name type dimension }
    location { id name type dimension }
    episode { id name air_date episode }
  }
}
```

### GraphQLZero (`src/commonMain/graphql/graphqlzero/`)

**GetPostsQuery.graphql:**

```graphql
query GetPosts($page: Int, $limit: Int) {
  posts(options: { paginate: { page: $page, limit: $limit } }) {
    data { id title body }
    meta { totalCount }
  }
}
```

**CreatePostMutation.graphql:**

```graphql
mutation CreatePost($title: String!, $body: String!) {
  createPost(input: { title: $title, body: $body }) {
    id title body
  }
}
```

**UpdatePostMutation.graphql:**

```graphql
mutation UpdatePost($id: ID!, $title: String!, $body: String!) {
  updatePost(id: $id, input: { title: $title, body: $body }) {
    id title body
  }
}
```

**DeletePostMutation.graphql:**

```graphql
mutation DeletePost($id: ID!) {
  deletePost(id: $id)
}
```

---

## 6. UseCases

| UseCase | Input | Output | Source |
|---------|-------|--------|--------|
| `GetCharactersUseCase` | page, filter? | `Result<CharacterPage>` | Rick & Morty API + SQLDelight favorites |
| `GetCharacterDetailUseCase` | id | `Result<Character>` with episodes | Rick & Morty API |
| `ToggleFavoriteUseCase` | characterId, character | `Result<Boolean>` (new state) | SQLDelight |
| `GetFavoritesUseCase` | -- | `Flow<List<Character>>` | SQLDelight |
| `GetPostsUseCase` | page, limit | `Result<List<Post>>` | GraphQLZero API |
| `CreatePostUseCase` | title, body | `Result<Post>` | GraphQLZero API |
| `UpdatePostUseCase` | id, title, body | `Result<Post>` | GraphQLZero API |
| `DeletePostUseCase` | id | `Result<Boolean>` | GraphQLZero API |
| `CapturePhotoUseCase` | -- | `Result<CapturedPhoto>` | Platform Camera |
| `GetCurrentLocationUseCase` | -- | `Result<GpsLocation>` | Platform GPS |

---

## 7. SQLDelight Schema

```sql
-- Favorite.sq
CREATE TABLE Favorite (
    character_id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    species TEXT NOT NULL,
    image_url TEXT NOT NULL,
    added_at INTEGER NOT NULL
);

selectAll:
SELECT * FROM Favorite ORDER BY added_at DESC;

insert:
INSERT OR REPLACE INTO Favorite(character_id, name, status, species, image_url, added_at)
VALUES (?, ?, ?, ?, ?, ?);

deleteById:
DELETE FROM Favorite WHERE character_id = ?;

isFavorite:
SELECT COUNT(*) FROM Favorite WHERE character_id = ?;
```

```sql
-- CapturedPhoto.sq
CREATE TABLE CapturedPhotoEntity (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    file_path TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    latitude REAL,
    longitude REAL,
    accuracy REAL
);

selectAll:
SELECT * FROM CapturedPhotoEntity ORDER BY timestamp DESC;

insert:
INSERT INTO CapturedPhotoEntity(file_path, timestamp, latitude, longitude, accuracy)
VALUES (?, ?, ?, ?, ?);

deleteById:
DELETE FROM CapturedPhotoEntity WHERE id = ?;
```

---

## 8. Screens & Navigation

### Tab Structure (5 tabs)

| Tab | Icon (Android) | Icon (iOS) | Screen |
|-----|---------------|------------|--------|
| Characters | `Icons.Default.People` | `person.3.fill` | Character list + detail |
| Posts | `Icons.Default.Article` | `doc.text.fill` | Posts CRUD |
| Camera | `Icons.Default.CameraAlt` | `camera.fill` | Capture + history |
| GPS | `Icons.Default.LocationOn` | `location.fill` | Location display |
| Favorites | `Icons.Default.Favorite` | `heart.fill` | Saved characters |

### Screen Details

**Characters Tab:**

- Paginated list with search/filter
- Pull-to-refresh
- Tap -> Character Detail (pushed)
- Character Detail: hero image, status badge, origin/location, episode list, favorite toggle

**Posts Tab:**

- Paginated list
- FAB (Android) / toolbar button (iOS) -> Create Post modal
- Tap -> Edit Post modal
- Swipe-to-delete with confirmation
- Empty state when no posts

**Camera Tab:**

- "Capture Photo" button
- Opens native camera (CameraX on Android, UIImagePickerController on iOS)
- Photo preview with GPS coordinates overlay
- History list of captured photos below

**GPS Tab:**

- Permission request flow with explanation
- Current coordinates display (lat, lng, accuracy)
- Last updated timestamp
- Refresh button

**Favorites Tab:**

- SQLDelight-backed reactive list
- Same card/row style as Characters
- Tap -> Character Detail
- Swipe to remove from favorites
- Empty state: "No favorites yet"

### Platform UI Patterns

**Android (Material3):**

- `NavigationBar` bottom tabs with Material icons
- `TopAppBar` with back navigation for detail screens
- `Scaffold` wrapping every screen
- Dynamic color theming (Android 12+)
- `Snackbar` for confirmations
- `SearchBar` for character search
- `FloatingActionButton` for create post
- `SwipeToDismiss` for delete actions

**iOS (HIG + SwiftUI):**

- `TabView` with SF Symbols
- `NavigationStack` per tab with native back swipe
- `.navigationTitle()` with large/inline styles
- `.searchable()` for character search
- `.refreshable {}` for pull-to-refresh
- `.sheet()` for create/edit modals
- `.swipeActions {}` for delete
- `.alert()` for confirmations
- System background colors

---

## 9. Platform expect/actual Declarations

### Camera

```kotlin
// commonMain
expect class PlatformCamera {
    suspend fun capturePhoto(): CapturedPhoto
    fun isAvailable(): Boolean
}

// androidMain — CameraX + ActivityResultContracts
// iosMain — UIImagePickerController via Kotlin/Native interop
```

### GPS

```kotlin
// commonMain
expect class PlatformLocationProvider {
    suspend fun getCurrentLocation(): GpsLocation
    suspend fun requestPermission(): Boolean
    fun isPermissionGranted(): Boolean
}

// androidMain — FusedLocationProviderClient
// iosMain — CLLocationManager
```

---

## 10. Dependency Stack

| Library | Version | Purpose | Module |
|---------|---------|---------|--------|
| Kotlin | 2.3.20 | Language | All |
| Compose Multiplatform | 1.10.3 | Android Compose UI | composeApp |
| Apollo Kotlin | 4.4.3 | GraphQL client + codegen | shared |
| SQLDelight | 2.0.2 | Offline DB | shared |
| Koin | 4.0.0 | DI (KMP) | shared + composeApp |
| Koin Compose | 4.0.0 | `koinViewModel()` | composeApp |
| Kotlinx Coroutines | 1.10.2 | Async | shared |
| Kotlinx Serialization | 1.7.3 | JSON | shared |
| Coil3 | 3.1.0 | Image loading (Compose) | composeApp |
| SKIE | 0.10.0 | Kotlin->Swift bridging | shared (iOS) |
| AndroidX Lifecycle | 2.10.0 | ViewModel | composeApp |
| CameraX | 1.4.1 | Android camera | shared (androidMain) |
| Play Services Location | 21.3.0 | Android GPS | shared (androidMain) |

### Koin DI Configuration

**Shared module (commonMain):**

- Apollo clients (one per GraphQL endpoint)
- Repository implementations
- All UseCases as `factory`

**Android module (composeApp):**

- Platform implementations (Camera, GPS) with Android `Context`
- All ViewModels as `viewModel`

**iOS setup (Swift):**

- `KoinHelper` class to access shared Koin graph
- Platform implementations initialized in Swift and provided to Koin

---

## 11. Error Handling

### Shared Layer

```kotlin
sealed class AppError {
    data class Network(val message: String) : AppError()
    data class GraphQL(val errors: List<String>) : AppError()
    data class Permission(val type: String) : AppError()
    data class Storage(val message: String) : AppError()
    data object Unknown : AppError()
}
```

All UseCases return `Result<T>`. Repositories wrap Apollo responses:

- `response.exception` -> `AppError.Network`
- `response.errors` -> `AppError.GraphQL`
- Success -> `Result.success(data.toDomain())`

### UI Layer

Both platforms use a `UiState` pattern:

- `Loading` -> show spinner/skeleton
- `Success(data)` -> show content
- `Error(message)` -> show error with retry

### Offline Handling

- Apollo network failure -> check SQLDelight cache -> show cached data with "offline" banner
- Favorites always work offline (SQLDelight only)
- Posts CRUD shows error toast/alert when offline

---

## 12. Testing Strategy

| Layer | What to Test | Tool | Location |
|-------|-------------|------|----------|
| Domain (UseCases) | Business logic, data transformation | `kotlin-test` + `runTest` | `shared/src/commonTest/` |
| Data (Repositories) | Apollo response mapping, SQLDelight queries | `kotlin-test` + fakes | `shared/src/commonTest/` |
| Android ViewModels | State transitions, event handling | JUnit + Turbine + `runTest` | `composeApp/src/test/` |
| Android UI | Screen rendering, interactions | Compose UI testing | `composeApp/src/androidTest/` |
| iOS ViewModels | State transitions | XCTest + `async` | `iosApp/Tests/` |
| iOS UI | Screen rendering, navigation | XCUITest | `iosApp/UITests/` |

### Not in Scope

- End-to-end GraphQL integration tests (external APIs)
- Camera/GPS hardware tests (manual on device)
- Performance/load testing

---

## 13. Android Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

## 14. iOS Permissions (Info.plist)

```xml
<key>NSCameraUsageDescription</key>
<string>Take photos to attach to your posts</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Tag your photos and posts with your current location</string>
```
