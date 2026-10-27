# Changelog

## 1.3.0 — Combat, bridging, exploration, performance and a new menu

Existing settings, keybinds and module states carry over. The config moves to format version 4, and the old file is kept as `hollow.json.v3.bak`. On first start, your current setup is also saved as a profile named **Default**.

### Fair-play rules for this update
- **Automated actions only run where they are allowed.** This covers Kill Aura, Trigger Bot, Aim Assist, Auto Bridge, Safe Walk and Speed Bridge's automatic placement.
  - They run in single-player.
  - On a server, they run only if you list it under *Server Rules → Servers Permitting Automation*. A server under *Restricted Servers* always wins.
  - Anywhere else the module shows **BLOCKED** and does nothing.
- **Everything goes through the game's normal actions.** Attacks, block placements and camera turns use the same paths as your own input.
  - No packets are crafted, delayed or altered, and no server confirmation is faked.
  - Vanilla reach and attack cooldown always apply.
  - Nothing hides the client from anti-cheat.
- **Detection-style features stay off on restricted servers.** This covers the World Scanner, ESP, tracers and Freecam. The scanner never loads chunks.
- **Entity Visibility reports only what your client can observe.** It never reveals hidden players or their positions.

### Added

**Combat**
- **Kill Aura**:
  - Attacks targets you choose (players, hostile, neutral or passive mobs; teammates can be skipped) with ordinary attacks.
  - Range is capped at your real reach.
  - It waits for a configurable attack charge and can turn your camera smoothly toward the target.
  - Also: a field-of-view limit, sword/axe-only mode, a pause while using items, a target outline and a HUD indicator.
- **Trigger Bot**: attacks when your crosshair rests on an eligible target. It has a reaction delay, a charge threshold and the same target filters.
- **Reach Display** (visual only):
  - Shows the distance to what you aim at against your real entity and block reach.
  - Optional ground rings at both reaches.
- **Knockback Tester**:
  - Measures the horizontal and vertical knockback speed and the distance you travel after a hit.
  - In single-player only, it can also set a knockback-resistance value and deal a test hit.
  - No packet changes.

**Aim Assist**
- Smooth assistance that only adds to your own aiming:
  - It is frame-rate independent and has a speed limit, so it never snaps.
  - By default it only acts while you move the mouse.
- **Presets** Subtle, Smooth and Standard; editing a tuned value switches to Custom.
- Controls:
  - Strength, smoothness and speed limit.
  - Optional matching to your mouse sensitivity.
  - Separate horizontal and vertical multipliers.
  - Field of view, range and aim point (closest point, body or head).
- **Target filters and priority** (crosshair, nearest, lowest health), plus a line-of-sight check.
- **Activation**: always, while attacking, hold a key or toggle a key.
- **Display**: a target marker and a status HUD (Assisting / No target / Waiting / Blocked here).
- **Practice mode**:
  - Spawns static and wandering practice targets in your single-player world.
  - Counts your hits and accuracy.
- It stops whenever a menu, chat or text field is open, or the window is unfocused.

**Bridging & Movement**
- **Auto Bridge**:
  - Places a block under you as you walk off an edge, against a real neighbouring face.
  - It never places into air with nothing to click, and never mid-jump or while flying.
  - Also: placement delay, place-ahead, hotbar block selection, optional sneak at edges, a placement preview and a block counter HUD.
  - It stops by itself and alerts you when you run out of blocks.
- **Speed Bridge**, timing help for crouch-and-place bridging:
  - **Safe** holds sneak at edges.
  - **Rhythm** gives a visual and audio cue when it is time to place.
  - **Practice** adds statistics: blocks per second, longest bridge and falls.
  - Also: edge indicator, placement preview, cue lead and optional automatic placement.
  - Hold or toggle it with its keybind's *Key Mode*.
- **Safe Walk**: you never walk off an edge by accident, without the slowdown of sneaking.

**Render**
- **Freecam**:
  - The camera flies freely while your player stays exactly where it is.
  - Speed (mouse wheel), acceleration, sprint boost and rotation smoothing.
  - A maximum distance from the player, a return-to-player key and a position HUD.
  - Attack, use and pick-block are ignored while detached, because they would act from your player, not the camera.

**Exploration**
- **Waypoints & Bases**:
  - Per-world waypoints with name, category (Base, Point of Interest, Scouted, Death, Other), note and timestamp.
  - Beacons with names and distances, and a compass strip HUD.
  - Automatic death points and an optional scouting trail.
  - A quick-add key.
  - An add/edit form and a list with show/hide, edit and delete.
  - Clipboard export/import. Imports are validated and duplicates skipped.
  - Stored in `config/hollow/waypoints/`.
