# KMP Basic App

A Kotlin Multiplatform application targeting **Android** (Jetpack Compose) and **iOS** (SwiftUI), showcasing a modern cross-platform architecture with shared business logic and native UI on each platform.

## What This App Does

The app has five main tabs:

- **Characters** — Browse characters from the [Rick and Morty GraphQL API](https://rickandmortyapi.com/graphql) with search, pagination, and tap-to-view detail pages. Favorite characters are persisted locally.
- **Posts** — Full CRUD for posts via the [GraphQLZero API](https://graphqlzero.almansi.me). Create, edit, and swipe-to-delete posts.
- **Camera** — Opens the native device camera to capture photos. Photos are saved locally with GPS coordinates attached.
- **GPS Location** — Displays your current coordinates with an interactive OpenStreetMap view (no API key required).
- **Favorites** — View and manage your favorited characters, persisted with SQLDelight.

## Architecture

```
shared/                  # Kotlin Multiplatform shared module
  commonMain/            # Domain models, use cases, repository interfaces, GraphQL queries
  androidMain/           # Android platform actuals (CameraX, FusedLocationProvider, SQLDelight driver)
  iosMain/               # iOS platform actuals (native drivers)

composeApp/              # Android app (Jetpack Compose)
  ui/                    # Screens, components, navigation, theme
  viewmodel/             # Android ViewModels
  di/                    # Koin dependency injection

iosApp/                  # iOS app (SwiftUI)
  Sources/
    Screens/             # SwiftUI screens
    Components/          # Reusable UI components
    ViewModels/          # ObservableObject ViewModels
    Theme/               # Notion design tokens
    Navigation/          # Tab-based navigation
    DI/                  # KoinHelper bridge
```

### Key Libraries

| Layer | Library |
|-------|---------|
| GraphQL | Apollo Kotlin |
| Local DB | SQLDelight |
| DI | Koin |
| Image Loading (Android) | Coil 3 + OkHttp |
| Maps (Android) | OSMDroid (OpenStreetMap) |
| Serialization | kotlinx.serialization |
| Navigation (Android) | Jetpack Navigation Compose |

### Design

Both platforms follow a **Notion-inspired warm minimalist theme** with whisper borders, warm neutral palette, and clean typography.

Animations include spring-based press-scale on cards, heart bounce on favorites, shimmer loading placeholders, hero image fade+scale entries, slide+fade screen transitions, and pulsing camera button.

## Prerequisites

- **Android Studio** Ladybug or newer (with Kotlin 2.3.20+)
- **JDK 11+**
- **Xcode 15+** (for iOS)
- An Android device/emulator or iOS simulator

## Getting Started

### Clone

```shell
git clone <repo-url>
cd KMP_Basic_App
```

### Android

Build the debug APK:

```shell
./gradlew :composeApp:assembleDebug
```

Install on a connected device:

```shell
./gradlew :composeApp:installDebug
```

Or open the project in Android Studio and run the `composeApp` configuration.

### iOS

1. Open the `iosApp/iosApp.xcodeproj` in Xcode
2. Select an iOS simulator or device
3. Build and run

> The shared KMP framework is built automatically as part of the Xcode build process.

## Permissions

The app requests the following permissions at runtime:

- **Camera** — to capture photos
- **Fine/Coarse Location** — to display GPS coordinates and tag photos
- **Internet** — for GraphQL API calls and map tiles

## Project Structure Details

| Directory | Purpose |
|-----------|---------|
| `shared/src/commonMain/graphql/` | GraphQL schemas and query definitions |
| `shared/src/commonMain/kotlin/.../domain/` | Models, repository interfaces, use cases |
| `shared/src/commonMain/kotlin/.../data/` | Apollo clients, mappers, repository implementations |
| `shared/src/commonTest/` | Unit tests for use cases |
| `composeApp/src/androidMain/` | Android Compose UI layer |
| `iosApp/iosApp/Sources/` | SwiftUI UI layer |
