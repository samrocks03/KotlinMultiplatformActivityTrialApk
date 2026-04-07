# KMP Demo App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a KMP demo app with two GraphQL APIs (Rick & Morty + GraphQLZero), SQLDelight offline storage, Camera/GPS platform features, Koin DI, Jetpack Compose (Android) and native SwiftUI (iOS).

**Architecture:** Approach B — Multi-module. `shared` KMP library holds domain models, use cases, repositories, Apollo GraphQL, SQLDelight, and expect/actual platform declarations. `composeApp` is the Android app with Jetpack Compose UI and ViewModels. `iosApp` is the iOS app with native SwiftUI views and ObservableObject ViewModels. Both platforms consume shared UseCases via Koin DI.

**Tech Stack:** Kotlin 2.3.20, Apollo Kotlin 4.4.3, SQLDelight 2.0.2, Koin 4.0.0, Coil3 3.1.0, SKIE 0.10.0, CameraX 1.4.1, Play Services Location 21.3.0, Compose Multiplatform 1.10.3 (Android UI only), SwiftUI (iOS UI).

**Spec:** `docs/superpowers/specs/2026-04-07-kmp-demo-app-design.md`

---

## File Structure

### shared/ module (new)

```
shared/
├── build.gradle.kts
├── src/
│   ├── commonMain/
│   │   ├── kotlin/com/example/kmp_basic_app/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Character.kt
│   │   │   │   │   ├── Post.kt
│   │   │   │   │   └── Platform.kt          (GpsLocation, CapturedPhoto)
│   │   │   │   ├── repository/
│   │   │   │   │   ├── CharacterRepository.kt
│   │   │   │   │   ├── PostRepository.kt
│   │   │   │   │   ├── FavoriteRepository.kt
│   │   │   │   │   └── PhotoRepository.kt
│   │   │   │   └── usecase/
│   │   │   │       ├── GetCharactersUseCase.kt
│   │   │   │       ├── GetCharacterDetailUseCase.kt
│   │   │   │       ├── ToggleFavoriteUseCase.kt
│   │   │   │       ├── GetFavoritesUseCase.kt
│   │   │   │       ├── GetPostsUseCase.kt
│   │   │   │       ├── CreatePostUseCase.kt
│   │   │   │       ├── UpdatePostUseCase.kt
│   │   │   │       ├── DeletePostUseCase.kt
│   │   │   │       ├── CapturePhotoUseCase.kt
│   │   │   │       └── GetCurrentLocationUseCase.kt
│   │   │   ├── data/
│   │   │   │   ├── remote/
│   │   │   │   │   ├── RickAndMortyClient.kt
│   │   │   │   │   ├── GraphQLZeroClient.kt
│   │   │   │   │   └── mapper/
│   │   │   │   │       ├── CharacterMapper.kt
│   │   │   │   │       └── PostMapper.kt
│   │   │   │   ├── local/
│   │   │   │   │   └── DatabaseDriverFactory.kt
│   │   │   │   └── repository/
│   │   │   │       ├── CharacterRepositoryImpl.kt
│   │   │   │       ├── PostRepositoryImpl.kt
│   │   │   │       ├── FavoriteRepositoryImpl.kt
│   │   │   │       └── PhotoRepositoryImpl.kt
│   │   │   ├── platform/
│   │   │   │   ├── PlatformCamera.kt         (expect)
│   │   │   │   └── PlatformLocationProvider.kt (expect)
│   │   │   └── di/
│   │   │       └── SharedModule.kt
│   │   ├── graphql/
│   │   │   ├── rickandmorty/
│   │   │   │   ├── schema.graphqls
│   │   │   │   ├── GetCharactersQuery.graphql
│   │   │   │   └── GetCharacterDetailQuery.graphql
│   │   │   └── graphqlzero/
│   │   │       ├── schema.graphqls
│   │   │       ├── GetPostsQuery.graphql
│   │   │       ├── GetPostQuery.graphql
│   │   │       ├── CreatePostMutation.graphql
│   │   │       ├── UpdatePostMutation.graphql
│   │   │       └── DeletePostMutation.graphql
│   │   └── sqldelight/
│   │       └── com/example/kmp_basic_app/
│   │           ├── Favorite.sq
│   │           └── CapturedPhotoEntity.sq
│   ├── androidMain/
│   │   └── kotlin/com/example/kmp_basic_app/
│   │       ├── data/local/DatabaseDriverFactory.android.kt
│   │       └── platform/
│   │           ├── PlatformCamera.android.kt
│   │           └── PlatformLocationProvider.android.kt
│   ├── iosMain/
│   │   └── kotlin/com/example/kmp_basic_app/
│   │       ├── data/local/DatabaseDriverFactory.ios.kt
│   │       └── platform/
│   │           ├── PlatformCamera.ios.kt
│   │           └── PlatformLocationProvider.ios.kt
│   └── commonTest/
│       └── kotlin/com/example/kmp_basic_app/
│           ├── domain/usecase/
│           │   ├── GetCharactersUseCaseTest.kt
│           │   ├── GetPostsUseCaseTest.kt
│           │   └── ToggleFavoriteUseCaseTest.kt
│           └── data/repository/
│               └── FakeRepositories.kt
```

### composeApp/ module (modified — Android only)

```
composeApp/
├── build.gradle.kts                          (modified)
├── src/androidMain/
│   ├── kotlin/com/example/kmp_basic_app/
│   │   ├── App.kt                            (rewritten)
│   │   ├── MainActivity.kt                   (modified)
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   └── AppTheme.kt
│   │   │   ├── navigation/
│   │   │   │   └── AppNavigation.kt
│   │   │   ├── components/
│   │   │   │   ├── CharacterCard.kt
│   │   │   │   ├── PostCard.kt
│   │   │   │   ├── LoadingIndicator.kt
│   │   │   │   └── ErrorView.kt
│   │   │   └── screens/
│   │   │       ├── CharactersScreen.kt
│   │   │       ├── CharacterDetailScreen.kt
│   │   │       ├── PostsScreen.kt
│   │   │       ├── CreateEditPostScreen.kt
│   │   │       ├── CameraScreen.kt
│   │   │       ├── LocationScreen.kt
│   │   │       └── FavoritesScreen.kt
│   │   ├── viewmodel/
│   │   │   ├── CharactersViewModel.kt
│   │   │   ├── CharacterDetailViewModel.kt
│   │   │   ├── PostsViewModel.kt
│   │   │   ├── CameraViewModel.kt
│   │   │   ├── LocationViewModel.kt
│   │   │   └── FavoritesViewModel.kt
│   │   └── di/
│   │       └── AndroidModule.kt
│   └── AndroidManifest.xml                   (modified)
```

### iosApp/ (native SwiftUI — rewritten)

```
iosApp/
├── iosApp.swift                              (rewritten)
├── Sources/
│   ├── DI/
│   │   └── KoinHelper.swift
│   ├── Navigation/
│   │   └── MainTabView.swift
│   ├── ViewModels/
│   │   ├── CharactersViewModel.swift
│   │   ├── CharacterDetailViewModel.swift
│   │   ├── PostsViewModel.swift
│   │   ├── CameraViewModel.swift
│   │   ├── LocationViewModel.swift
│   │   └── FavoritesViewModel.swift
│   ├── Screens/
│   │   ├── CharactersScreen.swift
│   │   ├── CharacterDetailScreen.swift
│   │   ├── PostsScreen.swift
│   │   ├── CreateEditPostSheet.swift
│   │   ├── CameraScreen.swift
│   │   ├── LocationScreen.swift
│   │   └── FavoritesScreen.swift
│   └── Components/
│       ├── CharacterRow.swift
│       ├── PostRow.swift
│       ├── LoadingView.swift
│       └── ErrorView.swift
├── Info.plist                                (modified)
└── Configuration/Config.xcconfig
```

### Root files (modified)

```
settings.gradle.kts                           (add :shared)
build.gradle.kts                              (add apollo, sqldelight, serialization plugins)
gradle/libs.versions.toml                     (add all new dependencies)
```

---

## Task 1: Add Dependencies to Version Catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add new versions to libs.versions.toml**

Add these entries to the `[versions]` section:

```toml
apollo = "4.4.3"
sqldelight = "2.0.2"
koin = "4.0.0"
kotlinx-serialization = "1.7.3"
coil3 = "3.1.0"
skie = "0.10.0"
camerax = "1.4.1"
playServicesLocation = "21.3.0"
kotlinx-coroutines = "1.10.2"
```

Add these entries to the `[libraries]` section:

```toml
# Apollo GraphQL
apollo-runtime = { module = "com.apollographql.apollo:apollo-runtime" }

# SQLDelight
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

# Kotlinx Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Kotlinx Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

# Coil (Image loading)
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }

# CameraX
camerax-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
camerax-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
camerax-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }

# Play Services Location
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "playServicesLocation" }
```

Add these entries to the `[plugins]` section:

```toml
apolloGraphQL = { id = "com.apollographql.apollo", version.ref = "apollo" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Verify the file is valid TOML**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && cat gradle/libs.versions.toml`

Check no syntax errors.

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore: add Apollo, SQLDelight, Koin, Coil, CameraX, and GPS dependencies to version catalog"
```

---

## Task 2: Create shared Module and Update Root Build Files

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/.gitkeep`
- Create: `shared/src/androidMain/kotlin/com/example/kmp_basic_app/.gitkeep`
- Create: `shared/src/iosMain/kotlin/com/example/kmp_basic_app/.gitkeep`
- Create: `shared/src/commonTest/kotlin/com/example/kmp_basic_app/.gitkeep`

- [ ] **Step 1: Update settings.gradle.kts to include :shared**

Replace the entire file:

```kotlin
rootProject.name = "KMP_Basic_App"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":composeApp")
```

- [ ] **Step 2: Update root build.gradle.kts to declare new plugins**

Replace the entire file:

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.apolloGraphQL) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}
```

- [ ] **Step 3: Create shared/build.gradle.kts**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.apolloGraphQL)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.apollo.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.play.services.location)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.example.kmp_basic_app.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

apollo {
    service("rickandmorty") {
        packageName.set("com.example.kmp_basic_app.graphql.rickandmorty")
        srcDir("src/commonMain/graphql/rickandmorty")
        introspection {
            endpointUrl.set("https://rickandmortyapi.com/graphql")
            schemaFile.set(file("src/commonMain/graphql/rickandmorty/schema.graphqls"))
        }
    }
    service("graphqlzero") {
        packageName.set("com.example.kmp_basic_app.graphql.graphqlzero")
        srcDir("src/commonMain/graphql/graphqlzero")
        introspection {
            endpointUrl.set("https://graphqlzero.almansi.me/api")
            schemaFile.set(file("src/commonMain/graphql/graphqlzero/schema.graphqls"))
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.example.kmp_basic_app.db")
        }
    }
}
```

- [ ] **Step 4: Create source directory structure**

Run:
```bash
mkdir -p shared/src/commonMain/kotlin/com/example/kmp_basic_app
mkdir -p shared/src/commonMain/graphql/rickandmorty
mkdir -p shared/src/commonMain/graphql/graphqlzero
mkdir -p shared/src/commonMain/sqldelight/com/example/kmp_basic_app
mkdir -p shared/src/androidMain/kotlin/com/example/kmp_basic_app
mkdir -p shared/src/iosMain/kotlin/com/example/kmp_basic_app
mkdir -p shared/src/commonTest/kotlin/com/example/kmp_basic_app
```

- [ ] **Step 5: Update composeApp/build.gradle.kts — remove iOS/Desktop targets, add shared dependency**

