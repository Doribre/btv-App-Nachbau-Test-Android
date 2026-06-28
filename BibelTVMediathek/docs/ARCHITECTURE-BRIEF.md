Verification complete. The findings were largely accurate on versions (Kotlin 2.4.0, Compose BOM 2026.06.00, Media3 1.10.x are all real as of June 2026). The one material correction: **Apollo Kotlin's safe stable choice is 4.4.3** on the Gradle Plugin Portal, not the RC/Maven-only 5.x that the findings overstated. AGP 9 is real but bleeding-edge; I'll recommend AGP 8.x stable. I now have everything to write the prescriptive brief.

---

# Bibel TV Mediathek â€” Android Build Brief (v1)

Jetpack Compose Â· Apollo Kotlin Â· Media3 Â· Hilt Â· No login. This is the prescriptive spec to build from. Where the research disagreed with reality, I corrected it (notably Apollo and AGP versions). Decisions are made for you â€” deviate only with a reason.

---

## 1. Screen List + Navigation Structure

**Bottom navigation bar, 4 tabs** (not 5 â€” drop "Downloads" from v1; no login means no robust offline-rights story yet, and a dead tab reads as broken):

| Tab | Route | Purpose |
|-----|-------|---------|
| **Start** (Home) | `Home` | Curated hero + content rows + live strip |
| **Entdecken** (Browse) | `Browse` | Category/content-type grid, filter chips |
| **Suche** (Search) | `Search` | Query + results, content-type filter |
| **Live** | `Live` | Live streams + FAST channels (Bibel TV is a broadcaster â€” live belongs as a first-class tab, not buried) |

Settings is **not** a tab â€” it lives behind a gear icon in the Home top app bar (`Settings` route). With no account, settings is thin (language, theme, autoplay, about/impressum, data protection).

**Full destination list (type-safe `@Serializable` routes):**

```kotlin
sealed interface Route {
  @Serializable data object Home : Route
  @Serializable data object Browse : Route
  @Serializable data object Search : Route
  @Serializable data object Live : Route
  @Serializable data object Settings : Route

  // Detail layer (pushed above the tab scaffold)
  @Serializable data class VideoDetail(val id: String) : Route   // single film / sermon / audio
  @Serializable data class SeriesDetail(val id: String) : Route  // series with seasons/episodes
  @Serializable data class Category(val id: String, val title: String) : Route // "all X" grid
  @Serializable data class Player(val id: String, val kind: PlayableKind) : Route // VOD/LIVE/AUDIO
}
```

**Navigation rules:**
- A **single top-level `NavHost`**. The bottom bar swaps between the 4 tab graphs; detail/player destinations push on top and **hide the bottom bar**.
- Each tab is its own **nested nav graph** so tabs retain independent back stacks (standard Compose `saveState`/`restoreState` pattern on `navigate`).
- Player is a **separate full-screen destination**, not an inline sheet â€” needed for fullscreen, PiP, orientation control.
- `Browse` chips filter in-place; tapping "Mehr" on a category opens the `Category` grid (paged).
- **Adaptive:** â‰¥600dp width â†’ replace bottom bar with a `NavigationRail`; â‰¥840dp â†’ `NavigationRail` + two-pane (list/detail) on Browse and Search. Use `WindowSizeClass`.
- Type-safe args via `kotlinx.serialization` (Navigation Compose 2.8+ `composable<Route.X>` + `toRoute()`).

---

## 2. Home-Screen Composition

A single `LazyColumn` of typed row sections, driven by editorial/algorithmic curation (no watch history exists in v1). Order top-to-bottom:

1. **Hero carousel** â€” full-bleed, 16:9, auto-advancing `HorizontalPager` (3â€“5 editorial picks). Bottom gradient scrim (`Color.Black` 0% â†’ ~60%) for title legibility. Respects top safe inset. One primary CTA: **Ansehen**. Sharp corners (hero is immersive; cards are rounded).
2. **Live jetzt** â€” horizontal strip of currently-airing live/FAST channels with a red "LIVE" badge. Tapping goes straight to `Player(kind = LIVE)`.
3. **Neu in der Mediathek** â€” newest VOD across all types.
4. **Editorial collections** â€” 1â€“3 themed rows curated server-side ("Ostern", "FÃ¼r Kinder", etc.).
5. **Per-content-type rows:** Filme, Serien, Predigten, Audios â€” each a horizontal rail, last card peeking ~30% to signal scroll, "Mehr" affordance â†’ `Category` grid.

