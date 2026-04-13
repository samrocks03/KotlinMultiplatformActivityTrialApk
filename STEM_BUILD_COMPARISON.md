# STEM Platform — Build Size Comparison
## React Native vs KMP (both with Supabase Edge Functions)

---

## App Scope Recap

9+ modules: School, Student, Trainer, Teacher, Session, Attendance, Assessment, Donor, Reporting, LMS/SCORM. Features: offline sync, OMR camera capture, geolocation, push notifications, WebSocket real-time, charts/dashboards, PDF/Excel export, multi-tenant RBAC, approval workflows.

---

## 1. Android APK Size (arm64, release, optimized)

### React Native

| Component | Size |
|---|---|
| React Native core + Hermes engine | ~7-8 MB |
| JS bundle (9+ modules, Hermes bytecode) | ~5-10 MB |
| react-native-firebase (messaging + analytics) | ~6-10 MB |
| WatermelonDB (offline sync) | ~2 MB |
| React Navigation | ~0.5-1 MB |
| expo-camera / RN camera | ~3-5 MB |
| Geolocation | ~0.5-1 MB |
| Charts (Victory Native) | ~1-2 MB |
| WebView (SCORM player) | ~1-2 MB |
| react-native-reanimated | ~1.5-2.5 MB |
| Supabase JS client | ~0.5-1 MB |
| Other deps (image picker, file system, etc.) | ~2-3 MB |
| App assets (icons, fonts, images) | ~2-5 MB |
| **TOTAL APK** | **~45-65 MB** |
| On-device installed | ~90-130 MB |

### KMP (Jetpack Compose)

| Component | Size |
|---|---|
| Shared KMP module (models, repos, use cases, Ktor, Koin) | ~0.6-1 MB (after R8) |
| Jetpack Compose (full UI toolkit) | ~2-4 MB |
| Ktor client (networking) | ~0.4-0.6 MB (after R8) |
| SQLDelight (offline DB) | ~0.3-0.5 MB |
| Koin (DI) | ~0.15-0.25 MB |
| CameraX | ~1.5-2.5 MB |
| Firebase Messaging | ~0.4-0.8 MB |
| Charts (Vico or similar) | ~0.5-1 MB |
| Geolocation (Play Services subset) | ~0.5-1 MB |
| WebView (SCORM — system component) | ~0 MB |
| App assets (icons, fonts, images) | ~2-5 MB |
| **TOTAL APK** | **~10-18 MB** |
| On-device installed | ~25-45 MB |

### Android Winner: KMP by 3-4x

---

## 2. iOS IPA Size (arm64, App Store thinned)

### React Native

| Component | Size |
|---|---|
| React Native + Hermes runtime | ~8-12 MB |
| JS bundle (Hermes bytecode) | ~5-10 MB |
| Firebase Messaging (iOS SDK) | ~2-3 MB |
| WatermelonDB | ~1.5-2 MB |
| Camera, Maps, Charts (native bridges) | ~3-5 MB |
| WebView (system — free) | ~0 MB |
| Other RN native modules | ~2-4 MB |
| App assets | ~2-5 MB |
| **TOTAL IPA (download)** | **~35-50 MB** |
| On-device installed | ~70-100 MB |

### KMP (SwiftUI)

| Component | Size |
|---|---|
| Kotlin/Native shared framework (static, stripped) | ~4-8 MB |
| SwiftUI (system framework) | ~0 MB |
| AVFoundation / CoreLocation (system) | ~0 MB |
| APNs (system — no Firebase needed) | ~0 MB |
| Charts (Swift Charts — system on iOS 16+) | ~0 MB |
| WebView (WKWebView — system) | ~0 MB |
| SQLDelight iOS driver | bundled in framework |
| App assets | ~2-5 MB |
| **TOTAL IPA (download)** | **~8-18 MB** |
| On-device installed | ~20-40 MB |

### iOS Winner: KMP by 2-3x

**Why the gap is so large on iOS:** SwiftUI, AVFoundation, CoreLocation, WKWebView, Swift Charts, and APNs are all **system frameworks** — they cost zero bytes in your IPA. React Native must bundle its entire runtime + bridge + every native module.

---

## 3. Head-to-Head Summary

| Metric | React Native | KMP | Difference |
|---|---|---|---|
| **Android APK** | 45-65 MB | 10-18 MB | **3-4x smaller** |
| **Android installed** | 90-130 MB | 25-45 MB | **3x smaller** |
| **iOS IPA download** | 35-50 MB | 8-18 MB | **2-3x smaller** |
| **iOS installed** | 70-100 MB | 20-40 MB | **2.5x smaller** |
| **Runtime overhead** | Hermes JS engine always running | None — native binary | KMP wins |
| **Memory baseline** | +30-50 MB (JS heap) | ~0 MB extra | KMP wins |
| **Startup time** | 1-3s (JS init + bridge) | <500ms (native) | KMP wins |

---

## 4. Why KMP Is Smaller — The Fundamental Reason

```
React Native:
  Your App Code (JS) → Hermes Engine → Bridge → Native APIs
  [everything in this chain ships in your binary]

KMP:
  Shared Logic (Kotlin) → compiles to native binary (no runtime)
  Android UI (Compose) → Kotlin (already native)
  iOS UI (SwiftUI) → Swift (already native, system frameworks = free)
  [no engine, no bridge, no runtime to ship]
```

React Native ships a **parallel runtime** (Hermes + bridge) alongside the OS runtime. KMP compiles away — your shared code becomes native code on each platform.