- **World Scanner**: Block Finder, renamed and moved to Exploration. It still only reads chunks your client already has.
- **Entity Visibility**:
  - Logs tab-list joins and leaves, game-mode changes and invisibility-flag changes.
  - Where player detection is permitted, it also logs nearby player entities disappearing while still listed.
  - Events show in a log and optionally as alerts.

**Performance**
- **Presets** Potato, Low, Balanced, High Quality and Custom. Use *Performance → Preset* and then *Apply*.
  - They set sky detail, menu and loading effects, motion blur, swing trails and FPS Boost.
  - They never touch other modules' on/off state, keybinds or unrelated settings.
- **Adaptive Quality** (optional):
  - When FPS stays below your target, Holo's own decorative effects step down one level at a time, with a cooldown, and back up when there is headroom.
  - Your settings themselves are not changed.
- **FPS Boost**:
  - Skips drawing mobs, items and other entities beyond chosen distances. Named entities are optional. They still exist and tick; only drawing is skipped.
  - Caps the number of particles.
  - Limits the frame rate while the window is in the background.
- **Performance Monitor HUD**:
  - FPS, average FPS, average and worst frame time, memory, entities and particles, and the active preset and adaptive level.
  - Warnings for low FPS, stutter and high memory.

**HUD**
- **HUD editor** (*HUD* button in the menu, or *HUD Layout → Open Editor*):
  - Drag every Holo HUD element; edges and centres snap to gold guides.
  - Arrow keys nudge, right-click resets one element, and *Reset All* restores the layout.
  - Positions are stored relative to the screen, so they survive resolution changes.

**Framework**
- **Keybinds**:
  - Any module can be bound to a key, a mouse button (3–8) or a combination with Ctrl, Shift or Alt.
  - Each bind has a *Key Mode*: Toggle, or Hold (on only while held).
  - Conflicts with other Holo binds and Minecraft controls are shown.
  - Keys never fire while a menu, chat or text field is open.
- **Profiles**: save the whole setup under a name, then load, rename, export to the clipboard, import (validated, max 512 KB), delete, or reset everything. Older profiles are migrated when loaded.
- **Error isolation**: a module that throws during its tick is turned off, and a HOLO ALERT tells you, instead of the error repeating every tick.

### Replaced
- **The click GUI is new**: a central window instead of floating category windows. It has:
  - A header with search, profile chip, HUD editor and close button.
  - A collapsible sidebar with eleven categories (Combat, Aim Assist, Bridging, Movement, Render, World, Exploration, Player, Performance, HUD, Settings), each with an icon and an on-count.
  - Keybinds and Profiles pages.
  - **Module cards** with switch, status (ON / OFF / BLOCKED / UNAVAILABLE / SETTINGS), keybind and description.
  - A **settings panel** that slides in, with a reset arrow on every changed setting, a reset for the whole module (it asks you to confirm), the module's live preview, and the reason it is blocked or unavailable.
  - Full keyboard use: `Ctrl+F` search, `Tab`/arrows to move, `Enter` to open, `Space` to toggle, `←`/`→` to adjust, `Delete` to reset a setting, `Page Up`/`Page Down` to switch category, and `Esc` to go back.
  - It works down to Minecraft's smallest GUI size (320×240 GUI pixels), where the sidebar becomes icons. Settings are saved when it closes.
- **Interface** settings:
  - Themes Holo Violet, Pharaoh Gold, Crimson, Frost, Emerald or Custom, each with an accent and a gold *detail* color.
  - *Reduced Motion*, which turns off sliding, scaling and fading.
  - Compact sidebar.
  - The old window-layout and panel-opacity settings are gone with the floating windows.

### Changed
- Modules are regrouped into the eleven categories above.
- **Rounded shapes are much cheaper to draw.**
  - Corners now come from small cached anti-aliased textures, so a shape takes about seven draws instead of one per pixel row of its corners.
  - This speeds up the menu, the HUD and alerts.
- Alerts can be placed anywhere with the HUD editor (*Position: Custom*).
- Performance: the old *Quality / Balanced / Low Performance* profiles became presets (QUALITY maps to High Quality).

