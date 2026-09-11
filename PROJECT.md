# Project: JetMelo-Next Performance, Overdraw & Animation Continuity

## Architecture
- **Rendering Pipeline**: Scaffold blur lifecycle tied to player expansion progress; eliminate duplicate offscreen render nodes in nested composables.
- **Navigation & Shared Elements**: Predictive transitions between list screens and detail screens via `SharedTransitionLayout` with immediate skeleton pre-mounting using `initialArtworkUri`.
- **Dock Decoupling**: Separation of root Scaffold constraints from bottom dock animated visibility; full-screen stable constraints for `NavGraph` and `LazyColumn`.
- **Player Motion & State**: Continuous micro-interpolated timeline for playback slider; isolated coroutine position polling for lyrics without triggering Composable root recomposition; unified predictive back gestures.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Scaffold Dynamic Blur Bypass | Bypass Scaffold offscreen blur in fully expanded and settled player states | M1 | ORIGINAL_REQUEST §R1.1 |
| 2 | Eliminate Nested ImmersiveBackground | Remove duplicate background and blur in Lyric and PlayerQueue | M1 | ORIGINAL_REQUEST §R1.2 |
| 3 | Native Motion Duration Scale | Remove manual animator duration scale multiplication in NavGraph and MainScreen | M1 | ORIGINAL_REQUEST §R1.3 |
| 4 | ItemThumbnail ImageRequest Cache | Cache ImageRequest instances using remember in ItemThumbnail and item lists | M1 | ORIGINAL_REQUEST §R1.4 |
| 5 | Initial Artwork Skeleton Pre-mounting | Pre-mount hero cover container with initialArtworkUri & sharedElement in PlaylistScreen/AlbumScreen | M2 | ORIGINAL_REQUEST §R2.1 |
| 6 | Remove hasCache Gating | Remove artificial hasCache gates from HomeScreen, UserPlaylistScreen, ListScreen, ExploreScreen | M2 | ORIGINAL_REQUEST §R2.1 |
| 7 | Dock Constraint Decoupling | Move NavigationBar AnimatedVisibility out of Scaffold.bottomBar to overlay layer, giving NavGraph stable constraints | M2 | ORIGINAL_REQUEST §R2.2 |
| 8 | Fluid Player Transformation | Re-architect miniAlpha and fullControlsAlpha timeline with dual crossover, eliminating blank card slide | M3 | ORIGINAL_REQUEST §R3.1 |
| 9 | Progress Slider Micro-Easing | Continuous 120fps progress interpolation with Animatable, SeekGuardLock, and isolated 1Hz time text | M3 | ORIGINAL_REQUEST §R3.2 |
| 10 | Lyric 10Hz Recomposition Elimination | Eliminate Compose State position in Lyric root, track position purely in coroutines, center viewport alignment | M3 | ORIGINAL_REQUEST §R3.3 |
| 11 | Unified Predictive BackHandler | Consolidate BackHandler into PlayerTransform with Android 14+ PredictiveBackHandler support | M3 | ORIGINAL_REQUEST §R3.4 |
| 12 | Full Build & Regression Testing | Compile with assembleDebug and verify zero regression across all screens | M4 | ORIGINAL_REQUEST §Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Rendering Pipeline & Overdraw Optimization | R1.1, R1.2, R1.3, R1.4 | none | DONE (commit fbf6597, tests d5537f0) |
| 2 | M2: Seamless Shared Element Transitions & Dock Decoupling | R2.1, R2.2 | M1 | DONE (commit 4fcb032, tests 8026c21) |
| 3 | M3: Player Continuity & Micro-Interpolation | R3.1, R3.2, R3.3, R3.4 | M1, M2 | DONE |
| 4 | M4: Final Integration & E2E Validation | Full build, regression check, acceptance criteria | M1, M2, M3 | PLANNED |

## Interface Contracts