---

## 5. Supabase Edge Functions — Shared Backend Concerns

Both approaches use the same backend, but there are limits:

| Workload | Edge Function Capable? | Alternative Needed? |
|---|---|---|
| CRUD operations | Yes | No |
| Auth (JWT, RLS) | Yes (built-in) | No |
| Approval workflows | Yes | No |
| Cron jobs (escalation, reminders) | Yes (pg_cron + pg_net) | No |
| WebSocket / real-time | Yes (Supabase Realtime) | No |
| Multi-table aggregations | Yes (runs in Postgres) | No |
| **OMR image processing** | **No** (2s CPU limit, no OpenCV) | **Yes — Cloud Run / Lambda** |
| **PDF generation (Puppeteer)** | **Partial** (needs external browser) | **Yes — Browserless.io** |
| **Large Excel exports** | **Risky** (150 MB memory cap) | **Yes — background worker** |
| **Heavy analytics/reporting** | **Risky** (load on primary DB) | **Consider read replica** |

### Edge Function Hard Limits
- Wall clock: 400s max
- CPU time: 2s per request (async I/O excluded)
- Memory: 150 MB heap
- Bundle size: 20 MB

**Bottom line:** Edge Functions handle ~80% of this app's backend needs. You still need a separate microservice for OMR processing and heavy PDF/Excel generation — regardless of whether the frontend is RN or KMP.

---

## 6. Supabase Storage vs AWS S3

| Feature | Supabase Storage | AWS S3 |
|---|---|---|
| Max upload (simple) | 6 MB | 5 GB |
| Max upload (resumable/multipart) | 50 GB (TUS protocol) | 5 TB |
| Presigned URLs | Yes | Yes |
| Direct mobile upload | Yes (TUS + signed URLs) | Yes |
| S3-compatible API | Yes (Storage v3) | Native |
| Lifecycle policies | No | Yes |
| Cross-region replication | No | Yes |
| Storage classes (Glacier, etc.) | No | Yes |
| CDN integration | Supabase CDN (basic) | CloudFront (advanced) |

**For this app:** Supabase Storage is sufficient. OMR sheets, SCORM packages, documents, and photos don't need lifecycle policies or cross-region replication. The TUS resumable upload is actually better for mobile than S3's multipart.

---

## 7. Total Architecture Comparison

### Option A: React Native + Supabase

```
React Native App (JS/TS)
├── Hermes Engine (bundled runtime)
├── JS Bridge (bundled)
├── WatermelonDB (offline)
├── All native modules (bundled)
└── Supabase JS Client
        │
        ▼
Supabase Edge Functions (Deno)    ← handles 80% of backend
Supabase Auth (JWT + RLS)
Supabase DB (PostgreSQL)
Supabase Storage
        │
        ▼
External Services:
├── Cloud Run: OMR Processing (Python/OpenCV)
├── Browserless: PDF Generation
└── Firebase: Push Notifications (both platforms)
```

### Option B: KMP + Supabase

```
Android App (Kotlin/Compose)  ←──┐
iOS App (Swift/SwiftUI)       ←──┤  shared KMP module
        │                         │  (compiled to native)
        │    Shared Module ───────┘
        │    ├── Models, Repos, UseCases
        │    ├── Ktor Client
        │    ├── SQLDelight (offline)
        │    └── Koin DI
        ▼
Supabase Edge Functions (Deno)    ← handles 80% of backend
Supabase Auth (JWT + RLS)
Supabase DB (PostgreSQL)
Supabase Storage
        │
        ▼
External Services:
├── Cloud Run: OMR Processing (Python/OpenCV)
├── Browserless: PDF Generation
└── Android: FCM | iOS: APNs (native, no Firebase SDK on iOS)
```

---

## 8. The Real Trade-off

| Factor | React Native | KMP |
|---|---|---|
| **Build size** | 3-4x larger | Smallest possible |
| **Runtime performance** | Good (Hermes), but JS bridge overhead | Native — no overhead |
| **Offline support** | WatermelonDB (good, but JS layer) | SQLDelight (native SQL, compiled) |
| **Team requirement** | JS/TS devs (one team) | Kotlin + Swift devs (need both) |
| **Code sharing** | ~90% (UI + logic) | ~40-50% (logic only, UI separate) |
| **Dev speed** | Faster — one codebase for both UIs | Slower — two UI codebases |
| **UI quality** | Good, not pixel-perfect native | Pixel-perfect native on both |
| **SCORM player** | WebView (same either way) | WebView (same either way) |
| **Camera/OMR capture** | RN camera module | CameraX / AVFoundation |
| **Long-term maintenance** | RN upgrades can be painful | Stable — native toolchains |
| **Hiring** | Easier — larger JS/TS talent pool | Harder — need Kotlin + Swift |

---

## 9. Recommendation for This STEM App

**If build size and performance matter most (field conditions, low-end devices, poor connectivity):**
→ **KMP wins.** 10-18 MB APK vs 45-65 MB. For trainers in rural India on budget phones with limited storage, this is a real difference.

**If dev speed and team simplicity matter most:**
→ **React Native wins.** One team, one codebase, faster iteration. The 45-65 MB APK is still reasonable.

**The offline story favors KMP:** SQLDelight compiles to native SQLite on both platforms with zero bridge overhead. WatermelonDB is good but adds a JS-to-native layer for every DB operation.

**Both approaches need the same external services:** OMR processing, PDF generation, and heavy analytics can't run on Edge Functions regardless of the frontend stack.