### AMSS
The request asked for the existing "AMSS" utility to be upgraded. Nothing named AMSS exists in this project:
- The code, settings, resources and the whole git history were searched for it.
- In line with the instruction not to guess its meaning, nothing was renamed, duplicated or claimed as improved.
- If AMSS is a feature from another client or a planned idea, describe what it should do and it can be added.

### Tests
- **Unit tests:** 97 pass (61 before). New tests cover:
  - Aim maths.
  - Keybind storage.
  - HUD snapping.
  - Bridge placement planning.
  - Frame statistics and adaptive quality.
  - The waypoint store (persistence, import, sanitising, damaged files).
  - Profiles: save/load, full-setup semantics, rename/export/import/delete, file-name collisions, validation, migration of old profiles.
  - The automation policy.
  - Config migration v4.
  - Interface themes.
- **The in-game test** (`runClientGameTest`) now also checks the following:
  - Aim Assist turns the view toward a practice target.
  - Kill Aura damages a zombie in reach.
  - Trigger Bot attacks the zombie under the crosshair.
  - Auto Bridge places a real block at an edge.
  - Freecam moves the camera while the player stays put.
  - A waypoint is added.
  - The Knockback Tester measures a hit.
  - A profile round-trip restores the theme.
  - Keyboard navigation in the menu works.
  - Typing in the search field toggles nothing.
- It also captures screenshots of every menu page, every module's settings, the HUD and the HUD editor.

### Files
Java paths are relative to `src/main/java/dev/hollow/`.

**Added (81)**
`src/gametest/java/dev/hollow/gametest/Benchmarks.java`, `src/gametest/java/dev/hollow/gametest/NewFeatures.java`, `ModuleRuntime.java`, `bridge/BridgeAssist.java`, `bridge/BridgeInput.java`, `bridge/Bridging.java`, `camera/Freecam.java`, `combat/AimAssist.java`, `combat/Attacks.java`, `combat/KillAura.java`, `combat/KnockbackTester.java`, `combat/ReachMeter.java`, `combat/TargetSettings.java`, `combat/Targeting.java`, `core/aim/AimMath.java`, `core/bridge/BridgeMath.java`, `core/config/ProfileStore.java`, `core/hud/HudSnap.java`, `core/input/BindMode.java`, `core/input/Keybind.java`, `core/module/Tickable.java`, `core/perf/AdaptiveQuality.java`, `core/perf/FrameStats.java`, `core/waypoints/Waypoint.java`, `core/waypoints/WaypointStore.java`, `exploration/VisibilityLog.java`, `exploration/Waypoints.java`, `gui/Confirm.java`, `gui/ConfirmRow.java`, `gui/HoloScreen.java`, `gui/Icons.java`, `gui/InfoRow.java`, `gui/KeybindsPage.java`, `gui/Pill.java`, `gui/ProfilesPage.java`, `gui/RowList.java`, `gui/SettingsPanel.java`, `gui/VisibilityLogRow.java`, `gui/WaypointListRow.java`, `hud/AlertsHudElement.java`, `hud/HudEditorScreen.java`, `hud/HudElement.java`, `hud/HudElements.java`, `hud/HudStyle.java`, `hud/ModuleHuds.java`, `input/KeyInput.java`, `input/KeybindConflicts.java`, `mixin/CameraMixin.java`, `mixin/EntityRenderDispatcherMixin.java`, `mixin/FramerateLimitTrackerMixin.java`, `mixin/KeyboardInputMixin.java`, `mixin/MouseHandlerMixin.java`, `mixin/ParticleEngineMixin.java`, `mixin/PlayerMixin.java`, `modules/AimAssistModule.java`, `modules/AutoBridgeModule.java`, `modules/EntityVisibilityModule.java`, `modules/FpsBoostModule.java`, `modules/FreecamModule.java`, `modules/HudLayoutModule.java`, `modules/KillAuraModule.java`, `modules/KnockbackTesterModule.java`, `modules/PerformanceMonitorModule.java`, `modules/ReachDisplayModule.java`, `modules/SafeWalkModule.java`, `modules/SpeedBridgeModule.java`, `modules/TriggerBotModule.java`, `modules/WaypointsModule.java`, `perf/FpsBoost.java`, `perf/PerformanceState.java`, `policy/Automation.java`, `practice/LocalServer.java`, `render/Corners.java`, `render/OverlayProvider.java`, `src/test/java/dev/hollow/core/AimMathTest.java`, `src/test/java/dev/hollow/core/BridgeMathTest.java`, `src/test/java/dev/hollow/core/HudSnapTest.java`, `src/test/java/dev/hollow/core/KeybindTest.java`, `src/test/java/dev/hollow/core/PerformanceMathTest.java`, `src/test/java/dev/hollow/core/ProfileStoreTest.java`, `src/test/java/dev/hollow/core/WaypointStoreTest.java`