**Card spec:** 16:9 thumbnail (Coil), 8dp rounded corners, 12dp gap, title on **2 lines max below** the card (German titles run long â€” do not overlay title on the thumbnail in rails). Min touch target 48dp. Series cards get a small "Serie" pill; audios get a headphones glyph + duration.

**States:** skeleton shimmer per row on first load (rows fade in progressively); offline â†’ show cached rows if present, else a retry empty-state. Each row is independently loadable so one failing query doesn't blank the screen.

The Home screen is **one ViewModel** exposing `StateFlow<HomeUiState>` where `HomeUiState.Content` holds a `List<HomeRow>` (sealed: `Hero`, `LiveStrip`, `Rail(type, items)`). Rows are fetched in parallel.

---

## 3. Architecture + Package Structure

**Pattern:** MVVM + strict UDF. UI emits events upward (ViewModel function calls); ViewModel exposes one `StateFlow<UiState>` downward. One-shot effects (navigation, snackbars) go through a `Channel`/`receiveAsFlow`. Immutable sealed `UiState` (`Loading`/`Content`/`Error`) per screen.

**Modularization:** Do **not** start with the full 12-module multi-module setup from the findings â€” that's premature for a v1 with one team. Start **single-module, package-by-feature**, but lay the package boundaries so you *can* extract modules later. Promote to modules only when build times or team size demand it.

```
app/src/main/kotlin/de/bibeltv/mediathek/
â”œâ”€â”€ MediathekApp.kt              // @HiltAndroidApp
â”œâ”€â”€ MainActivity.kt              // @AndroidEntryPoint, setContent { MediathekTheme { AppRoot() } }
â”œâ”€â”€ core/
â”‚   â”œâ”€â”€ designsystem/            // theme/, components/ (VideoCard, Rail, Skeleton, ErrorState)
â”‚   â”‚   â”œâ”€â”€ theme/               // Color.kt, Type.kt, Shape.kt, Theme.kt
â”‚   â”‚   â””â”€â”€ component/
â”‚   â”œâ”€â”€ ui/                      // WindowSizeClass helpers, Modifier ext, formatters (German date/duration)
â”‚   â””â”€â”€ common/                  // Result wrappers, dispatchers, constants
â”œâ”€â”€ data/
â”‚   â”œâ”€â”€ graphql/                 // generated Apollo code lands here (packageName)
â”‚   â”œâ”€â”€ apollo/                  // ApolloClient builder, interceptors, cache config
â”‚   â”œâ”€â”€ paging/                  // VideoHubPagingSource<T>
â”‚   â”œâ”€â”€ mapper/                  // GraphQL DTO -> domain model
â”‚   â”œâ”€â”€ repository/              // VideoRepository, LiveRepository, SearchRepository (thin)
â”‚   â””â”€â”€ prefs/                   // DataStore (theme, language, autoplay)
â”œâ”€â”€ domain/
â”‚   â””â”€â”€ model/                   // Video, Series, Episode, LiveChannel, ContentType (clean, Apollo-free)
â”œâ”€â”€ feature/
â”‚   â”œâ”€â”€ home/                    // HomeScreen, HomeViewModel, HomeUiState, components
â”‚   â”œâ”€â”€ browse/
â”‚   â”œâ”€â”€ search/
â”‚   â”œâ”€â”€ live/
â”‚   â”œâ”€â”€ detail/                  // VideoDetailScreen, SeriesDetailScreen + VMs
â”‚   â”œâ”€â”€ player/                  // PlayerScreen, PlayerViewModel, controls
â”‚   â””â”€â”€ settings/
â”œâ”€â”€ player/                      // Media3 layer: PlayerFactory, MediaItem builders, PlaybackService
â””â”€â”€ navigation/                  // Route.kt, MediathekNavHost.kt, bottom bar / rail scaffold
```

**DI (Hilt + KSP):** `@HiltViewModel` ViewModels; `@Module @InstallIn(SingletonComponent)` provides `ApolloClient` (singleton), repositories, DataStore, `PlayerFactory`. Use **KSP**, not KAPT.

**Domain models are Apollo-free.** Map generated GraphQL types to `domain/model` in the data layer. This is the one place the findings under-stressed: if `VideoDetail`/`SeriesDetail` Compose code touches generated Apollo classes directly, every schema change ripples into the UI. Map at the repository boundary.