Replace the entire file:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.example.kmp_basic_app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.kmp_basic_app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
```

- [ ] **Step 6: Verify Gradle sync succeeds**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:help --no-daemon 2>&1 | tail -5`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts shared/build.gradle.kts composeApp/build.gradle.kts
git commit -m "feat: create shared KMP library module, restructure composeApp for Android-only"
```

---

## Task 3: Download GraphQL Schemas

**Files:**
- Create: `shared/src/commonMain/graphql/rickandmorty/schema.graphqls` (auto-generated)
- Create: `shared/src/commonMain/graphql/graphqlzero/schema.graphqls` (auto-generated)

- [ ] **Step 1: Download Rick and Morty schema**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:downloadRickandmortyApolloSchemaFromIntrospection --no-daemon
```

Expected: Schema file created at `shared/src/commonMain/graphql/rickandmorty/schema.graphqls`

- [ ] **Step 2: Download GraphQLZero schema**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:downloadGraphqlzeroApolloSchemaFromIntrospection --no-daemon
```

Expected: Schema file created at `shared/src/commonMain/graphql/graphqlzero/schema.graphqls`

- [ ] **Step 3: Verify both schemas exist**

Run: `ls -la shared/src/commonMain/graphql/*/schema.graphqls`

Expected: Both files present with non-zero size.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/graphql/
git commit -m "chore: download GraphQL schemas for Rick and Morty and GraphQLZero APIs"
```

---

## Task 4: Write GraphQL Operations

**Files:**
- Create: `shared/src/commonMain/graphql/rickandmorty/GetCharactersQuery.graphql`
- Create: `shared/src/commonMain/graphql/rickandmorty/GetCharacterDetailQuery.graphql`
- Create: `shared/src/commonMain/graphql/graphqlzero/GetPostsQuery.graphql`
- Create: `shared/src/commonMain/graphql/graphqlzero/GetPostQuery.graphql`
- Create: `shared/src/commonMain/graphql/graphqlzero/CreatePostMutation.graphql`
- Create: `shared/src/commonMain/graphql/graphqlzero/UpdatePostMutation.graphql`
- Create: `shared/src/commonMain/graphql/graphqlzero/DeletePostMutation.graphql`

- [ ] **Step 1: Create Rick and Morty queries**

`shared/src/commonMain/graphql/rickandmorty/GetCharactersQuery.graphql`:
```graphql
query GetCharacters($page: Int, $nameFilter: String) {
  characters(page: $page, filter: { name: $nameFilter }) {
    info {
      count
      pages
      next
    }
    results {
      id
      name
      status
      species
      gender
      image
      origin {
        id
        name
      }
      location {
        id
        name
      }
      episode {
        id
      }
    }
  }
}
```

`shared/src/commonMain/graphql/rickandmorty/GetCharacterDetailQuery.graphql`:
```graphql
query GetCharacterDetail($id: ID!) {
  character(id: $id) {
    id
    name
    status
    species
    gender
    image
    origin {
      id
      name
      type
      dimension
    }
    location {
      id
      name
      type
      dimension
    }
    episode {
      id
      name
      air_date
      episode
    }
  }
}
```

- [ ] **Step 2: Create GraphQLZero operations**

`shared/src/commonMain/graphql/graphqlzero/GetPostsQuery.graphql`:
```graphql
query GetPosts($page: Int, $limit: Int) {
  posts(options: { paginate: { page: $page, limit: $limit } }) {
    data {
      id
      title
      body
    }
    meta {
      totalCount
    }
  }
}
```

`shared/src/commonMain/graphql/graphqlzero/GetPostQuery.graphql`:
```graphql
query GetPost($id: ID!) {
  post(id: $id) {
    id
    title
    body
  }
}
```

`shared/src/commonMain/graphql/graphqlzero/CreatePostMutation.graphql`:
```graphql
mutation CreatePost($title: String!, $body: String!) {
  createPost(input: { title: $title, body: $body }) {
    id
    title
    body
  }
}
```

`shared/src/commonMain/graphql/graphqlzero/UpdatePostMutation.graphql`:
```graphql
mutation UpdatePost($id: ID!, $title: String!, $body: String!) {
  updatePost(id: $id, input: { title: $title, body: $body }) {
    id
    title
    body
  }
}
```

`shared/src/commonMain/graphql/graphqlzero/DeletePostMutation.graphql`:
```graphql
mutation DeletePost($id: ID!) {
  deletePost(id: $id)
}
```

- [ ] **Step 3: Verify Apollo code generation works**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:generateApolloSources --no-daemon 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` — generated Kotlin classes for all operations.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/graphql/
git commit -m "feat: add GraphQL operations for Rick and Morty and GraphQLZero APIs"
```

---

## Task 5: Create SQLDelight Schema Files

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/example/kmp_basic_app/Favorite.sq`
- Create: `shared/src/commonMain/sqldelight/com/example/kmp_basic_app/CapturedPhotoEntity.sq`

- [ ] **Step 1: Create Favorite.sq**

`shared/src/commonMain/sqldelight/com/example/kmp_basic_app/Favorite.sq`:
```sql
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

- [ ] **Step 2: Create CapturedPhotoEntity.sq**

`shared/src/commonMain/sqldelight/com/example/kmp_basic_app/CapturedPhotoEntity.sq`:
```sql
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

- [ ] **Step 3: Verify SQLDelight code generation**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:generateCommonMainAppDatabaseInterface --no-daemon 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/sqldelight/
git commit -m "feat: add SQLDelight schema for favorites and captured photos"
```

---

## Task 6: Create Domain Models

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Character.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Post.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Platform.kt`

- [ ] **Step 1: Create Character.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Character.kt`:
```kotlin
package com.example.kmp_basic_app.domain.model

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

enum class CharacterStatus {
    ALIVE, DEAD, UNKNOWN;

    companion object {
        fun fromString(value: String): CharacterStatus = when (value.lowercase()) {
            "alive" -> ALIVE
            "dead" -> DEAD
            else -> UNKNOWN
        }
    }
}

data class LocationBrief(
    val id: String?,
    val name: String
)

data class LocationDetail(
    val id: String?,
    val name: String,
    val type: String,
    val dimension: String
)

data class Episode(
    val id: String,
    val name: String,
    val airDate: String,
    val episode: String
)

data class CharacterDetail(
    val character: Character,
    val origin: LocationDetail,
    val location: LocationDetail,
    val episodes: List<Episode>
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

- [ ] **Step 2: Create Post.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Post.kt`:
```kotlin
package com.example.kmp_basic_app.domain.model

data class Post(
    val id: String,
    val title: String,
    val body: String
)

data class PostPage(
    val posts: List<Post>,
    val totalCount: Int
)
```

- [ ] **Step 3: Create Platform.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/Platform.kt`:
```kotlin
package com.example.kmp_basic_app.domain.model

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)

data class CapturedPhoto(
    val id: Long = 0,
    val filePath: String,
    val timestamp: Long,
    val location: GpsLocation? = null
)
```

- [ ] **Step 4: Verify compilation**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:compileKotlinMetadata --no-daemon 2>&1 | tail -5`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/model/
git commit -m "feat: add domain models for Character, Post, GpsLocation, and CapturedPhoto"
```

---

## Task 7: Create Repository Interfaces

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/CharacterRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/PostRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/FavoriteRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/PhotoRepository.kt`

- [ ] **Step 1: Create CharacterRepository.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/CharacterRepository.kt`:
```kotlin
package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.model.CharacterPage

interface CharacterRepository {
    suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage>
    suspend fun getCharacterDetail(id: String): Result<CharacterDetail>
}
```

- [ ] **Step 2: Create PostRepository.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/PostRepository.kt`:
```kotlin
package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage

interface PostRepository {
    suspend fun getPosts(page: Int, limit: Int): Result<PostPage>
    suspend fun createPost(title: String, body: String): Result<Post>
    suspend fun updatePost(id: String, title: String, body: String): Result<Post>
    suspend fun deletePost(id: String): Result<Boolean>
}
```

- [ ] **Step 3: Create FavoriteRepository.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/FavoriteRepository.kt`:
```kotlin
package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<Character>>
    suspend fun toggleFavorite(character: Character): Boolean
    suspend fun isFavorite(characterId: String): Boolean
}
```

- [ ] **Step 4: Create PhotoRepository.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/PhotoRepository.kt`:
```kotlin
package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotos(): Flow<List<CapturedPhoto>>
    suspend fun savePhoto(photo: CapturedPhoto)
    suspend fun deletePhoto(id: Long)
}
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/repository/
git commit -m "feat: add repository interfaces for Character, Post, Favorite, and Photo"
```

---

## Task 8: Create UseCases

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/GetCharactersUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/GetCharacterDetailUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/ToggleFavoriteUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/GetFavoritesUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/GetPostsUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/CreatePostUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/UpdatePostUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/DeletePostUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/CapturePhotoUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/GetCurrentLocationUseCase.kt`

- [ ] **Step 1: Create GetCharactersUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class GetCharactersUseCase(
    private val characterRepository: CharacterRepository,
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(page: Int, nameFilter: String? = null): Result<CharacterPage> {
        return characterRepository.getCharacters(page, nameFilter).map { characterPage ->
            val results = characterPage.results.map { character ->
                character.copy(isFavorite = favoriteRepository.isFavorite(character.id))
            }
            characterPage.copy(results = results)
        }
    }
}
```

- [ ] **Step 2: Create GetCharacterDetailUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class GetCharacterDetailUseCase(
    private val characterRepository: CharacterRepository,
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(id: String): Result<CharacterDetail> {
        return characterRepository.getCharacterDetail(id).map { detail ->
            val isFav = favoriteRepository.isFavorite(detail.character.id)
            detail.copy(character = detail.character.copy(isFavorite = isFav))
        }
    }
}
```

- [ ] **Step 3: Create ToggleFavoriteUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class ToggleFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(character: Character): Boolean {
        return favoriteRepository.toggleFavorite(character)
    }
}
```

- [ ] **Step 4: Create GetFavoritesUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Character>> {
        return favoriteRepository.observeFavorites()
    }
}
```

- [ ] **Step 5: Create GetPostsUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.domain.repository.PostRepository

class GetPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 10): Result<PostPage> {
        return postRepository.getPosts(page, limit)
    }
}
```

- [ ] **Step 6: Create CreatePostUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.repository.PostRepository

class CreatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(title: String, body: String): Result<Post> {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
        return postRepository.createPost(title, body)
    }
}
```

- [ ] **Step 7: Create UpdatePostUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.repository.PostRepository

class UpdatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(id: String, title: String, body: String): Result<Post> {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
        return postRepository.updatePost(id, title, body)
    }
}
```

- [ ] **Step 8: Create DeletePostUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.repository.PostRepository

class DeletePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(id: String): Result<Boolean> {
        return postRepository.deletePost(id)
    }
}
```

- [ ] **Step 9: Create CapturePhotoUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.platform.PlatformCamera
import com.example.kmp_basic_app.platform.PlatformLocationProvider

class CapturePhotoUseCase(
    private val camera: PlatformCamera,
    private val locationProvider: PlatformLocationProvider,
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(): Result<CapturedPhoto> {
        return try {
            val photo = camera.capturePhoto()
            val location = try {
                if (locationProvider.isPermissionGranted()) {
                    locationProvider.getCurrentLocation()
                } else null
            } catch (_: Exception) { null }
            val photoWithLocation = photo.copy(location = location)
            photoRepository.savePhoto(photoWithLocation)
            Result.success(photoWithLocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 10: Create GetCurrentLocationUseCase.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.platform.PlatformLocationProvider

class GetCurrentLocationUseCase(
    private val locationProvider: PlatformLocationProvider
) {
    suspend operator fun invoke(): Result<GpsLocation> {
        return try {
            if (!locationProvider.isPermissionGranted()) {
                locationProvider.requestPermission()
            }
            if (!locationProvider.isPermissionGranted()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            Result.success(locationProvider.getCurrentLocation())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isPermissionGranted(): Boolean = locationProvider.isPermissionGranted()
}
```

- [ ] **Step 11: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/domain/usecase/
git commit -m "feat: add all use cases for characters, posts, camera, and location"
```

---

## Task 9: Create Platform expect Declarations

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/platform/PlatformCamera.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/platform/PlatformLocationProvider.kt`

- [ ] **Step 1: Create PlatformCamera.kt expect**

```kotlin
package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.CapturedPhoto

expect class PlatformCamera {
    suspend fun capturePhoto(): CapturedPhoto
    fun isAvailable(): Boolean
}
```

- [ ] **Step 2: Create PlatformLocationProvider.kt expect**

```kotlin
package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.GpsLocation

expect class PlatformLocationProvider {
    suspend fun getCurrentLocation(): GpsLocation
    suspend fun requestPermission(): Boolean
    fun isPermissionGranted(): Boolean
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/platform/
git commit -m "feat: add expect declarations for PlatformCamera and PlatformLocationProvider"
```

---

## Task 10: Create SQLDelight DatabaseDriverFactory and Data Layer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.kt`
- Create: `shared/src/androidMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.android.kt`
- Create: `shared/src/iosMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.ios.kt`

- [ ] **Step 1: Create expect DatabaseDriverFactory**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.kt`:
```kotlin
package com.example.kmp_basic_app.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
```

- [ ] **Step 2: Create Android actual DatabaseDriverFactory**

`shared/src/androidMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.android.kt`:
```kotlin
package com.example.kmp_basic_app.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.kmp_basic_app.db.AppDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "kmp_app.db")
    }
}
```

- [ ] **Step 3: Create iOS actual DatabaseDriverFactory**

`shared/src/iosMain/kotlin/com/example/kmp_basic_app/data/local/DatabaseDriverFactory.ios.kt`:
```kotlin
package com.example.kmp_basic_app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.kmp_basic_app.db.AppDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "kmp_app.db")
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/local/ shared/src/androidMain/kotlin/com/example/kmp_basic_app/data/local/ shared/src/iosMain/kotlin/com/example/kmp_basic_app/data/local/
git commit -m "feat: add SQLDelight DatabaseDriverFactory with Android and iOS actual implementations"
```

---

## Task 11: Create GraphQL Mappers and Apollo Clients

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/RickAndMortyClient.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/GraphQLZeroClient.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/mapper/CharacterMapper.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/mapper/PostMapper.kt`

- [ ] **Step 1: Create CharacterMapper.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/mapper/CharacterMapper.kt`:
```kotlin
package com.example.kmp_basic_app.data.remote.mapper

import com.example.kmp_basic_app.domain.model.*
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharacterDetailQuery
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharactersQuery

fun GetCharactersQuery.Data.toDomain(): CharacterPage {
    val chars = characters?.results?.filterNotNull()?.map { result ->
        Character(
            id = result.id ?: "",
            name = result.name ?: "",
            status = CharacterStatus.fromString(result.status ?: ""),
            species = result.species ?: "",
            gender = result.gender ?: "",
            origin = LocationBrief(
                id = result.origin?.id,
                name = result.origin?.name ?: "Unknown"
            ),
            location = LocationBrief(
                id = result.location?.id,
                name = result.location?.name ?: "Unknown"
            ),
            imageUrl = result.image ?: "",
            episodeIds = result.episode?.filterNotNull()?.mapNotNull { it.id } ?: emptyList()
        )
    } ?: emptyList()

    return CharacterPage(
        info = PageInfo(
            count = characters?.info?.count ?: 0,
            pages = characters?.info?.pages ?: 0,
            next = characters?.info?.next
        ),
        results = chars
    )
}

fun GetCharacterDetailQuery.Data.toDomain(): CharacterDetail {
    val c = character ?: throw IllegalStateException("Character not found")
    return CharacterDetail(
        character = Character(
            id = c.id ?: "",
            name = c.name ?: "",
            status = CharacterStatus.fromString(c.status ?: ""),
            species = c.species ?: "",
            gender = c.gender ?: "",
            origin = LocationBrief(id = c.origin?.id, name = c.origin?.name ?: "Unknown"),
            location = LocationBrief(id = c.location?.id, name = c.location?.name ?: "Unknown"),
            imageUrl = c.image ?: "",
            episodeIds = c.episode?.filterNotNull()?.mapNotNull { it.id } ?: emptyList()
        ),
        origin = LocationDetail(
            id = c.origin?.id,
            name = c.origin?.name ?: "Unknown",
            type = c.origin?.type ?: "",
            dimension = c.origin?.dimension ?: ""
        ),
        location = LocationDetail(
            id = c.location?.id,
            name = c.location?.name ?: "Unknown",
            type = c.location?.type ?: "",
            dimension = c.location?.dimension ?: ""
        ),
        episodes = c.episode?.filterNotNull()?.map { ep ->
            Episode(
                id = ep.id ?: "",
                name = ep.name ?: "",
                airDate = ep.air_date ?: "",
                episode = ep.episode ?: ""
            )
        } ?: emptyList()
    )
}
```

- [ ] **Step 2: Create PostMapper.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/mapper/PostMapper.kt`:
```kotlin
package com.example.kmp_basic_app.data.remote.mapper

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.graphql.graphqlzero.CreatePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.GetPostsQuery
import com.example.kmp_basic_app.graphql.graphqlzero.UpdatePostMutation

fun GetPostsQuery.Data.toDomain(): PostPage {
    return PostPage(
        posts = posts?.data?.filterNotNull()?.map { post ->
            Post(
                id = post.id ?: "",
                title = post.title ?: "",
                body = post.body ?: ""
            )
        } ?: emptyList(),
        totalCount = posts?.meta?.totalCount ?: 0
    )
}

fun CreatePostMutation.Data.toDomain(): Post {
    return Post(
        id = createPost?.id ?: "",
        title = createPost?.title ?: "",
        body = createPost?.body ?: ""
    )
}

fun UpdatePostMutation.Data.toDomain(): Post {
    return Post(
        id = updatePost?.id ?: "",
        title = updatePost?.title ?: "",
        body = updatePost?.body ?: ""
    )
}
```

- [ ] **Step 3: Create RickAndMortyClient.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/RickAndMortyClient.kt`:
```kotlin
package com.example.kmp_basic_app.data.remote

import com.apollographql.apollo.ApolloClient

fun createRickAndMortyApolloClient(): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("https://rickandmortyapi.com/graphql")
        .build()
}
```

- [ ] **Step 4: Create GraphQLZeroClient.kt**

`shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/GraphQLZeroClient.kt`:
```kotlin
package com.example.kmp_basic_app.data.remote

import com.apollographql.apollo.ApolloClient

fun createGraphQLZeroApolloClient(): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("https://graphqlzero.almansi.me/api")
        .build()
}
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/remote/
git commit -m "feat: add Apollo clients and GraphQL response mappers for both APIs"
```

---

## Task 12: Create Repository Implementations

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/repository/CharacterRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/repository/PostRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/repository/FavoriteRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/repository/PhotoRepositoryImpl.kt`

- [ ] **Step 1: Create CharacterRepositoryImpl.kt**

```kotlin
package com.example.kmp_basic_app.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.kmp_basic_app.data.remote.mapper.toDomain
import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharacterDetailQuery
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharactersQuery

class CharacterRepositoryImpl(
    private val apolloClient: ApolloClient
) : CharacterRepository {

    override suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage> {
        return try {
            val response = apolloClient.query(
                GetCharactersQuery(
                    page = Optional.present(page),
                    nameFilter = if (nameFilter.isNullOrBlank()) Optional.absent() else Optional.present(nameFilter)
                )
            ).execute()

            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(response.dataOrThrow().toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCharacterDetail(id: String): Result<CharacterDetail> {
        return try {
            val response = apolloClient.query(GetCharacterDetailQuery(id)).execute()

            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(response.dataOrThrow().toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 2: Create PostRepositoryImpl.kt**

```kotlin
package com.example.kmp_basic_app.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.kmp_basic_app.data.remote.mapper.toDomain
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.domain.repository.PostRepository
import com.example.kmp_basic_app.graphql.graphqlzero.CreatePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.DeletePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.GetPostsQuery
import com.example.kmp_basic_app.graphql.graphqlzero.UpdatePostMutation

class PostRepositoryImpl(
    private val apolloClient: ApolloClient
) : PostRepository {

    override suspend fun getPosts(page: Int, limit: Int): Result<PostPage> {
        return try {
            val response = apolloClient.query(
                GetPostsQuery(
                    page = Optional.present(page),
                    limit = Optional.present(limit)
                )
            ).execute()

            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(response.dataOrThrow().toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(title: String, body: String): Result<Post> {
        return try {
            val response = apolloClient.mutation(CreatePostMutation(title, body)).execute()
            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(response.dataOrThrow().toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(id: String, title: String, body: String): Result<Post> {
        return try {
            val response = apolloClient.mutation(UpdatePostMutation(id, title, body)).execute()
            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(response.dataOrThrow().toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(id: String): Result<Boolean> {
        return try {
            val response = apolloClient.mutation(DeletePostMutation(id)).execute()
            when {
                response.exception != null -> Result.failure(
                    Exception("Network error: ${response.exception?.message}")
                )
                response.hasErrors() -> Result.failure(
                    Exception("GraphQL error: ${response.errors?.firstOrNull()?.message}")
                )
                else -> Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 3: Create FavoriteRepositoryImpl.kt**

```kotlin
package com.example.kmp_basic_app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.model.CharacterStatus
import com.example.kmp_basic_app.domain.model.LocationBrief
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(
    private val database: AppDatabase
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<Character>> {
        return database.favoriteQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { favorites ->
                favorites.map { fav ->
                    Character(
                        id = fav.character_id,
                        name = fav.name,
                        status = CharacterStatus.fromString(fav.status),
                        species = fav.species,
                        gender = "",
                        origin = LocationBrief(null, ""),
                        location = LocationBrief(null, ""),
                        imageUrl = fav.image_url,
                        episodeIds = emptyList(),
                        isFavorite = true
                    )
                }
            }
    }

    override suspend fun toggleFavorite(character: Character): Boolean {
        val isFav = isFavorite(character.id)
        if (isFav) {
            database.favoriteQueries.deleteById(character.id)
        } else {
            database.favoriteQueries.insert(
                character_id = character.id,
                name = character.name,
                status = character.status.name,
                species = character.species,
                image_url = character.imageUrl,
                added_at = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
        }
        return !isFav
    }

    override suspend fun isFavorite(characterId: String): Boolean {
        return database.favoriteQueries.isFavorite(characterId).executeAsOne() > 0
    }
}
```

**Note:** We need to add `kotlinx-datetime` to the version catalog. Add to `libs.versions.toml`:
- versions: `kotlinx-datetime = "0.6.1"`
- libraries: `kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }`

And add to `shared/build.gradle.kts` commonMain dependencies: `implementation(libs.kotlinx.datetime)`

- [ ] **Step 4: Create PhotoRepositoryImpl.kt**

```kotlin
package com.example.kmp_basic_app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PhotoRepositoryImpl(
    private val database: AppDatabase
) : PhotoRepository {

    override fun observePhotos(): Flow<List<CapturedPhoto>> {
        return database.capturedPhotoEntityQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { entity ->
                    CapturedPhoto(
                        id = entity.id,
                        filePath = entity.file_path,
                        timestamp = entity.timestamp,
                        location = if (entity.latitude != null && entity.longitude != null) {
                            GpsLocation(
                                latitude = entity.latitude,
                                longitude = entity.longitude,
                                accuracy = entity.accuracy?.toFloat()
                            )
                        } else null
                    )
                }
            }
    }

    override suspend fun savePhoto(photo: CapturedPhoto) {
        database.capturedPhotoEntityQueries.insert(
            file_path = photo.filePath,
            timestamp = photo.timestamp,
            latitude = photo.location?.latitude,
            longitude = photo.location?.longitude,
            accuracy = photo.location?.accuracy?.toDouble()
        )
    }

    override suspend fun deletePhoto(id: Long) {
        database.capturedPhotoEntityQueries.deleteById(id)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/data/repository/ gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "feat: add repository implementations for Character, Post, Favorite, and Photo"
```

---

## Task 13: Create Platform actual Implementations (Android)

**Files:**
- Create: `shared/src/androidMain/kotlin/com/example/kmp_basic_app/platform/PlatformCamera.android.kt`
- Create: `shared/src/androidMain/kotlin/com/example/kmp_basic_app/platform/PlatformLocationProvider.android.kt`

- [ ] **Step 1: Create Android PlatformCamera**

```kotlin
package com.example.kmp_basic_app.platform

import android.content.Context
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformCamera(private val context: Context) {

    actual suspend fun capturePhoto(): CapturedPhoto = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture
                )

                val photoFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "photo_${System.currentTimeMillis()}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            cameraProvider.unbindAll()
                            continuation.resume(
                                CapturedPhoto(
                                    filePath = photoFile.absolutePath,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            cameraProvider.unbindAll()
                            continuation.resumeWithException(exception)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    actual fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
}
```

- [ ] **Step 2: Create Android PlatformLocationProvider**

```kotlin
package com.example.kmp_basic_app.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformLocationProvider(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    actual suspend fun getCurrentLocation(): GpsLocation = suspendCoroutine { continuation ->
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMaxUpdates(1)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null) {
                        continuation.resume(
                            GpsLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy
                            )
                        )
                    } else {
                        continuation.resumeWithException(Exception("Could not get location"))
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }

    actual suspend fun requestPermission(): Boolean {
        // Permission requesting is handled at the UI layer (Activity)
        // This returns current state — the actual permission dialog is triggered from Compose
        return isPermissionGranted()
    }

    actual fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/androidMain/kotlin/com/example/kmp_basic_app/platform/
git commit -m "feat: add Android actual implementations for Camera (CameraX) and Location (FusedLocation)"
```

---

## Task 14: Create Platform actual Implementations (iOS)

**Files:**
- Create: `shared/src/iosMain/kotlin/com/example/kmp_basic_app/platform/PlatformCamera.ios.kt`
- Create: `shared/src/iosMain/kotlin/com/example/kmp_basic_app/platform/PlatformLocationProvider.ios.kt`

- [ ] **Step 1: Create iOS PlatformCamera**

```kotlin
package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo

actual class PlatformCamera {

    actual suspend fun capturePhoto(): CapturedPhoto {
        // iOS camera capture requires UIKit coordinator pattern.
        // The actual camera UI is presented from SwiftUI via a UIViewControllerRepresentable wrapper.
        // This stub returns a placeholder — the real capture flow is driven from the iOS UI layer.
        val filePath = NSTemporaryDirectory() + "photo_${NSUUID().UUIDString}.jpg"
        return CapturedPhoto(
            filePath = filePath,
            timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
        )
    }

    actual fun isAvailable(): Boolean {
        return AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null
    }
}
```

- [ ] **Step 2: Create iOS PlatformLocationProvider**

```kotlin
package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.GpsLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLLocation
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformLocationProvider {

    private val locationManager = CLLocationManager()

    actual suspend fun getCurrentLocation(): GpsLocation = suspendCoroutine { continuation ->
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                manager.stopUpdatingLocation()
                manager.delegate = null
                if (location != null) {
                    continuation.resume(
                        GpsLocation(
                            latitude = location.coordinate.latitude,
                            longitude = location.coordinate.longitude,
                            accuracy = location.horizontalAccuracy.toFloat()
                        )
                    )
                } else {
                    continuation.resumeWithException(Exception("Could not get location"))
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
                manager.stopUpdatingLocation()
                manager.delegate = null
                continuation.resumeWithException(Exception("Location error: ${didFailWithError.localizedDescription}"))
            }
        }

        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestLocation()
    }

    actual suspend fun requestPermission(): Boolean = suspendCoroutine { continuation ->
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                manager.delegate = null
                continuation.resume(isPermissionGranted())
            }
        }
        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()
    }

    actual fun isPermissionGranted(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/iosMain/kotlin/com/example/kmp_basic_app/platform/
git commit -m "feat: add iOS actual implementations for Camera (AVFoundation) and Location (CoreLocation)"
```

---

## Task 15: Create Koin DI Shared Module

**Files:**
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/di/SharedModule.kt`
- Create: `shared/src/commonMain/kotlin/com/example/kmp_basic_app/di/KoinInit.kt`

- [ ] **Step 1: Create SharedModule.kt**

```kotlin
package com.example.kmp_basic_app.di

import com.example.kmp_basic_app.data.local.DatabaseDriverFactory
import com.example.kmp_basic_app.data.remote.createGraphQLZeroApolloClient
import com.example.kmp_basic_app.data.remote.createRickAndMortyApolloClient
import com.example.kmp_basic_app.data.repository.CharacterRepositoryImpl
import com.example.kmp_basic_app.data.repository.FavoriteRepositoryImpl
import com.example.kmp_basic_app.data.repository.PhotoRepositoryImpl
import com.example.kmp_basic_app.data.repository.PostRepositoryImpl
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.domain.repository.PostRepository
import com.example.kmp_basic_app.domain.usecase.*
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val sharedModule = module {
    // Apollo clients
    single(named("rickandmorty")) { createRickAndMortyApolloClient() }
    single(named("graphqlzero")) { createGraphQLZeroApolloClient() }

    // Database
    single { get<DatabaseDriverFactory>().create() }
    single { AppDatabase(get()) }

    // Repositories
    single<CharacterRepository> { CharacterRepositoryImpl(get(named("rickandmorty"))) }
    single<PostRepository> { PostRepositoryImpl(get(named("graphqlzero"))) }
    single<FavoriteRepository> { FavoriteRepositoryImpl(get()) }
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }

    // UseCases
    factory { GetCharactersUseCase(get(), get()) }
    factory { GetCharacterDetailUseCase(get(), get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { GetFavoritesUseCase(get()) }
    factory { GetPostsUseCase(get()) }
    factory { CreatePostUseCase(get()) }
    factory { UpdatePostUseCase(get()) }
    factory { DeletePostUseCase(get()) }
    factory { CapturePhotoUseCase(get(), get(), get()) }
    factory { GetCurrentLocationUseCase(get()) }
}
```

- [ ] **Step 2: Create KoinInit.kt for iOS**

```kotlin
package com.example.kmp_basic_app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModule: Module): KoinApplication {
    return startKoin {
        modules(sharedModule, platformModule)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/example/kmp_basic_app/di/
git commit -m "feat: add Koin shared DI module with all repositories and use cases"
```

---

## Task 16: Verify Shared Module Compiles

- [ ] **Step 1: Run full shared module build**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:build --no-daemon 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Fix any compilation errors**

If there are errors, read the output, fix the affected files, and re-run until the build succeeds.

- [ ] **Step 3: Commit any fixes**

```bash
git add -A && git commit -m "fix: resolve shared module compilation issues"
```

---

## Task 17: Create Android Theme and Navigation

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/theme/AppTheme.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Create AppTheme.kt**

```kotlin
package com.example.kmp_basic_app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF21005D),
    secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260),
    onTertiary = androidx.compose.ui.graphics.Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF381E72),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF4F378B),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF332D41),
    tertiary = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF492532),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 2: Create AppNavigation.kt**

```kotlin
package com.example.kmp_basic_app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kmp_basic_app.ui.screens.*
import kotlinx.serialization.Serializable

@Serializable data object CharactersRoute
@Serializable data class CharacterDetailRoute(val id: String)
@Serializable data object PostsRoute
@Serializable data class CreateEditPostRoute(val postId: String? = null, val title: String? = null, val body: String? = null)
@Serializable data object CameraRoute
@Serializable data object LocationRoute
@Serializable data object FavoritesRoute

enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    val label: String
) {
    Characters(CharactersRoute, Icons.Default.People, "Characters"),
    Posts(PostsRoute, Icons.Default.Article, "Posts"),
    Camera(CameraRoute, Icons.Default.CameraAlt, "Camera"),
    Location(LocationRoute, Icons.Default.LocationOn, "GPS"),
    Favorites(FavoritesRoute, Icons.Default.Favorite, "Favorites"),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route?.contains(destination.route::class.qualifiedName ?: "") == true
                        } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CharactersRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<CharactersRoute> {
                CharactersScreen(
                    onCharacterClick = { id -> navController.navigate(CharacterDetailRoute(id)) }
                )
            }
            composable<CharacterDetailRoute> { backStackEntry ->
                CharacterDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<PostsRoute> {
                PostsScreen(
                    onCreatePost = { navController.navigate(CreateEditPostRoute()) },
                    onEditPost = { post ->
                        navController.navigate(CreateEditPostRoute(post.id, post.title, post.body))
                    }
                )
            }
            composable<CreateEditPostRoute> {
                CreateEditPostScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<CameraRoute> { CameraScreen() }
            composable<LocationRoute> { LocationScreen() }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onCharacterClick = { id -> navController.navigate(CharacterDetailRoute(id)) }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/theme/ composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/navigation/
git commit -m "feat: add Material3 theme and Navigation Compose setup with 5 tab destinations"
```

---

## Task 18: Create Android ViewModels

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/CharactersViewModel.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/CharacterDetailViewModel.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/PostsViewModel.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/CameraViewModel.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/LocationViewModel.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/FavoritesViewModel.kt`

- [ ] **Step 1: Create CharactersViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.usecase.GetCharactersUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharactersState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasNextPage: Boolean = true
)

class CharactersViewModel(
    private val getCharacters: GetCharactersUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CharactersState())
    val state: StateFlow<CharactersState> = _state.asStateFlow()

    init { loadCharacters() }

    fun onSearch(query: String) {
        _state.update { it.copy(searchQuery = query, currentPage = 1, characters = emptyList()) }
        loadCharacters()
    }

    fun loadNextPage() {
        if (_state.value.isLoading || !_state.value.hasNextPage) return
        _state.update { it.copy(currentPage = it.currentPage + 1) }
        loadCharacters(append = true)
    }

    fun refresh() {
        _state.update { it.copy(currentPage = 1, characters = emptyList()) }
        loadCharacters()
    }

    fun onToggleFavorite(character: Character) {
        viewModelScope.launch {
            val newState = toggleFavorite(character)
            _state.update { state ->
                state.copy(characters = state.characters.map {
                    if (it.id == character.id) it.copy(isFavorite = newState) else it
                })
            }
        }
    }

    private fun loadCharacters(append: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val filter = _state.value.searchQuery.takeIf { it.isNotBlank() }
            getCharacters(_state.value.currentPage, filter).fold(
                onSuccess = { page ->
                    _state.update { state ->
                        state.copy(
                            characters = if (append) state.characters + page.results else page.results,
                            isLoading = false,
                            hasNextPage = page.info.next != null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }
}
```

- [ ] **Step 2: Create CharacterDetailViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.usecase.GetCharacterDetailUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailState(
    val detail: CharacterDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CharacterDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getCharacterDetail: GetCharacterDetailUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val characterId: String = savedStateHandle["id"] ?: ""

    private val _state = MutableStateFlow(CharacterDetailState())
    val state: StateFlow<CharacterDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onToggleFavorite() {
        val character = _state.value.detail?.character ?: return
        viewModelScope.launch {
            val newState = toggleFavorite(character)
            _state.update { state ->
                state.copy(
                    detail = state.detail?.copy(
                        character = state.detail.character.copy(isFavorite = newState)
                    )
                )
            }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getCharacterDetail(characterId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(detail = detail, isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }
}
```

- [ ] **Step 3: Create PostsViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class PostsViewModel(
    private val getPosts: GetPostsUseCase,
    private val createPost: CreatePostUseCase,
    private val updatePost: UpdatePostUseCase,
    private val deletePost: DeletePostUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PostsState())
    val state: StateFlow<PostsState> = _state.asStateFlow()

    init { loadPosts() }

    fun loadPosts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getPosts().fold(
                onSuccess = { page ->
                    _state.update { it.copy(posts = page.posts, isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun onCreatePost(title: String, body: String) {
        viewModelScope.launch {
            createPost(title, body).fold(
                onSuccess = { post ->
                    _state.update { it.copy(
                        posts = listOf(post) + it.posts,
                        snackbarMessage = "Post created"
                    ) }
                },
                onFailure = { e ->
                    _state.update { it.copy(snackbarMessage = "Error: ${e.message}") }
                }
            )
        }
    }

    fun onUpdatePost(id: String, title: String, body: String) {
        viewModelScope.launch {
            updatePost(id, title, body).fold(
                onSuccess = { updated ->
                    _state.update { state ->
                        state.copy(
                            posts = state.posts.map { if (it.id == id) updated else it },
                            snackbarMessage = "Post updated"
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(snackbarMessage = "Error: ${e.message}") }
                }
            )
        }
    }

    fun onDeletePost(id: String) {
        viewModelScope.launch {
            deletePost(id).fold(
                onSuccess = {
                    _state.update { state ->
                        state.copy(
                            posts = state.posts.filter { it.id != id },
                            snackbarMessage = "Post deleted"
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(snackbarMessage = "Error: ${e.message}") }
                }
            )
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }
}
```

- [ ] **Step 4: Create CameraViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.domain.usecase.CapturePhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraState(
    val photos: List<CapturedPhoto> = emptyList(),
    val lastCaptured: CapturedPhoto? = null,
    val isCapturing: Boolean = false,
    val error: String? = null
)

class CameraViewModel(
    private val capturePhoto: CapturePhotoUseCase,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            photoRepository.observePhotos().collect { photos ->
                _state.update { it.copy(photos = photos) }
            }
        }
    }

    fun onCapturePhoto() {
        viewModelScope.launch {
            _state.update { it.copy(isCapturing = true, error = null) }
            capturePhoto().fold(
                onSuccess = { photo ->
                    _state.update { it.copy(lastCaptured = photo, isCapturing = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isCapturing = false) }
                }
            )
        }
    }
}
```

- [ ] **Step 5: Create LocationViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.domain.usecase.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationState(
    val location: GpsLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val permissionGranted: Boolean = false,
    val lastUpdated: Long? = null
)

class LocationViewModel(
    private val getCurrentLocation: GetCurrentLocationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    init {
        _state.update { it.copy(permissionGranted = getCurrentLocation.isPermissionGranted()) }
    }

    fun onRefreshLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getCurrentLocation().fold(
                onSuccess = { location ->
                    _state.update { it.copy(
                        location = location,
                        isLoading = false,
                        permissionGranted = true,
                        lastUpdated = System.currentTimeMillis()
                    ) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) onRefreshLocation()
    }
}
```

- [ ] **Step 6: Create FavoritesViewModel.kt**

```kotlin
package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.usecase.GetFavoritesUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesState(
    val favorites: List<Character> = emptyList()
)

class FavoritesViewModel(
    private val getFavorites: GetFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getFavorites().collect { favorites ->
                _state.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun onRemoveFavorite(character: Character) {
        viewModelScope.launch { toggleFavorite(character) }
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/viewmodel/
git commit -m "feat: add all Android ViewModels for characters, posts, camera, location, and favorites"
```

---

## Task 19: Create Android Reusable Components

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/components/CharacterCard.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/components/PostCard.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/components/LoadingIndicator.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/components/ErrorView.kt`

- [ ] **Step 1: Create CharacterCard.kt**

```kotlin
package com.example.kmp_basic_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.model.CharacterStatus

@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name,
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(character.status)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${character.status.name.lowercase().replaceFirstChar { it.uppercase() }} - ${character.species}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (character.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (character.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (character.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusDot(status: CharacterStatus) {
    val color = when (status) {
        CharacterStatus.ALIVE -> Color(0xFF55CC44)
        CharacterStatus.DEAD -> Color(0xFFD63D2E)
        CharacterStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }
    Surface(
        modifier = Modifier.size(8.dp),
        shape = CircleShape,
        color = color
    ) {}
}
```

- [ ] **Step 2: Create PostCard.kt**

```kotlin
package com.example.kmp_basic_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kmp_basic_app.domain.model.Post

@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

- [ ] **Step 3: Create LoadingIndicator.kt and ErrorView.kt**

`LoadingIndicator.kt`:
```kotlin
package com.example.kmp_basic_app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

`ErrorView.kt`:
```kotlin
package com.example.kmp_basic_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/components/
git commit -m "feat: add reusable Compose components — CharacterCard, PostCard, LoadingIndicator, ErrorView"
```

---

## Task 20: Create Android Screens

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/CharactersScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/CharacterDetailScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/PostsScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/CreateEditPostScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/CameraScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/LocationScreen.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/FavoritesScreen.kt`

This is the largest task — 7 screen files. Each screen follows the pattern: collect state from ViewModel, render stateless content composable.

- [ ] **Step 1: Create CharactersScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kmp_basic_app.ui.components.CharacterCard
import com.example.kmp_basic_app.ui.components.ErrorView
import com.example.kmp_basic_app.ui.components.LoadingIndicator
import com.example.kmp_basic_app.viewmodel.CharactersViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    onCharacterClick: (String) -> Unit,
    viewModel: CharactersViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Characters") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { viewModel.onSearch(it) },
                active = false,
                onActiveChange = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search characters...") }
            ) {}

            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading && state.characters.isEmpty() -> LoadingIndicator()
                state.error != null && state.characters.isEmpty() -> ErrorView(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.characters, key = { it.id }) { character ->
                            CharacterCard(
                                character = character,
                                onClick = { onCharacterClick(character.id) },
                                onFavoriteClick = { viewModel.onToggleFavorite(character) }
                            )
                        }
                        if (state.isLoading) {
                            item { LoadingIndicator(Modifier.height(48.dp)) }
                        }
                    }

                    // Trigger pagination
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItem >= state.characters.size - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) viewModel.loadNextPage()
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create CharacterDetailScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.kmp_basic_app.domain.model.CharacterStatus
import com.example.kmp_basic_app.ui.components.ErrorView
import com.example.kmp_basic_app.ui.components.LoadingIndicator
import com.example.kmp_basic_app.viewmodel.CharacterDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    onBack: () -> Unit,
    viewModel: CharacterDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.character?.name ?: "Character") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    state.detail?.character?.let { character ->
                        IconButton(onClick = { viewModel.onToggleFavorite() }) {
                            Icon(
                                if (character.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (character.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.padding(padding))
            state.error != null -> ErrorView(state.error ?: "", onRetry = {}, modifier = Modifier.padding(padding))
            state.detail != null -> {
                val detail = state.detail!!
                Column(
                    modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = detail.character.imageUrl,
                        contentDescription = detail.character.name,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusColor = when (detail.character.status) {
                                CharacterStatus.ALIVE -> Color(0xFF55CC44)
                                CharacterStatus.DEAD -> Color(0xFFD63D2E)
                                CharacterStatus.UNKNOWN -> Color(0xFF9E9E9E)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(detail.character.status.name) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.2f))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(detail.character.species, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Origin", style = MaterialTheme.typography.labelLarge)
                        Text(detail.origin.name, style = MaterialTheme.typography.bodyMedium)
                        if (detail.origin.type.isNotBlank()) {
                            Text("${detail.origin.type} - ${detail.origin.dimension}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Location", style = MaterialTheme.typography.labelLarge)
                        Text(detail.location.name, style = MaterialTheme.typography.bodyMedium)
                        if (detail.location.type.isNotBlank()) {
                            Text("${detail.location.type} - ${detail.location.dimension}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Episodes (${detail.episodes.size})", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detail.episodes) { episode ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(episode.episode) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create PostsScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.ui.components.ErrorView
import com.example.kmp_basic_app.ui.components.LoadingIndicator
import com.example.kmp_basic_app.ui.components.PostCard
import com.example.kmp_basic_app.viewmodel.PostsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    onCreatePost: () -> Unit,
    onEditPost: (Post) -> Unit,
    viewModel: PostsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Posts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePost) {
                Icon(Icons.Default.Add, "Create post")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading && state.posts.isEmpty() -> LoadingIndicator(Modifier.padding(padding))
            state.error != null && state.posts.isEmpty() -> ErrorView(
                state.error ?: "", onRetry = { viewModel.loadPosts() }, modifier = Modifier.padding(padding)
            )
            state.posts.isEmpty() -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No posts yet. Tap + to create one!", style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                val dismissState = remember { mutableStateOf<String?>(null) }
                var showDeleteDialog by remember { mutableStateOf<String?>(null) }

                if (showDeleteDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        title = { Text("Delete Post") },
                        text = { Text("Are you sure you want to delete this post?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.onDeletePost(showDeleteDialog!!)
                                showDeleteDialog = null
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.posts, key = { it.id }) { post ->
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        showDeleteDialog = post.id
                                    }
                                    false
                                }
                            ),
                            backgroundContent = {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.CenterEnd) {
                                        Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            PostCard(post = post, onClick = { onEditPost(post) })
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create CreateEditPostScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kmp_basic_app.viewmodel.PostsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditPostScreen(
    onBack: () -> Unit,
    postId: String? = null,
    initialTitle: String? = null,
    initialBody: String? = null,
    viewModel: PostsViewModel = koinViewModel()
) {
    val isEdit = postId != null
    var title by remember { mutableStateOf(initialTitle ?: "") }
    var body by remember { mutableStateOf(initialBody ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Post" else "Create Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Body") },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                maxLines = 10
            )
            Button(
                onClick = {
                    if (isEdit) {
                        viewModel.onUpdatePost(postId!!, title, body)
                    } else {
                        viewModel.onCreatePost(title, body)
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && body.isNotBlank()
            ) {
                Text(if (isEdit) "Update" else "Create")
            }
        }
    }
}
```

- [ ] **Step 5: Create CameraScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.kmp_basic_app.viewmodel.CameraViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Camera") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { viewModel.onCapturePhoto() },
                enabled = hasCameraPermission && !state.isCapturing
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isCapturing) "Capturing..." else "Capture Photo")
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            state.lastCaptured?.let { photo ->
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = photo.filePath,
                            contentDescription = "Last captured",
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        photo.location?.let { loc ->
                            Spacer(Modifier.height(8.dp))
                            Text("Location: ${loc.latitude}, ${loc.longitude}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Photo History", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.photos) { photo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = photo.filePath,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Photo #${photo.id}", style = MaterialTheme.typography.bodyMedium)
                                photo.location?.let { loc ->
                                    Text("${loc.latitude}, ${loc.longitude}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Create LocationScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kmp_basic_app.viewmodel.LocationViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: LocationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (!state.permissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.onRefreshLocation()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("GPS Location") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            if (!state.permissionGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Location permission required", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Grant location permission to see your coordinates.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }) { Text("Grant Permission") }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current Location", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        state.location?.let { loc ->
                            LocationRow("Latitude", "%.6f".format(loc.latitude))
                            LocationRow("Longitude", "%.6f".format(loc.longitude))
                            loc.accuracy?.let { LocationRow("Accuracy", "%.1f m".format(it)) }
                        } ?: Text("No location data yet", style = MaterialTheme.typography.bodyMedium)

                        state.lastUpdated?.let { timestamp ->
                            Spacer(Modifier.height(8.dp))
                            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            Text("Last updated: ${dateFormat.format(Date(timestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.onRefreshLocation() },
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isLoading) "Fetching..." else "Refresh Location")
                }

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LocationRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 7: Create FavoritesScreen.kt**

```kotlin
package com.example.kmp_basic_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kmp_basic_app.ui.components.CharacterCard
import com.example.kmp_basic_app.viewmodel.FavoritesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onCharacterClick: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorites") }) }
    ) { padding ->
        if (state.favorites.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HeartBroken, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap the heart icon on a character to add it here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.favorites, key = { it.id }) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onCharacterClick(character.id) },
                        onFavoriteClick = { viewModel.onRemoveFavorite(character) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/ui/screens/
git commit -m "feat: add all Android Compose screens — Characters, Posts, Camera, Location, Favorites"
```

---

## Task 21: Create Android DI Module, Update App Entry Point and Manifest

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/di/AndroidModule.kt`
- Create: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/KmpApp.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/App.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/example/kmp_basic_app/MainActivity.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

- [ ] **Step 1: Create AndroidModule.kt**

```kotlin
package com.example.kmp_basic_app.di

import com.example.kmp_basic_app.data.local.DatabaseDriverFactory
import com.example.kmp_basic_app.platform.PlatformCamera
import com.example.kmp_basic_app.platform.PlatformLocationProvider
import com.example.kmp_basic_app.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(get()) }
    single { PlatformCamera(get()) }
    single { PlatformLocationProvider(get()) }

    viewModel { CharactersViewModel(get(), get()) }
    viewModel { CharacterDetailViewModel(get(), get(), get()) }
    viewModel { PostsViewModel(get(), get(), get(), get()) }
    viewModel { CameraViewModel(get(), get()) }
    viewModel { LocationViewModel(get()) }
    viewModel { FavoritesViewModel(get(), get()) }
}
```

- [ ] **Step 2: Create KmpApp.kt (Application class)**

```kotlin
package com.example.kmp_basic_app

import android.app.Application
import com.example.kmp_basic_app.di.androidModule
import com.example.kmp_basic_app.di.initKoin
import org.koin.android.ext.koin.androidContext

class KmpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule).apply {
            androidContext(this@KmpApp)
        }
    }
}
```

- [ ] **Step 3: Rewrite App.kt**

```kotlin
package com.example.kmp_basic_app

import androidx.compose.runtime.Composable
import com.example.kmp_basic_app.ui.navigation.AppNavigation
import com.example.kmp_basic_app.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavigation()
    }
}
```

- [ ] **Step 4: Update MainActivity.kt**

```kotlin
package com.example.kmp_basic_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}
```

- [ ] **Step 5: Update AndroidManifest.xml with permissions and Application class**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application
        android:name=".KmpApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: Remove old unused files** (Platform.kt, Greeting.kt, Platform.android.kt, etc.)

Delete:
- `composeApp/src/commonMain/` (entire directory — no longer needed, shared code is in `shared/`)
- `composeApp/src/jvmMain/` (Desktop removed)
- `composeApp/src/iosMain/` (iOS is native SwiftUI now)
- `composeApp/src/commonTest/`

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/androidMain/ composeApp/build.gradle.kts
git commit -m "feat: add Android DI module, Application class, update entry points and manifest"
```

---

## Task 22: Build and Fix Android App

- [ ] **Step 1: Build the Android app**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :composeApp:assembleDebug --no-daemon 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` (or compilation errors to fix)

- [ ] **Step 2: Fix any compilation errors iteratively**

Read error output, fix affected files, re-run build until it succeeds.

- [ ] **Step 3: Commit fixes**

```bash
git add -A && git commit -m "fix: resolve Android compilation issues"
```

---

## Task 23: Create iOS SwiftUI — ViewModels

**Files:**
- Create: `iosApp/Sources/DI/KoinHelper.swift`
- Create: `iosApp/Sources/ViewModels/CharactersViewModel.swift`
- Create: `iosApp/Sources/ViewModels/CharacterDetailViewModel.swift`
- Create: `iosApp/Sources/ViewModels/PostsViewModel.swift`
- Create: `iosApp/Sources/ViewModels/CameraViewModel.swift`
- Create: `iosApp/Sources/ViewModels/LocationViewModel.swift`
- Create: `iosApp/Sources/ViewModels/FavoritesViewModel.swift`

- [ ] **Step 1: Create KoinHelper.swift**

```swift
import Shared

class KoinHelper {
    static let shared = KoinHelper()
    private var koin: Koin_coreKoin?

    func start() {
        let platformModule = Koin_coreModule()
        // Register iOS platform implementations
        // PlatformCamera and PlatformLocationProvider have no-arg constructors on iOS
        let app = KoinInitKt.doInitKoin(platformModule: platformModule)
        koin = app.koin
    }

    func resolve<T: AnyObject>(_ type: T.Type) -> T {
        guard let koin = koin else { fatalError("Koin not started") }
        guard let result = koin.get(objCClass: type) as? T else {
            fatalError("Could not resolve \(type)")
        }
        return result
    }
}
```

- [ ] **Step 2: Create CharactersViewModel.swift**

```swift
import SwiftUI
import Shared

@MainActor
class CharactersViewModel: ObservableObject {
    @Published var characters: [Character_] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var searchQuery = ""

    private let getCharacters: GetCharactersUseCase
    private let toggleFavorite: ToggleFavoriteUseCase
    private var currentPage: Int32 = 1
    private var hasNextPage = true

    init() {
        getCharacters = KoinHelper.shared.resolve(GetCharactersUseCase.self)
        toggleFavorite = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)
        Task { await loadCharacters() }
    }

    func onSearch(_ query: String) {
        searchQuery = query
        currentPage = 1
        characters = []
        Task { await loadCharacters() }
    }

    func loadNextPageIfNeeded(currentItem: Character_?) {
        guard let item = currentItem,
              let index = characters.firstIndex(where: { $0.id == item.id }),
              index >= characters.count - 3,
              !isLoading, hasNextPage else { return }
        currentPage += 1
        Task { await loadCharacters(append: true) }
    }

    func refresh() async {
        currentPage = 1
        characters = []
        await loadCharacters()
    }

    func onToggleFavorite(_ character: Character_) {
        Task {
            let newState = try await toggleFavorite.invoke(character: character)
            if let index = characters.firstIndex(where: { $0.id == character.id }) {
                characters[index] = character.doCopy(
                    id: character.id, name: character.name, status: character.status,
                    species: character.species, gender: character.gender,
                    origin: character.origin, location: character.location,
                    imageUrl: character.imageUrl, episodeIds: character.episodeIds,
                    isFavorite: newState.boolValue
                )
            }
        }
    }

    private func loadCharacters(append: Bool = false) async {
        isLoading = true
        error = nil
        let filter = searchQuery.isEmpty ? nil : searchQuery
        do {
            let result = try await getCharacters.invoke(page: currentPage, nameFilter: filter)
            if append {
                characters.append(contentsOf: result.results)
            } else {
                characters = result.results
            }
            hasNextPage = result.info.next != nil
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
```

- [ ] **Step 3: Create CharacterDetailViewModel.swift**

```swift
import SwiftUI
import Shared

@MainActor
class CharacterDetailViewModel: ObservableObject {
    @Published var detail: CharacterDetail?
    @Published var isLoading = false
    @Published var error: String?

    private let getCharacterDetail: GetCharacterDetailUseCase
    private let toggleFavorite: ToggleFavoriteUseCase
    private let characterId: String

    init(characterId: String) {
        self.characterId = characterId
        getCharacterDetail = KoinHelper.shared.resolve(GetCharacterDetailUseCase.self)
        toggleFavorite = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)
        Task { await loadDetail() }
    }

    func onToggleFavorite() {
        guard let character = detail?.character else { return }
        Task {
            let newState = try await toggleFavorite.invoke(character: character)
            detail = detail?.doCopy(
                character: character.doCopy(
                    id: character.id, name: character.name, status: character.status,
                    species: character.species, gender: character.gender,
                    origin: character.origin, location: character.location,
                    imageUrl: character.imageUrl, episodeIds: character.episodeIds,
                    isFavorite: newState.boolValue
                ),
                origin: detail!.origin, location: detail!.location, episodes: detail!.episodes
            )
        }
    }

    private func loadDetail() async {
        isLoading = true
        do {
            detail = try await getCharacterDetail.invoke(id: characterId)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
```

- [ ] **Step 4: Create PostsViewModel.swift**

```swift
import SwiftUI
import Shared

@MainActor
class PostsViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoading = false
    @Published var error: String?

    private let getPosts: GetPostsUseCase
    private let createPost: CreatePostUseCase
    private let updatePost: UpdatePostUseCase
    private let deletePost: DeletePostUseCase

    init() {
        getPosts = KoinHelper.shared.resolve(GetPostsUseCase.self)
        createPost = KoinHelper.shared.resolve(CreatePostUseCase.self)
        updatePost = KoinHelper.shared.resolve(UpdatePostUseCase.self)
        deletePost = KoinHelper.shared.resolve(DeletePostUseCase.self)
        Task { await loadPosts() }
    }

    func loadPosts() async {
        isLoading = true
        error = nil
        do {
            let result = try await getPosts.invoke(page: 1, limit: 10)
            posts = result.posts
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func onCreatePost(title: String, body: String) async {
        do {
            let post = try await createPost.invoke(title: title, body: body)
            posts.insert(post, at: 0)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func onUpdatePost(id: String, title: String, body: String) async {
        do {
            let updated = try await updatePost.invoke(id: id, title: title, body: body)
            if let index = posts.firstIndex(where: { $0.id == id }) {
                posts[index] = updated
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func onDeletePost(id: String) async {
        do {
            _ = try await deletePost.invoke(id: id)
            posts.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
```

- [ ] **Step 5: Create CameraViewModel.swift**

```swift
import SwiftUI
import Shared

@MainActor
class CameraViewModel: ObservableObject {
    @Published var capturedImage: UIImage?
    @Published var photos: [CapturedPhoto] = []
    @Published var isCapturing = false
    @Published var error: String?

    private let capturePhoto: CapturePhotoUseCase
    private let photoRepository: PhotoRepository

    init() {
        capturePhoto = KoinHelper.shared.resolve(CapturePhotoUseCase.self)
        photoRepository = KoinHelper.shared.resolve(PhotoRepository.self)
    }

    func onPhotoCaptured(_ image: UIImage) {
        capturedImage = image
        // Save to documents and store metadata
        if let data = image.jpegData(compressionQuality: 0.8) {
            let filename = "photo_\(Int(Date().timeIntervalSince1970 * 1000)).jpg"
            let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(filename)
            try? data.write(to: url)

            let photo = CapturedPhoto(
                id: 0,
                filePath: url.path,
                timestamp: Int64(Date().timeIntervalSince1970 * 1000),
                location: nil
            )
            Task {
                try? await photoRepository.savePhoto(photo: photo)
            }
        }
    }
}
```

- [ ] **Step 6: Create LocationViewModel.swift**

```swift
import SwiftUI
import Shared
import CoreLocation

@MainActor
class LocationViewModel: ObservableObject {
    @Published var location: GpsLocation?
    @Published var isLoading = false
    @Published var error: String?
    @Published var permissionGranted = false
    @Published var lastUpdated: Date?

    private let getCurrentLocation: GetCurrentLocationUseCase

    init() {
        getCurrentLocation = KoinHelper.shared.resolve(GetCurrentLocationUseCase.self)
        permissionGranted = getCurrentLocation.isPermissionGranted()
    }

    func onRefreshLocation() async {
        isLoading = true
        error = nil
        do {
            location = try await getCurrentLocation.invoke()
            permissionGranted = true
            lastUpdated = Date()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func onPermissionResult(_ granted: Bool) {
        permissionGranted = granted
        if granted {
            Task { await onRefreshLocation() }
        }
    }
}
```

- [ ] **Step 7: Create FavoritesViewModel.swift**

```swift
import SwiftUI
import Shared

@MainActor
class FavoritesViewModel: ObservableObject {
    @Published var favorites: [Character_] = []

    private let getFavorites: GetFavoritesUseCase
    private let toggleFavorite: ToggleFavoriteUseCase

    init() {
        getFavorites = KoinHelper.shared.resolve(GetFavoritesUseCase.self)
        toggleFavorite = KoinHelper.shared.resolve(ToggleFavoriteUseCase.self)
        observeFavorites()
    }

    func onRemoveFavorite(_ character: Character_) {
        Task { _ = try await toggleFavorite.invoke(character: character) }
    }

    private func observeFavorites() {
        Task {
            for await favs in getFavorites.invoke() {
                self.favorites = favs
            }
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add iosApp/Sources/
git commit -m "feat: add iOS SwiftUI ViewModels and KoinHelper for DI"
```

---

## Task 24: Create iOS SwiftUI Screens

**Files:**
- Create: `iosApp/Sources/Components/CharacterRow.swift`
- Create: `iosApp/Sources/Components/PostRow.swift`
- Create: `iosApp/Sources/Components/LoadingView.swift`
- Create: `iosApp/Sources/Components/ErrorView.swift`
- Create: `iosApp/Sources/Navigation/MainTabView.swift`
- Create: `iosApp/Sources/Screens/CharactersScreen.swift`
- Create: `iosApp/Sources/Screens/CharacterDetailScreen.swift`
- Create: `iosApp/Sources/Screens/PostsScreen.swift`
- Create: `iosApp/Sources/Screens/CreateEditPostSheet.swift`
- Create: `iosApp/Sources/Screens/CameraScreen.swift`
- Create: `iosApp/Sources/Screens/LocationScreen.swift`
- Create: `iosApp/Sources/Screens/FavoritesScreen.swift`
- Modify: `iosApp/iosApp.swift`

Due to the size of this task, the exact Swift code for each file is provided below. Each file should be created with the content shown.

- [ ] **Step 1: Create iOS reusable components**

`iosApp/Sources/Components/CharacterRow.swift`:
```swift
import SwiftUI

struct CharacterRow: View {
    let character: Character_
    let onFavoriteToggle: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: character.imageUrl)) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                Circle().fill(.gray.opacity(0.3))
            }
            .frame(width: 56, height: 56)
            .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(character.name).font(.headline)
                HStack(spacing: 4) {
                    Circle()
                        .fill(statusColor(character.status))
                        .frame(width: 8, height: 8)
                    Text("\(character.status.name.capitalized) - \(character.species)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            Button(action: onFavoriteToggle) {
                Image(systemName: character.isFavorite ? "heart.fill" : "heart")
                    .foregroundStyle(character.isFavorite ? .red : .secondary)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 4)
    }

    private func statusColor(_ status: CharacterStatus) -> Color {
        switch status {
        case .alive: return .green
        case .dead: return .red
        default: return .gray
        }
    }
}
```

`iosApp/Sources/Components/PostRow.swift`:
```swift
import SwiftUI

struct PostRow: View {
    let post: Post

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(post.title).font(.headline)
            Text(post.body)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(3)
        }
        .padding(.vertical, 4)
    }
}
```

`iosApp/Sources/Components/LoadingView.swift`:
```swift
import SwiftUI

struct LoadingView: View {
    var body: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
```

`iosApp/Sources/Components/ErrorView.swift`:
```swift
import SwiftUI

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(message).font(.body)
            Button("Retry", action: onRetry)
                .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
```

- [ ] **Step 2: Create MainTabView.swift**

```swift
import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            Tab("Characters", systemImage: "person.3.fill") {
                NavigationStack { CharactersScreen() }
            }
            Tab("Posts", systemImage: "doc.text.fill") {
                NavigationStack { PostsScreen() }
            }
            Tab("Camera", systemImage: "camera.fill") {
                NavigationStack { CameraScreen() }
            }
            Tab("GPS", systemImage: "location.fill") {
                NavigationStack { LocationScreen() }
            }
            Tab("Favorites", systemImage: "heart.fill") {
                NavigationStack { FavoritesScreen() }
            }
        }
    }
}
```

- [ ] **Step 3: Create CharactersScreen.swift**

```swift
import SwiftUI

struct CharactersScreen: View {
    @StateObject private var viewModel = CharactersViewModel()
    @State private var searchText = ""

    var body: some View {
        List {
            ForEach(viewModel.characters, id: \.id) { character in
                NavigationLink(value: character.id) {
                    CharacterRow(
                        character: character,
                        onFavoriteToggle: { viewModel.onToggleFavorite(character) }
                    )
                }
                .onAppear { viewModel.loadNextPageIfNeeded(currentItem: character) }
            }
            if viewModel.isLoading {
                HStack { Spacer(); ProgressView(); Spacer() }
            }
        }
        .navigationTitle("Characters")
        .searchable(text: $searchText, prompt: "Search characters...")
        .onSubmit(of: .search) { viewModel.onSearch(searchText) }
        .refreshable { await viewModel.refresh() }
        .navigationDestination(for: String.self) { id in
            CharacterDetailScreen(characterId: id)
        }
        .overlay {
            if let error = viewModel.error, viewModel.characters.isEmpty {
                ErrorView(message: error, onRetry: { Task { await viewModel.refresh() } })
            }
        }
    }
}
```

- [ ] **Step 4: Create CharacterDetailScreen.swift**

```swift
import SwiftUI

struct CharacterDetailScreen: View {
    let characterId: String
    @StateObject private var viewModel: CharacterDetailViewModel

    init(characterId: String) {
        self.characterId = characterId
        _viewModel = StateObject(wrappedValue: CharacterDetailViewModel(characterId: characterId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingView()
            } else if let error = viewModel.error {
                ErrorView(message: error, onRetry: {})
            } else if let detail = viewModel.detail {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        AsyncImage(url: URL(string: detail.character.imageUrl)) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Rectangle().fill(.gray.opacity(0.3))
                        }
                        .frame(height: 300)
                        .clipped()

                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Label(detail.character.status.name.capitalized, systemImage: "circle.fill")
                                    .font(.caption)
                                    .foregroundStyle(statusColor(detail.character.status))
                                Text(detail.character.species)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }

                            Section("Origin") {
                                Text(detail.origin.name).font(.body)
                                if !detail.origin.type.isEmpty {
                                    Text("\(detail.origin.type) - \(detail.origin.dimension)")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                            }

                            Section("Location") {
                                Text(detail.location.name).font(.body)
                                if !detail.location.type.isEmpty {
                                    Text("\(detail.location.type) - \(detail.location.dimension)")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                            }

                            Section("Episodes (\(detail.episodes.count))") {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack {
                                        ForEach(detail.episodes, id: \.id) { episode in
                                            Text(episode.episode)
                                                .font(.caption)
                                                .padding(.horizontal, 12)
                                                .padding(.vertical, 6)
                                                .background(.blue.opacity(0.1), in: Capsule())
                                        }
                                    }
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }
            }
        }
        .navigationTitle(viewModel.detail?.character.name ?? "Character")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let character = viewModel.detail?.character {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.onToggleFavorite() }) {
                        Image(systemName: character.isFavorite ? "heart.fill" : "heart")
                            .foregroundStyle(character.isFavorite ? .red : .secondary)
                    }
                }
            }
        }
    }

    private func statusColor(_ status: CharacterStatus) -> Color {
        switch status {
        case .alive: return .green
        case .dead: return .red
        default: return .gray
        }
    }

    @ViewBuilder
    private func Section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.headline)
            content()
        }
    }
}
```

- [ ] **Step 5: Create PostsScreen.swift and CreateEditPostSheet.swift**

`iosApp/Sources/Screens/PostsScreen.swift`:
```swift
import SwiftUI

struct PostsScreen: View {
    @StateObject private var viewModel = PostsViewModel()
    @State private var showCreateSheet = false
    @State private var editingPost: Post?

    var body: some View {
        List {
            if viewModel.posts.isEmpty && !viewModel.isLoading {
                ContentUnavailableView("No Posts", systemImage: "doc.text", description: Text("Tap + to create one"))
            }
            ForEach(viewModel.posts, id: \.id) { post in
                PostRow(post: post)
                    .onTapGesture { editingPost = post }
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            Task { await viewModel.onDeletePost(id: post.id) }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
            }
        }
        .navigationTitle("Posts")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showCreateSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .refreshable { await viewModel.loadPosts() }
        .sheet(isPresented: $showCreateSheet) {
            CreateEditPostSheet { title, body in
                Task { await viewModel.onCreatePost(title: title, body: body) }
            }
        }
        .sheet(item: $editingPost) { post in
            CreateEditPostSheet(post: post) { title, body in
                Task { await viewModel.onUpdatePost(id: post.id, title: title, body: body) }
            }
        }
        .overlay {
            if viewModel.isLoading && viewModel.posts.isEmpty { LoadingView() }
        }
    }
}
```

`iosApp/Sources/Screens/CreateEditPostSheet.swift`:
```swift
import SwiftUI

struct CreateEditPostSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var title: String
    @State private var body: String
    let isEdit: Bool
    let onSave: (String, String) -> Void

    init(post: Post? = nil, onSave: @escaping (String, String) -> Void) {
        _title = State(initialValue: post?.title ?? "")
        _body = State(initialValue: post?.body ?? "")
        isEdit = post != nil
        self.onSave = onSave
    }

    var body_: some View {
        NavigationStack {
            Form {
                TextField("Title", text: $title)
                Section("Body") {
                    TextEditor(text: $body)
                        .frame(minHeight: 150)
                }
            }
            .navigationTitle(isEdit ? "Edit Post" : "New Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEdit ? "Update" : "Create") {
                        onSave(title, body)
                        dismiss()
                    }
                    .disabled(title.isEmpty || body.isEmpty)
                }
            }
        }
    }
}
```

- [ ] **Step 6: Create CameraScreen.swift**

```swift
import SwiftUI

struct CameraScreen: View {
    @StateObject private var viewModel = CameraViewModel()
    @State private var showCamera = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Button(action: { showCamera = true }) {
                    Label("Capture Photo", systemImage: "camera.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.horizontal)

                if let image = viewModel.capturedImage {
                    VStack(alignment: .leading) {
                        Text("Last Captured").font(.headline).padding(.horizontal)
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                            .frame(maxHeight: 250)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal)
                    }
                }

                if !viewModel.photos.isEmpty {
                    VStack(alignment: .leading) {
                        Text("Photo History").font(.headline).padding(.horizontal)
                        ForEach(viewModel.photos, id: \.id) { photo in
                            HStack {
                                if let uiImage = UIImage(contentsOfFile: photo.filePath) {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 56, height: 56)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                VStack(alignment: .leading) {
                                    Text("Photo #\(photo.id)")
                                    if let loc = photo.location {
                                        Text("\(loc.latitude, specifier: "%.4f"), \(loc.longitude, specifier: "%.4f")")
                                            .font(.caption).foregroundStyle(.secondary)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                }
            }
            .padding(.vertical)
        }
        .navigationTitle("Camera")
        .sheet(isPresented: $showCamera) {
            ImagePicker { image in
                viewModel.onPhotoCaptured(image)
            }
        }
    }
}

struct ImagePicker: UIViewControllerRepresentable {
    let onImagePicked: (UIImage) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onImagePicked: onImagePicked) }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImagePicked: (UIImage) -> Void
        init(onImagePicked: @escaping (UIImage) -> Void) { self.onImagePicked = onImagePicked }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage { onImagePicked(image) }
            picker.dismiss(animated: true)
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            picker.dismiss(animated: true)
        }
    }
}
```

- [ ] **Step 7: Create LocationScreen.swift**

```swift
import SwiftUI
import CoreLocation

struct LocationScreen: View {
    @StateObject private var viewModel = LocationViewModel()

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "location.fill")
                .font(.system(size: 56))
                .foregroundStyle(.blue)

            if !viewModel.permissionGranted {
                VStack(spacing: 12) {
                    Text("Location Permission Required").font(.headline)
                    Text("Grant location permission to see your coordinates.")
                        .font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Button("Grant Permission") {
                        // Triggers the Kotlin-side permission request
                        Task { await viewModel.onRefreshLocation() }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            } else {
                GroupBox("Current Location") {
                    VStack(spacing: 8) {
                        if let loc = viewModel.location {
                            LocationRow(label: "Latitude", value: String(format: "%.6f", loc.latitude))
                            LocationRow(label: "Longitude", value: String(format: "%.6f", loc.longitude))
                            if let acc = loc.accuracy {
                                LocationRow(label: "Accuracy", value: String(format: "%.1f m", acc.floatValue))
                            }
                        } else {
                            Text("No location data yet").foregroundStyle(.secondary)
                        }

                        if let lastUpdated = viewModel.lastUpdated {
                            Text("Last updated: \(lastUpdated, style: .time)")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.horizontal)

                Button(action: { Task { await viewModel.onRefreshLocation() } }) {
                    Label(viewModel.isLoading ? "Fetching..." : "Refresh Location", systemImage: "arrow.clockwise")
                }
                .buttonStyle(.bordered)
                .disabled(viewModel.isLoading)

                if let error = viewModel.error {
                    Text(error).foregroundStyle(.red).font(.caption)
                }
            }

            Spacer()
        }
        .padding(.top, 32)
        .navigationTitle("GPS Location")
        .task {
            if viewModel.permissionGranted { await viewModel.onRefreshLocation() }
        }
    }
}

private struct LocationRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value)
        }
    }
}
```

- [ ] **Step 8: Create FavoritesScreen.swift**

```swift
import SwiftUI

struct FavoritesScreen: View {
    @StateObject private var viewModel = FavoritesViewModel()

    var body: some View {
        Group {
            if viewModel.favorites.isEmpty {
                ContentUnavailableView(
                    "No Favorites",
                    systemImage: "heart.slash",
                    description: Text("Tap the heart icon on a character to add it here")
                )
            } else {
                List(viewModel.favorites, id: \.id) { character in
                    NavigationLink(value: character.id) {
                        CharacterRow(
                            character: character,
                            onFavoriteToggle: { viewModel.onRemoveFavorite(character) }
                        )
                    }
                    .swipeActions {
                        Button(role: .destructive) {
                            viewModel.onRemoveFavorite(character)
                        } label: {
                            Label("Unfavorite", systemImage: "heart.slash")
                        }
                    }
                }
                .navigationDestination(for: String.self) { id in
                    CharacterDetailScreen(characterId: id)
                }
            }
        }
        .navigationTitle("Favorites")
    }
}
```

- [ ] **Step 9: Rewrite iosApp.swift entry point**

```swift
import SwiftUI

@main
struct iOSApp: App {
    init() {
        KoinHelper.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
```

- [ ] **Step 10: Update Info.plist with permissions**

Add to `iosApp/Info.plist` (or create if it doesn't exist as a standalone file — it may be in the Xcode project):
```xml
<key>NSCameraUsageDescription</key>
<string>Take photos to attach to your posts</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Tag your photos and posts with your current location</string>
```

- [ ] **Step 11: Commit**

```bash
git add iosApp/
git commit -m "feat: add complete iOS SwiftUI screens, components, navigation, and entry point"
```

---

## Task 25: Create Shared Module Unit Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/com/example/kmp_basic_app/data/repository/FakeRepositories.kt`
- Create: `shared/src/commonTest/kotlin/com/example/kmp_basic_app/domain/usecase/GetCharactersUseCaseTest.kt`
- Create: `shared/src/commonTest/kotlin/com/example/kmp_basic_app/domain/usecase/ToggleFavoriteUseCaseTest.kt`
- Create: `shared/src/commonTest/kotlin/com/example/kmp_basic_app/domain/usecase/GetPostsUseCaseTest.kt`

- [ ] **Step 1: Create FakeRepositories.kt**

```kotlin
package com.example.kmp_basic_app.data.repository

import com.example.kmp_basic_app.domain.model.*
import com.example.kmp_basic_app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository(
    private val result: Result<CharacterPage> = Result.success(
        CharacterPage(PageInfo(0, 0, null), emptyList())
    ),
    private val detailResult: Result<CharacterDetail>? = null
) : CharacterRepository {
    override suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage> = result
    override suspend fun getCharacterDetail(id: String): Result<CharacterDetail> =
        detailResult ?: Result.failure(Exception("Not configured"))
}

class FakePostRepository(
    private val postsResult: Result<PostPage> = Result.success(PostPage(emptyList(), 0)),
    private val createResult: Result<Post> = Result.success(Post("1", "Test", "Body")),
    private val updateResult: Result<Post> = Result.success(Post("1", "Updated", "Body")),
    private val deleteResult: Result<Boolean> = Result.success(true)
) : PostRepository {
    override suspend fun getPosts(page: Int, limit: Int): Result<PostPage> = postsResult
    override suspend fun createPost(title: String, body: String): Result<Post> = createResult
    override suspend fun updatePost(id: String, title: String, body: String): Result<Post> = updateResult
    override suspend fun deletePost(id: String): Result<Boolean> = deleteResult
}

class FakeFavoriteRepository : FavoriteRepository {
    private val favorites = MutableStateFlow<List<Character>>(emptyList())
    private val favoriteIds = mutableSetOf<String>()

    override fun observeFavorites(): Flow<List<Character>> = favorites
    override suspend fun toggleFavorite(character: Character): Boolean {
        return if (favoriteIds.contains(character.id)) {
            favoriteIds.remove(character.id)
            favorites.value = favorites.value.filter { it.id != character.id }
            false
        } else {
            favoriteIds.add(character.id)
            favorites.value = favorites.value + character.copy(isFavorite = true)
            true
        }
    }
    override suspend fun isFavorite(characterId: String): Boolean = favoriteIds.contains(characterId)
}
```

- [ ] **Step 2: Create GetCharactersUseCaseTest.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakeCharacterRepository
import com.example.kmp_basic_app.data.repository.FakeFavoriteRepository
import com.example.kmp_basic_app.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCharactersUseCaseTest {

    private val testCharacters = listOf(
        Character("1", "Rick", CharacterStatus.ALIVE, "Human", "Male",
            LocationBrief(null, "Earth"), LocationBrief(null, "Citadel"),
            "https://img.com/1.jpg", listOf("1", "2")),
        Character("2", "Morty", CharacterStatus.ALIVE, "Human", "Male",
            LocationBrief(null, "Earth"), LocationBrief(null, "Earth"),
            "https://img.com/2.jpg", listOf("1"))
    )

    @Test
    fun returnsCharactersFromRepository() = runTest {
        val page = CharacterPage(PageInfo(2, 1, null), testCharacters)
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.success(page)),
            FakeFavoriteRepository()
        )

        val result = useCase(page = 1)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().results.size)
        assertEquals("Rick", result.getOrThrow().results[0].name)
    }

    @Test
    fun marksFavoritedCharacters() = runTest {
        val page = CharacterPage(PageInfo(1, 1, null), testCharacters)
        val favRepo = FakeFavoriteRepository()
        favRepo.toggleFavorite(testCharacters[0]) // Rick is favorited
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.success(page)),
            favRepo
        )

        val result = useCase(page = 1)

        assertTrue(result.getOrThrow().results[0].isFavorite)
        assertTrue(!result.getOrThrow().results[1].isFavorite)
    }

    @Test
    fun returnsErrorOnNetworkFailure() = runTest {
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.failure(Exception("timeout"))),
            FakeFavoriteRepository()
        )

        val result = useCase(page = 1)

        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: Create ToggleFavoriteUseCaseTest.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakeFavoriteRepository
import com.example.kmp_basic_app.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToggleFavoriteUseCaseTest {

    private val character = Character(
        "1", "Rick", CharacterStatus.ALIVE, "Human", "Male",
        LocationBrief(null, "Earth"), LocationBrief(null, "Citadel"),
        "https://img.com/1.jpg", emptyList()
    )

    @Test
    fun togglesOnWhenNotFavorited() = runTest {
        val repo = FakeFavoriteRepository()
        val useCase = ToggleFavoriteUseCase(repo)

        val result = useCase(character)

        assertTrue(result)
        assertTrue(repo.isFavorite(character.id))
    }

    @Test
    fun togglesOffWhenAlreadyFavorited() = runTest {
        val repo = FakeFavoriteRepository()
        repo.toggleFavorite(character)
        val useCase = ToggleFavoriteUseCase(repo)

        val result = useCase(character)

        assertFalse(result)
        assertFalse(repo.isFavorite(character.id))
    }
}
```

- [ ] **Step 4: Create GetPostsUseCaseTest.kt**

```kotlin
package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakePostRepository
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPostsUseCaseTest {

    @Test
    fun returnsPostsFromRepository() = runTest {
        val posts = listOf(Post("1", "Title", "Body"), Post("2", "Title 2", "Body 2"))
        val useCase = GetPostsUseCase(FakePostRepository(Result.success(PostPage(posts, 2))))

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().posts.size)
    }

    @Test
    fun returnsErrorOnFailure() = runTest {
        val useCase = GetPostsUseCase(FakePostRepository(Result.failure(Exception("network error"))))

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 5: Run tests**

Run:
```bash
cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:allTests --no-daemon 2>&1 | tail -20
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonTest/
git commit -m "test: add unit tests for GetCharacters, ToggleFavorite, and GetPosts use cases"
```

---

## Task 26: Final Build Verification

- [ ] **Step 1: Build shared module**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:build --no-daemon 2>&1 | tail -10`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Build Android app**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :composeApp:assembleDebug --no-daemon 2>&1 | tail -10`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run all tests**

Run: `cd /home/anonymous/AndroidStudioProjects/KMP_Basic_App && ./gradlew :shared:allTests --no-daemon 2>&1 | tail -10`

Expected: All tests pass.

- [ ] **Step 4: Fix any remaining issues iteratively**

If builds fail, read errors, fix files, re-run until green.

- [ ] **Step 5: Final commit**

```bash
git add -A && git commit -m "chore: final build verification and fixes"
```

---

## Summary

| Task | Description | Estimated Steps |
|------|-------------|----------------|
| 1 | Version catalog dependencies | 3 |
| 2 | Create shared module, update build files | 7 |
| 3 | Download GraphQL schemas | 4 |
| 4 | Write GraphQL operations | 4 |
| 5 | SQLDelight schema files | 4 |
| 6 | Domain models | 5 |
| 7 | Repository interfaces | 5 |
| 8 | UseCases (10 files) | 11 |
| 9 | Platform expect declarations | 3 |
| 10 | SQLDelight DatabaseDriverFactory | 4 |
| 11 | Apollo clients + mappers | 5 |
| 12 | Repository implementations | 5 |
| 13 | Android platform actuals | 3 |
| 14 | iOS platform actuals | 3 |
| 15 | Koin shared DI module | 3 |
| 16 | Shared module build verification | 3 |
| 17 | Android theme + navigation | 3 |
| 18 | Android ViewModels (6 files) | 7 |
| 19 | Android reusable components | 4 |
| 20 | Android screens (7 files) | 8 |
| 21 | Android DI, App, Manifest | 7 |
| 22 | Android build verification | 3 |
| 23 | iOS ViewModels (7 files) | 8 |
| 24 | iOS screens + components (13 files) | 11 |
| 25 | Shared unit tests | 6 |
| 26 | Final build verification | 5 |
| **Total** | **26 tasks** | **~133 steps** |