**Modified (56)**
`CHANGELOG.md`, `README.md`, `gradle.properties`, `src/gametest/java/dev/hollow/gametest/HollowClientGameTest.java`, `Hollow.java`, `HollowClient.java`, `ModuleKeybinds.java`, `alerts/AlertSource.java`, `blur/MotionBlurRenderer.java`, `core/config/ConfigManager.java`, `core/config/ConfigMigrations.java`, `core/module/Category.java`, `core/module/Module.java`, `core/setting/KeySetting.java`, `core/setting/TextSetting.java`, `detection/BlockScanner.java`, `gui/ActionBarRow.java`, `gui/ActionRow.java`, `gui/BoolRow.java`, `gui/ColorRow.java`, `gui/EnumRow.java`, `gui/HeaderRow.java`, `gui/KeyRow.java`, `gui/NumberRow.java`, `gui/PreviewRow.java`, `gui/Row.java`, `gui/TextRow.java`, `gui/Toggle.java`, `gui/UiContext.java`, `hands/HandRenderHooks.java`, `menu/HoloTitleScreen.java`, `menu/MenuScene.java`, `mixin/GameRendererMixin.java`, `modules/BlockEspModule.java`, `modules/BlockFinderModule.java`, `modules/CharacterStudioModule.java`, `modules/EntityTracersModule.java`, `modules/HollowAlertsModule.java`, `modules/HollowHandsModule.java`, `modules/InterfaceModule.java`, `modules/LoadingScreenModule.java`, `modules/MainMenuModule.java`, `modules/MotionBlurModule.java`, `modules/PerformanceModule.java`, `modules/ServerRulesModule.java`, `modules/SkyStudioModule.java`, `modules/ThemePacksModule.java`, `policy/ServerPolicy.java`, `render/Draw2D.java`, `render/HudOverlay.java`, `render/Theme.java`, `render/WorldOverlayRenderer.java`, `sky/HoloSkyRenderer.java`, `resources/hollow.mixins.json`, `src/test/java/dev/hollow/core/ConfigManagerTest.java`, `src/test/java/dev/hollow/policy/ServerPolicyTest.java`

**Removed: replaced by the new menu (3)**
`gui/CategoryWindow.java`, `gui/HollowScreen.java`, `gui/ModuleWindow.java`

### Performance results
Measured by `runClientGameTest` on a CPU-only machine with software rendering (Mesa llvmpipe), at 960×540. Each value is the best of three rounds of 30 back-to-back frames, in **milliseconds per frame (lower is better)**.

- **1.2.0** was run with the 1.2.0 code before this update.
- **1.3.0** was run twice; both runs are shown, to give an idea of the run-to-run noise.
- **Potato** is the same run with *Performance → Potato → Apply*.

| Scene | 1.2.0 | 1.3.0 default | 1.3.0 Potato |
|---|---|---|---|
| Standing still | 19.9 | 21.9 / 22.6 | 18.7 / 18.8 |
| 6000 flame particles | 36.0 | 39.7 / 41.0 | 18.0 / 20.1 |
| Holo menu open | 65.6 (old menu) | 57.4 / 61.4 (new menu) | 24.9 / 25.9 |
| 150 mobs + 200 items, tracers | 38.1 | 39.0 / 37.9 | 27.8 / 28.3 |
| Sky, outfit, blur, tracers + crowd | 33.4 | 34.9 / 34.9 | 27.4 / 30.4 |

**What the numbers say:**
- **The new menu** is cheaper than the old one, while showing more.
  - It owes this to the new corner textures.
  - Before that change, the same menu measured 126 ms.
  - In a quieter scene, an empty world went from 13.0 ms with no menu to 22.1 ms with the menu on All Modules.
- **With default settings, 1.3.0 is about 1–4 ms per frame slower than 1.2.0** in these software-rendered scenes.
  - About 0.9 ms of that came from the new waypoint compass HUD. It now only appears once the dimension has a visible waypoint, so a fresh install doesn't pay it. The two 1.3.0 runs above were taken before that change.
  - The rest is within run-to-run noise or comes from the new per-frame hooks, which are near-free while their modules are off.
  - In a simple scene, defaults cost 1.4 ms more than all modules off.