---

## 4. Concrete Dependency List (verified stable, June 2026)

Use a `libs.versions.toml` version catalog. **Corrections to the raw findings are flagged.**

```toml
[versions]
kotlin = "2.4.0"                 # stable (released 2026-06-03)
agp = "8.13.0"                   # CORRECTION: use stable AGP 8.x, NOT the bleeding-edge "9.0.1".
                                 # AGP 9 + compileSdk 37 is real but lands with Compose 1.12; don't
                                 # start a new prod app on it. Pin compileSdk 36, minSdk 26.
ksp = "2.4.0-2.0.2"              # must match the Kotlin version prefix
composeBom = "2026.06.00"        # stable; core Compose 1.11, Material3 1.4
navigation = "2.9.8"
lifecycle = "2.9.4"
coroutines = "1.10.2"
serialization = "1.9.0"
hilt = "2.57.1"
hiltNavCompose = "1.2.0"
paging = "3.4.2"
coil = "3.3.0"                   # CORRECTION: "3.5.0 / June 2026" was not verified; pin a known 3.x
media3 = "1.10.0"               # stable (2026-03); 1.11 is alpha â€” stay on 1.10
apollo = "4.4.3"                 # CORRECTION: findings said 5.0.x. Apollo 5 is RC / Maven-Central-only.
                                 # 4.4.3 is the current Gradle-Plugin-Portal stable. Use it for v1.
datastore = "1.1.7"
windowSizeClass = "1.4.0"        # androidx.compose.material3:material3-window-size-class (BOM-managed)

[libraries]
# Compose (BOM-managed; no versions)
compose-bom            = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui            = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling    = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-foundation    = { group = "androidx.compose.foundation", name = "foundation" }
compose-material3     = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-material3-window = { group = "androidx.compose.material3", name = "material3-window-size-class" }

# Activity / Navigation / Lifecycle
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Coroutines / Serialization
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# DI
hilt-android   = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler  = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavCompose" }

# Paging
androidx-paging-runtime = { group = "androidx.paging", name = "paging-runtime", version.ref = "paging" }
androidx-paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }

# Apollo (GraphQL)
apollo-runtime = { group = "com.apollographql.apollo", name = "apollo-runtime", version.ref = "apollo" }
apollo-normalized-cache = { group = "com.apollographql.apollo", name = "apollo-normalized-cache", version.ref = "apollo" }
apollo-normalized-cache-sqlite = { group = "com.apollographql.apollo", name = "apollo-normalized-cache-sqlite", version.ref = "apollo" }

# Media3
media3-exoplayer       = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-exoplayer-hls   = { group = "androidx.media3", name = "media3-exoplayer-hls", version.ref = "media3" }
media3-exoplayer-dash  = { group = "androidx.media3", name = "media3-exoplayer-dash", version.ref = "media3" }
media3-session         = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-ui-compose      = { group = "androidx.media3", name = "media3-ui-compose", version.ref = "media3" }

# Images
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

# Storage
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp                 = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt                = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
apollo              = { id = "com.apollographql.apollo", version.ref = "apollo" }
```