### MainScreen ↔ PlayerTransform (Blur & Bottom Dock)
- `transitionProgress: Float`: Range 0.0f..1.0f representing player expansion progress.
- `Modifier.blur`: Enabled only when `transitionProgress > 0.05f && transitionProgress < 1f` on Android 12+ (SDK >= 31).
- `dockedBottomPadding: Dp`: Animated bottom offset for floating components (`PlayerTransform` and `BottomFogOverlay`).
- `tabBottomContentPadding: Dp` & `subpageBottomContentPadding: Dp`: Stable, non-animated padding values passed to `NavGraph` to isolate `LazyColumn` from bottom dock animation frames.

### PlayerTransform ↔ Lyric & PlayerQueue (Background Layering)
- `Lyric(..., showBackground: Boolean = false)`: Defaults to true for standalone use, set to false inside `PlayerTransform` to prevent nested `ImmersiveBackground`.
- `PlayerQueue(..., showBackground: Boolean = false)`: Defaults to true for standalone use, set to false inside `PlayerTransform` to prevent nested `ImmersiveBackground`.

### NavGraph ↔ PlaylistScreen & AlbumScreen (Shared Transition)
- `PlaylistScreen(..., initialArtworkUri: String? = null, playlistId: Long? = null, enableSharedTransition: Boolean = true)`
- `AlbumScreen(..., initialArtworkUri: String? = null, albumId: Long? = null, enableSharedTransition: Boolean = true)`
- Destination screens pre-mount cover skeleton with `Modifier.sharedElement(rememberSharedContentState(key = "cover_${id}"))` even while loading (`detail == null`).

### PlayerProgressSlider & Lyric (Progress Timing & Center Viewport)
- `PlayerProgressSlider`: Micro-interpolated via `Animatable` targeting `position + 100ms` with LinearEasing; seek events guarded by `SeekGuardLock`. Time labels decoupled to 1Hz derived state.
- `Lyric`: Viewport center alignment scroll delta:
  $$\Delta = \left(\text{item.offset} + \frac{\text{item.size}}{2}\right) - \frac{\text{viewportHeight}}{2}$$
  Top/bottom Spacers dynamically sized to `(viewportHeight / 2) - 30.dp` to allow all lines (including line 0) to align to center.

## Code Layout
- `app/src/main/java/com/rcmiku/music/ui/screen/MainScreen.kt` — Scaffold, bottom bar overlay, blur bypass, system animation scale.
- `app/src/main/java/com/rcmiku/music/ui/screen/PlayerTransform.kt` — Mini/Full transition alpha curves, unified PredictiveBackHandler, background host.
- `app/src/main/java/com/rcmiku/music/ui/components/Player.kt` — Full player layout, PlayerProgressSlider micro-easing.
- `app/src/main/java/com/rcmiku/music/ui/components/Lyric.kt` — Lyric coroutine-only position tracking, center alignment, showBackground.
- `app/src/main/java/com/rcmiku/music/ui/components/PlayerQueue.kt` — PlayerQueue showBackground, item thumbnail caching.
- `app/src/main/java/com/rcmiku/music/ui/components/Item.kt` — ItemThumbnail remember ImageRequest.
- `app/src/main/java/com/rcmiku/music/ui/navigation/NavGraph.kt` — System animation scale cleanup, route parameter propagation (`initialArtworkUri`, IDs).
- `app/src/main/java/com/rcmiku/music/ui/navigation/Screen.kt` — Navigation destinations with required arguments.
- `app/src/main/java/com/rcmiku/music/ui/screen/PlaylistScreen.kt` — Pre-mount skeleton cover with sharedElement, ImageRequest remember.
- `app/src/main/java/com/rcmiku/music/ui/screen/AlbumScreen.kt` — Pre-mount skeleton cover with sharedElement.
- `app/src/main/java/com/rcmiku/music/ui/screen/HomeScreen.kt` — Remove hasCache gating.
- `app/src/main/java/com/rcmiku/music/ui/screen/UserPlaylistScreen.kt` — Remove hasCache gating.
- `app/src/main/java/com/rcmiku/music/ui/screen/ListScreen.kt` — Remove hasCache gating.
- `app/src/main/java/com/rcmiku/music/ui/screen/ExploreScreen.kt` — Remove hasCache gating.