- **The Potato preset** roughly halves the particle-heavy frame and saves 20–30 % in crowds.
- **These are software-rendering results.** No FPS gain on a real GPU is promised.

## 1.2.0 — 3D characters

### Added
- **Five new characters:**
  - Grim Reaper: skull mask, peaked hood, ragged robe and a scythe on the back.
  - Shadow Ninja: eye-slit mask, headband tails, scarf and a katana.
  - Frost Witch: tall bent hat, ice-crystal collar and shoulders, starry cape.
  - Infernal Demon: curved horns, bat wings, tail and ember-cracked armor.
  - Star Voyager: gold-rimmed helmet visor, antenna, control panel and life-support pack.
- **Characters in 3D everywhere.** You, in your own skin and outfit, are drawn as a live 3D model with idle breathing:
  - The main menu figure turns gently and its head follows the mouse.
  - Character Studio shows a large figure you can drag to turn, next to a 3D gallery of every outfit. Click one to wear it.
  - Theme Pack cards and the reload loading screen also use the 3D model.
- **Five new theme packs:** Reaper, Shadow, Frost, Infernal and Voyager collections, each with a matching sky. The menu uses that sky as its background.
- **New sky preset, Hellfire Rift:** a black sun ringed in fire above a smouldering ember rift.

### Changed
- Every outfit, including Pharaoh, Wraith and Hollow Knight, is now generated by `tools/characters.py` with hand-designed face textures (eyes, trims, blades, visors) instead of repeating patterns.
- The main menu's *Featured Character* is now *Hidden*, *Your Character Studio Look* or *A Specific Character* (any of the eight). Existing choices are migrated automatically (config version 3).
- The flat 2D character artwork has been removed. All character views are 3D.

## 1.1.1 — Black-screen fix

### Fixed
- **Black screen at start-up on many graphics cards.**
  - Cause: the Sky Studio shader defined a helper function named `noise3`, which is also a built-in GLSL function. Strict drivers (most NVIDIA, AMD and Intel drivers) refuse to compile it.
  - Effect: because the shader was registered as a required pipeline, the failure aborted Minecraft's resource load. Minecraft never opened the title screen and the window stayed black.
  - Fix: the function is renamed.
- **Holo's shaders can no longer block start-up.**
  - They are no longer registered as required pipelines. They compile on first use instead.
  - If a driver rejects one, only the effect that needs it switches off, and the log explains why. Sky Studio and Motion Blur show as unavailable in the menu; the world overlays skip boxes and lines but keep their labels.
- Sky shader made portable for strict drivers:
  - `smoothstep` edges are always in ascending order.
  - No `pow` on negative numbers.
  - No local variable shadows a function name.
  - Guards against `atan(0, 0)`.
  - A final check so undefined values can never reach the screen.

### Added
- A unit test that rejects shaders redefining built-in GLSL functions or using reversed `smoothstep` edges.
- A `prodClient` Gradle task that runs the release jar exactly as players get it.

## 1.1.0 — Holo: Custom Skies, Main Menu & Loading Screen

The client is now branded **Holo**. Mod id, package names, config folder (`config/hollow/`) and every existing setting are unchanged, so existing configs, keybinds and window layouts carry over as they are.

### Added

**Holo Sky Studio** (*Studio → Holo Sky Studio*, off by default)
- A procedural overworld sky drawn by one full-screen shader pass. Its layers:
  - Gradient and horizon glow.
  - Optional resource-pack texture.
  - Nebula band.
  - Two star layers.
  - Planets, which can have rings.
  - Sun and moon styles: disc, halo, crescent, eclipse or hidden.
  - An ornamental celestial ring.
  - Distant silhouettes: pyramids, obelisks or spires.
- Seven original presets:
  - Anubis Eclipse
  - Void Realm
  - Pharaoh's Night
  - Astral Horizon
  - Crimson Dusk
  - Frozen Cosmos
  - Vanilla Enhanced
- Preset library:
  - Search, with a color swatch per preset.
  - Save, Save As, Rename, Duplicate, Delete and Revert.
  - Export copies the sky to the clipboard and writes it to `config/hollow/skies/exports/`.
  - Import reads a sky from the clipboard. It is limited to 64 KB, and the format and values are validated.
- Simple mode shows the essential controls. *Advanced Controls* shows every parameter.
- Time mode can follow the day cycle (blending into vanilla or custom day colors) or keep the theme's look at all hours. The sun and moon can follow the game or be pinned for a composed sky.
- Cloud handling: vanilla, tinted or hidden. Optional fog tint blends terrain into the sky's horizon color.
- *Restore Vanilla Sky* turns the studio off and keeps all presets.