Note: with Kotlin 2.0+, apply the **`kotlin-compose` plugin** for the compiler; do not pin `kotlinCompilerExtensionVersion`. Drop Retrofit/OkHttp-as-API/Room from the findings â€” you have **no REST API** (Apollo only) and **no relational caching need** in v1 (Apollo's normalized cache covers it). OkHttp still arrives transitively via Apollo/Coil.

---

## 5. Apollo + Paging 3 Data Approach

**Client setup (single Hilt-provided singleton):**

```kotlin
@Provides @Singleton
fun apolloClient(@ApplicationContext ctx: Context): ApolloClient =
  ApolloClient.Builder()
    .serverUrl(BuildConfig.VIDEOHUB_GRAPHQL_URL)
    .normalizedCache(
      MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
        .chain(SqlNormalizedCacheFactory(ctx, "bibeltv_apollo.db"))
    )
    .build()
```

No auth interceptor in v1 (no login). If VideoHub needs a static API key, add it via `.addHttpHeader(...)` or an `HttpInterceptor` â€” but **ignore the entire Keycloak/OAuth section from the findings**; it's not in scope for a no-login app.

**Cache keys:** declare `@typePolicy(keyFields: ["id"])` on the major VideoHub types in a local `extra.graphqls` so the normalized cache dedupes the same video appearing across many Home rows (it will appear in several). This is high-leverage given 20k+ videos and overlapping rails.

**Codegen:** `codegenModels = "operationBased"` (shared models, better for many overlapping queries), `generateDataBuilders = false`. Check the schema (`schema.graphqls`) into source control; download it via the introspection Gradle task, don't fetch at build time.

**Pagination (Prisma take/skip):** VideoHub uses offset pagination, so a `PagingSource<Int, T>` keyed on offset is correct (not Paging's `RemoteMediator`, which is for DB-backed). One **generic** paging source parameterized by the query, used by Browse/Category/Search:

```kotlin
class VideoHubPagingSource<D : Query.Data, T : Any>(
  private val apollo: ApolloClient,
  private val pageSize: Int = 24,
  private val query: (skip: Int, take: Int) -> Query<D>,
  private val extract: (D) -> List<T>,
) : PagingSource<Int, T>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
    val offset = params.key ?: 0
    return try {
      val resp = apollo.query(query(offset, params.loadSize)).execute()
      resp.exception?.let { return LoadResult.Error(it) }
      val data = resp.data ?: return LoadResult.Error(IllegalStateException(resp.errors?.joinToString { it.message }.orEmpty()))
      val items = extract(data)
      LoadResult.Page(
        data = items,
        prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
        nextKey = if (items.size < params.loadSize) null else offset + params.loadSize,
      )
    } catch (e: Exception) { LoadResult.Error(e) }
  }
  override fun getRefreshKey(state: PagingState<Int, T>) =
    state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey?.plus(pageSize) }
}
```

`Pager(PagingConfig(pageSize = 24, initialLoadSize = 48, prefetchDistance = 8, enablePlaceholders = false))`, `.flow.cachedIn(viewModelScope)`. **`enablePlaceholders = false`** for Compose grids (the findings contradicted themselves here â€” false is correct for `LazyVerticalGrid`).

**Home is NOT paged.** Home rows are bounded curated lists â€” fetch with plain `apollo.query(...).execute()` per row in parallel (`async`), map to domain, expose in `HomeUiState`. Paging 3 is only for the unbounded Browse/Category/Search grids.

**Error model:** wrap responses in a `Result`-like sealed type distinguishing fetch-error (`response.exception`), GraphQL-error (`response.errors`), and partial data. Repositories return domain models or typed errors; ViewModels map to `UiState.Error`.

---

## 6. Media3 Player Approach

- **Versions:** `media3-exoplayer` + `-hls` + `-dash` + `-session` + `-ui-compose` at **1.10.0**. Bibel TV streams are almost certainly HLS â€” include DASH defensively.
- **ExoPlayer lifecycle:** create in `PlayerViewModel` (or a Hilt `PlayerFactory`), **release in `onCleared()`** / `DisposableEffect(onDispose)`. Never leak. One player instance per Player screen.
- **MediaItem:** build from the playback URL resolved via GraphQL. Set explicit MIME (`APPLICATION_M3U8` / `APPLICATION_MPD`) since VideoHub URLs may lack extensions. **DRM:** only wire Widevine (`DrmConfiguration(C.WIDEVINE_UUID)` + license URI + `setMultiSession(true)`) **if** VideoHub returns a license URL â€” most Mediathek content is clear. Don't build DRM speculatively.
- **UI:** `media3-ui-compose` `PlayerSurface` + a **custom Compose control overlay** (the findings' `material3` sub-artifact components are not a guaranteed-stable API surface â€” build controls from the documented `PlayerSurface` + state holders, styled with your Material 3 theme). Controls: play/pause, seek bar with position/duration, Â±10s, captions toggle, quality/track selector, fullscreen, PiP, AirPlay-equivalent later. Auto-hide after 3â€“4s; tap to reveal.
- **Three playback modes** (`PlayableKind`): `VOD` (full controls + scrubbing), `LIVE` (live-edge button, no scrub past edge, "LIVE" indicator), `AUDIO` (collapse video surface to artwork; this is your Predigten-as-audio / audio content path).
- **Fullscreen:** lock landscape on enter, restore on exit (`DisposableEffect`).
- **PiP:** `android:supportsPictureInPicture="true"` + `android:resizeableActivity="true"` on the activity; `enterPictureInPictureMode` with 16:9 `Rational`; hide all chrome via `rememberIsInPipMode()`.
- **Background audio + lockscreen/notification:** add a `MediaSessionService` (from `media3-session`) so audio content (sermons, audios) keeps playing in background with media-notification controls. This is essential for the Audio content type â€” don't skip it.
- **Autoplay-next** for series episodes with a skippable countdown (a settings-toggle, stored in DataStore since there's no account).

---

## 7. Theming / Branding (adaptive colors around a recolored Bibel TV logo)

**Decision: brand-anchored Material 3, dynamic color OFF by default.** The findings repeatedly suggested wallpaper-based `dynamicColorScheme`. For a broadcaster, **do not let the user's wallpaper recolor the Bibel TV brand** â€” it dilutes brand identity and breaks the logo's relationship to the UI. Build a fixed, brand-seeded scheme. (You may offer dynamic color as an opt-in toggle in Settings, off by default.)

**Color system:**
1. Take the Bibel TV brand blue as the **seed**. Generate full light + dark tonal schemes via the **Material Theme Builder** (export the Compose `Color.kt` + `Theme.kt`). Verify all `primary`/`onPrimary` (and container) pairs hit **WCAG AA 4.5:1**.
2. Default the app to a **dark theme** (OLED-friendly, standard for video/Mediathek), with a working light theme. Respect `isSystemInDarkTheme()` plus a Settings override stored in DataStore.
3. `MediathekTheme` composable wraps `MaterialTheme(colorScheme, typography, shapes)`. Provide German-optimized typography (generous line height; titles wrap to 2 lines).

**Recolored logo â€” the load-bearing requirement:**
- Convert the Bibel TV logo to an Android **VectorDrawable** with the **wordmark and the star as separate `<path>` elements** (and named `<group>`s). Preserve `viewBox` and all `pathData` exactly â€” recolor only.
- For in-app placement (top app bar, splash, hero watermark), set fills to **theme attributes** (`?attr/colorPrimary` for wordmark, a chosen accent for the star) or, in Compose, tint via `ColorFilter`/`tint` from `MaterialTheme.colorScheme`. This makes the logo adapt automatically between light/dark and any future palette change â€” one logo asset, theme-driven recolor.
- Keep the **star as a distinct tinted path** so it can carry the accent color independently of the wordmark across themes. Verify the star stays distinguishable from the wordmark in both light and dark.

**Adaptive launcher icon:**
- `mipmap-anydpi-v26/ic_launcher.xml` adaptive icon: **foreground** = logo VectorDrawable centered inside the **66Ã—66dp safe zone** of the 108Ã—108dp canvas; **background** = solid brand color (or subtle brand gradient).
- Add a **`<monochrome>`** layer (single-color silhouette of the mark) for Android 13+ themed icons.
- Provide the legacy `ic_launcher_round`. Generate all densities via Android Studio's Image Asset Studio from the vector source.

**Splash:** use the `androidx.core:core-splashscreen` API (Android 12+ splash) with the brand background and the logo/monochrome icon â€” do not hand-roll a splash Activity.

---

## Build order (do this in sequence)

1. Project scaffold: catalog above, Hilt + KSP + Apollo + Compose plugins wire-up, `MediathekTheme`, recolored logo vector, adaptive icon, splash.
2. Apollo: introspect schema, define type policies, build `ApolloClient` + domain mappers, one smoke query.
3. Navigation scaffold: 4-tab bottom bar + rail adaptation, empty screens, type-safe routes.
4. Home (hero + rails + live strip) with parallel queries, skeletons, error/offline states.
5. Browse + Category + Search on the generic `VideoHubPagingSource`.
6. Detail screens (single vs. series with season/episode list).
7. Player: ExoPlayer + `PlayerSurface` + custom controls; VOD first, then LIVE, then AUDIO + `MediaSessionService`; fullscreen + PiP.
8. Settings (theme, language, autoplay) on DataStore.

**Key corrections vs. the raw research, in one place:** Apollo **4.4.3** (not 5.x â€” 5 is RC/Maven-only); **AGP 8.13 / compileSdk 36** (not AGP 9.0.1); **no Retrofit/OkHttp-as-API, no Room** (Apollo-only, normalized cache suffices); **no OAuth/Keycloak** (no login in v1); **dynamic color OFF by default** (protect the brand); **start single-module** package-by-feature (not the 12-module setup); **Paging only for unbounded grids**, Home rows are plain parallel queries; `enablePlaceholders = false` for Compose grids; add a **`MediaSessionService`** for background audio (the research omitted it but the Audio content type requires it).