**Character Studio** (*Studio → Character Studio*)
- Three original cosmetic sets drawn on your own player:
  - Pharaoh: nemes, uraeus and collar.
  - Wraith: hood and cloak.
  - Hollow Knight: helm, pauldrons and cape.
- Cosmetics follow the player's animation and show in third person and the inventory.
- They are client-side only: nobody else sees them and nothing is sent to servers.
- Live 3D preview while in a world; artwork otherwise.

**Theme Packs** (*Studio → Theme Packs*)
- Pharaoh, Wraith and Hollow Knight collections. Each one bundles a sky, character set, interface accent, Hollow Hands preset with swing-trail color, and main-menu scene.
- *Apply Selected Components* changes only what is ticked. *Apply Entire Collection* changes everything. Applying a sky never touches the character or interface, and the reverse holds too.

**Main Menu** (*Client → Main Menu*, on by default)
- A new title screen:
  - Animated scenes: Temple of Anubis, Pyramids at Night, Void Realm, the current Sky Studio sky, or a custom image.
  - Featured character art and drifting particles.
  - Glowing HOLO logo with a turning celestial ring, and fade-in.
- Includes everything vanilla offers:
  - Singleplayer, Multiplayer (with vanilla's disabled reasons) and Realms.
  - Options, Language, Accessibility, Quit, and Credits & Attribution.
  - Version information.
- Plus shortcuts to Character Studio, Sky Studio and Client Settings.
- Settings: scene, custom image, featured character, accent, lighting, animation intensity (0 stops all motion), particles and decorative effects.
- Custom backgrounds are PNGs in `config/hollow/backgrounds/`:
  - They are decoded off the render thread.
  - The limit is 16–4096 px per side. Missing or invalid files fall back to the temple scene, and the menu preview says why.
- Other out-of-world menus (world list, options, multiplayer, world loading) use the same backdrop instead of the panorama.
- *Vanilla Menu* shows Minecraft's own title screen. Turning the module off keeps it permanently. Demo mode always uses vanilla's menu.

**Loading Screen** (*Client → Loading Screen*, on by default)
- Restyles Minecraft's own loading overlay:
  - Dark background.
  - Letter-by-letter logo reveal.
  - A sundial ring that fills with the real reload progress, plus a circling comet trail.
  - Particles and a status line with the percentage.
- It draws with plain fills, so it works during the very first start-up before any font or texture exists.
- Character artwork appears on resource reloads.
- Options: animation speed, reduced motion, particles, ring, motion trail, character artwork, and *Skip Fade-Out*. *Skip Fade-Out* hands over to the menu as soon as loading finishes, instead of vanilla's two-second hold and fade.
- Nothing waits on an animation: the overlay's own completion logic is untouched.

**Performance** (*Client → Performance*)
- Quality, Balanced and Low Performance profiles. They scale:
  - Sky detail.
  - Menu and loading particles.
  - Decorative effects.
  - Motion blur and swing trails.

### Changed
- Visible branding: the menu wordmark, alert prefix (`HOLO ALERT — …`), mod name and keybind category now read Holo.
- Config format version 2, with an automatic migration. The old file is kept as `hollow.json.v1.bak`.
- New settings widgets: text fields, button bars and the sky library. All new pages have live preview cards.

### Performance notes
- The sky is one full-screen fragment pass, replacing vanilla's sky, sun, moon and star draws.
  - Uniforms are uploaded through a ring buffer and repacked only when a value changes.
  - *Detail: Low* skips the nebula noise and the second star layer.
- Menu and loading animations allocate nothing per frame: particles, stars and status strings are preallocated. They only advance while a menu is on screen.
- Menu textures (glow sprite, custom background) are freed when you join a world. Sky GPU buffers are freed when Sky Studio is turned off.
- Measured results are listed in the README under *Performance*.

### Compatibility
- New mixins:
  - `LoadingOverlayMixin`
  - `MinecraftMixin` (`setScreen`)
  - `ScreenMixin` (`renderPanorama`)
  - `SkyRendererMixin`
  - `FogRendererMixin`
  - `CloudRendererMixin`
- All of them are optional (`defaultRequire: 0`), and each passes straight through when its module is off. See the README's mixin table.
- Sky Studio stands aside while an Iris shader pack is active. It only replaces the overworld sky; the Nether, the End and modded dimensions keep their own skies.
