# Holo

A dark-fantasy Fabric client for **Minecraft 1.21.11** (formerly *Hollow*). It includes:

- Practice-friendly combat and aim tools, bridging help and a free camera.
- Waypoints and bases, a world scanner, and visibility diagnostics.
- Performance presets, FPS Boost, adaptive quality and a performance monitor.
- Restrained motion blur and fluid first-person hand animations.
- Configurable block detection, entity tracers and a unified alert system.
- Original custom skies, client-side 3D character outfits and coordinated theme packs.
- An animated Egyptian-themed main menu and loading screen.

Everything lives in one dark violet-and-gold menu with a category sidebar, module cards, keybinds, profiles and a HUD editor.

Open the menu with **Right Shift** (rebind under *Controls → Holo*), or use *Client Settings* on the title screen.

See [CHANGELOG.md](CHANGELOG.md) for what changed in each version.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.17.3 or newer |
| Fabric API | 0.141.0+1.21.11 or newer |
| Java | 21 |

Install: build the jar (see below) or take `build/libs/hollow-<version>.jar`, then drop it with Fabric API into `.minecraft/mods`.

## Features

Modules are grouped by sidebar category. *Automated* means the module acts for you; those only run where automation is allowed (see [Fair-play boundaries](#fair-play-boundaries)).

### Combat

| Module | What it does |
|---|---|
| **Kill Aura** *(automated)* | Attacks eligible targets with ordinary attacks. Range is capped at your real reach and it waits for your attack charge. It can turn your camera smoothly to the target. Also: field of view, target filters and priority, sword/axe-only mode, a pause while using items, a target outline and a status HUD. |
| **Trigger Bot** *(automated)* | Attacks when your crosshair rests on an eligible target, after a configurable reaction delay and attack charge. |
| **Reach Display** | Visual only. Shows the distance to what you aim at against your real entity and block reach, with optional range rings on the ground. |
| **Knockback Tester** | Measures the knockback you take (speed and distance). In single-player it can also set your knockback resistance and give you a test hit. No packet changes. |

### Aim Assist

| Module | What it does |
|---|---|
| **Aim Assist** *(automated)* | Smooth assistance that adds to your own aiming. It is frame-rate independent with a speed limit, so it never snaps, and by default it only acts while you move the mouse.<br>• **Presets:** Subtle, Smooth, Standard and Custom.<br>• **Tuning:** strength, smoothness, speed limit, sensitivity matching, horizontal and vertical multipliers, field of view, range, aim point (closest, body, head), target filters and priority.<br>• **Activation:** always, while attacking, hold key or toggle key.<br>• **Display:** a target marker and a status HUD.<br>• **Practice mode** (single-player): spawns practice targets and counts hits and accuracy.<br>• It stops in menus, chat and text fields, and when the window is unfocused. |

### Bridging

| Module | What it does |
|---|---|
| **Auto Bridge** *(automated)* | Places blocks under you as you walk off an edge, always against a real neighbouring face, and never mid-jump or while flying. It has a placement delay, place-ahead, hotbar selection, sneak at edges, a preview and a block counter. It stops by itself when you run out of blocks. |
| **Speed Bridge** | Crouch-and-place timing help.<br>• **Safe:** holds sneak at edges.<br>• **Rhythm:** a visual and audio cue tells you when to place.<br>• **Practice:** adds statistics (blocks per second, longest bridge, falls).<br>Also: edge indicator, preview and optional automatic placement *(automated)*. |

### Movement

| Module | What it does |
|---|---|
| **Safe Walk** *(automated)* | You never walk off an edge by accident, without sneaking's slowdown. |

### Render

| Module | What it does |
|---|---|
| **Motion Blur** | Frame-accumulation blur that only appears during fast camera turns, plus soft trails on Holo's panels and toasts while they move. The pass runs *before* the HUD, so text, crosshair, chat and menus are never blurred. Pauses while an Iris shader pack is active. |
| **Entity Tracers** | Lines, hitbox outlines and labels (name, health, distance) for hostile, passive and neutral mobs, villagers, players where permitted, and a custom id filter. Each category has its own color and approach alert. |
| **Freecam** | Fly the camera while your player stays exactly where it is.<br>• **Movement:** speed (mouse wheel), acceleration, sprint boost, rotation smoothing and a maximum distance.<br>• **Controls:** a return-to-player key and a position HUD.<br>• Attack, use and pick-block are ignored while detached, because they would act from your player, not the camera. |

### World

| Module | What it does |
|---|---|
| **Block ESP** | Draws World Scanner results as outlines, fills or both, optionally through walls. Also: distance fade, tracers, labels and **vein merging** (touching ores become one box, e.g. `Diamond Ore ×6`). |
| **Holo Sky Studio** | Replaces the overworld sky with a layered procedural sky (gradient, nebula, stars, planets, sun and moon styles, celestial ring, horizon silhouettes). Eight original presets and a full preset library (save, rename, duplicate, export, import). Off by default. |

### Exploration

| Module | What it does |
|---|---|
| **Waypoints & Bases** | Per-world waypoints, each with a name, category (Base, Point of Interest, Scouted, Death, Other), note and timestamp.<br>• **Display:** beacons with distances and a compass HUD.<br>• **Automatic:** death points and an optional scouting trail.<br>• **Managing:** a quick-add key, an add/edit form, and a list with show/hide, edit and delete.<br>• **Sharing:** clipboard export and import. |
| **World Scanner** | Formerly Block Finder. Finds amethyst, ores, debris, containers, spawners, vaults and your own blocks or `#tags` in chunks your client **already has loaded**. Each category has its own color, range, tracer and alert. |
| **Entity Visibility** | Logs tab-list joins and leaves, game-mode changes and invisibility-flag changes. Where player detection is permitted, it also logs nearby players disappearing while still listed. Shown in a log and optionally as alerts. It never reveals hidden players. |

### Player

| Module | What it does |
|---|---|
| **Hollow Hands** | Re-timed swings, walk bob, sprint lean, idle sway, equip styles, positioning and optional swing trails. Presets: **Subtle**, **Smooth**, **Stylized**. Only the drawing changes. |
| **Character Studio** | Eight original 3D outfits worn on your own skin, shown in third person, the inventory and the menus. Client-side only. |

### Performance

| Module | What it does |
|---|---|
| **Performance** | **Presets** Potato, Low, Balanced, High Quality and Custom; *Apply* sets sky detail, menu and loading effects, motion blur, swing trails and FPS Boost. Other modules, keybinds and unrelated settings are never touched. Optional **Adaptive Quality** lowers Holo's own effects one step at a time, with a cooldown, while FPS stays under your target. |
| **FPS Boost** | Skips drawing far mobs, items and other entities (they still exist and tick), caps particles, and limits the frame rate while the window is in the background. |

### HUD

| Module | What it does |
|---|---|
| **Hollow Alerts** | One notification system for every detector, e.g. `HOLO ALERT — Amethyst Cluster Detected · 12.4m`. Repeats merge into a count. Each source has its own toggle, sound and cooldown. |
| **Performance Monitor** | FPS, average FPS, frame time (average and worst), memory, entities and particles, and the active preset. Warns about low FPS, stutter and high memory. |
| **HUD Layout** | The drag-and-drop **HUD editor**: snap guides, arrow-key nudging, per-element reset and *Reset All*. Positions are relative to the screen, so they survive resolution changes. |

### Settings

| Module | What it does |
|---|---|
| **Interface** | Theme (Holo Violet, Pharaoh Gold, Crimson, Frost, Emerald, Custom), accent and detail colors, backdrop dim, descriptions, animation speed, **Reduced Motion** and compact sidebar. |
| **Main Menu** | The Holo title screen. Scenes, a live 3D figure, particles and a glowing logo. It keeps every vanilla entry and adds Character Studio, Sky Studio and Client Settings. *Vanilla Menu* or turning the module off restores Minecraft's own title screen. |
| **Loading Screen** | Restyles Minecraft's loading overlay with a ring that fills with the **real** reload progress. It never delays loading. |
| **Theme Packs** | Eight collections that each bundle a sky, outfit, interface accent, hand style and menu scene. You can apply only some parts. |
| **Server Rules** | Lists of servers that are restricted (detection off), permit player detection, or **permit automation**. |

## Fair-play boundaries

- **Automated modules only run where automation is allowed.** These are Kill Aura, Trigger Bot, Aim Assist, Auto Bridge, Safe Walk and Speed Bridge's automatic placement.
  - They run in single-player worlds.
  - On a server, they run only if you add it under *Server Rules → Servers Permitting Automation*.
  - A server under *Restricted Servers* always wins.
  - Anywhere else the module shows **BLOCKED** with the reason, and does nothing.
  - Only add a server if its rules allow these tools.
- **Everything uses the game's own actions.** Attacks, placements and camera turns go through the same code paths as your own input.
  - No packets are crafted, delayed or altered.
  - Server confirmations are never faked.
  - Vanilla reach and attack cooldowns always apply.
  - Nothing hides the client from anti-cheat.
- **Detection only reads data the client already has.**
  - Chunks are looked up with `getChunk(..., false)`, which returns nothing for unloaded chunks and never requests them.
  - Entity tracking uses the client's render list, and skips invisible entities and spectators.
- **Player detection is off on every multiplayer server** unless you add that server under *Server Rules → Servers Allowing Player Detection*. Single-player always allows it.
- Servers under *Restricted Servers* (wildcards like `*.example.net` work) turn off the World Scanner, Block ESP, Entity Tracers and Freecam.
- **Entity Visibility only reports what your client observes.** It cannot see through a server's vanish system, and it never shows hidden players or their positions.
- **Freecam is camera-only.** Your player does not move, and attack, use and pick-block are ignored while detached.
- Many servers forbid ESP, tracers, ore finders or combat automation. Check the rules before using them online.

## Using the menu

The menu is one central window:

- **Header:** search box, the active profile, the HUD editor button and a close button. The ≡ button collapses the sidebar to icons.
- **Sidebar:** *All Modules*, then the eleven categories with how many of their modules are on, then *Keybinds*, *Profiles* and *HUD Editor*.
- **Module cards:** name, switch, status, keybind and description.
  - Status is **ON**, **OFF**, **BLOCKED** (on, but not allowed here; hover for the reason), **UNAVAILABLE** (a required hook is missing) or **SETTINGS** (always-on modules).
  - Click a card's switch to turn it on or off. Click anywhere else on the card, or right-click it, to open its settings.
- **Settings panel:** slides in with a live preview where the module has one, the module's switch, and *Reset module* (click twice to confirm). Changed settings show a reset arrow next to their name.
- **Searching:** type anywhere. On a card page, search filters the modules, and with *All Modules* it covers every category. Inside a settings panel, it filters that module's settings.
- **Keyboard:**

  | Key | Action |
  |---|---|
  | `Ctrl+F` | Search |
  | `Tab` / `Shift+Tab` / arrows | Move between cards or settings |
  | `Enter` | Open a module, or activate a setting |
  | `Space` | Toggle |
  | `←` / `→` | Adjust sliders and choices |
  | `Delete` | Reset the focused setting |
  | `Page Up` / `Page Down` | Previous or next category |
  | `Esc` | Close the panel, clear the search, then close the menu |

  While a text field or keybind is being edited, keys go only to it.
- **Controls:**
  - Sliders: drag, or `Shift` + scroll for single steps.
  - Choices: left-click for the next value, right-click for the previous one.
  - Colors: click the swatch for an HSV picker with alpha and hex entry.
- **Keybinds:** click a bind, then press a key, a mouse button or a combination with Ctrl, Shift or Alt.
  - `Backspace` clears it and right-click restores the default.
  - Each module's *Key Mode* is Toggle or Hold.
  - The **Keybinds** page lists every bind, flags conflicts with other Holo binds and Minecraft controls, and can reset all binds.
- **Profiles:** save the current setup under a name, then load, rename, export to the clipboard, import, or delete it. *Reset Everything* returns all modules to defaults and keeps your saved profiles, waypoints and skies.
- **HUD editor:** drag elements, which snap to the screen edges, centre and each other. Arrow keys nudge, right-click resets an element, and *Done* saves.
- **Themes and motion:** *Interface* picks the theme. *Reduced Motion* turns off sliding, scaling and fading.

The menu adapts to the window size, down to Minecraft's smallest GUI (320×240 GUI pixels), where the sidebar turns into icons. It saves when it closes.

Settings save automatically to `config/hollow/hollow.json`, debounced and written atomically. A damaged file is moved to `hollow.json.broken` and defaults are restored. When the format changes, the old file is migrated in place and a copy is kept (for example `hollow.json.v1.bak`). Existing settings and keybinds carry over.

Other files:

| Path | Contents |
|---|---|
| `config/hollow/skies/*.json` | Your custom sky presets. One file per preset, format `holo-sky` v1. |
| `config/hollow/skies/exports/` | Skies written by *Export*. The same JSON is also copied to the clipboard. |
| `config/hollow/backgrounds/` | PNGs for the *Custom Image* menu scene (16–4096 px per side). |
| `config/hollow/profiles/*.json` | Saved profiles, format `holo-profile` v1. |
| `config/hollow/waypoints/*.json` | Waypoints, one file per world (`sp_<save name>` or `mp_<server address>`), format `holo-waypoints` v1. |

Sky textures for the *Texture Layer* come from resource packs. Use an equirectangular PNG and give its id, for example `hollow:textures/sky/aurora_veil.png`. Missing or invalid textures are skipped with one warning in the log.

## Compatibility

- **Holo Sky Studio:**
  - It only replaces the **overworld** sky. The Nether, the End and modded dimensions with their own sky effects are left alone, and changing dimension switches automatically.
  - It stands aside while an Iris shader pack is active, and when the camera is in water or lava the fog is not tinted.
  - If the sky pass fails (for example a driver error), Sky Studio turns itself off, the vanilla sky returns, and the error is logged.
- **Main menu:**
  - Only Minecraft's own `TitleScreen` class is replaced. Mods that subclass or replace the title screen keep theirs.
  - Demo mode always uses vanilla's menu. A rendering error restores the vanilla title screen and turns the module off.
- **Loading screen:** only the drawing of Minecraft's overlay changes; its completion logic does not. Mods that replace the overlay entirely simply take precedence.
- **Resource reloads, disconnects and shutdown:**
  - Sky textures are re-resolved after every resource reload.
  - Menu textures are freed when you join a world.
  - Sky GPU buffers are freed when Sky Studio is turned off.
  - Settings are saved on shutdown.
- **Missing hooks:** if another mod replaces the game renderer or first-person rendering so that Hollow's hook never runs, the affected module disables itself and shows why in the menu, rather than failing silently. Mixin injections are optional (`defaultRequire: 0`), so an incompatible renderer cannot crash the game.
- **Iris:** camera blur pauses while a shader pack is active. This can be toggled under *Motion Blur → Compatibility*.
- **Sodium:** Block Finder and the overlays use their own render hooks and work alongside Sodium.
- **Restoring vanilla:** disabling a module restores vanilla rendering. Hook code passes straight through when its module is off, and motion blur frees its GPU buffers.

### Mixins

All injections are optional (`defaultRequire: 0`) and pass straight through when their module is off.

| Mixin | Target | Purpose |
|---|---|---|
| `GameRendererMixin` | `GameRenderer.render` | Runs the motion blur pass before the HUD. |
| `LevelRendererMixin` | `LevelRenderer.renderLevel` | Captures the view and projection so HUD labels line up with world overlays. |
| `ClientLevelMixin` | `ClientLevel.sendBlockUpdated` | Incremental Block Finder updates. |
| `ItemInHandRendererMixin` | `ItemInHandRenderer` | Hollow Hands animation. |
| `ItemLayerRenderStateMixin` | `ItemStackRenderState.LayerRenderState.submit` | Samples the held item's position for swing trails. |
| `LivingEntityAccessor` | `LivingEntity` | Reads the current swing duration for hand animation timing. |
| `SkyRendererMixin` | `SkyRenderer` | Captures sky state; replaces the overworld sky when Sky Studio is on. |
| `FogRendererMixin` | `FogRenderer.computeFogColor` | Optional fog tint toward the sky's horizon. |
| `CloudRendererMixin` | `CloudRenderer.render` | Hides or tints clouds. |
| `MinecraftMixin` | `Minecraft.setScreen` | Swaps vanilla's title screen for the Holo one. |
| `ScreenMixin` | `Screen.renderPanorama` | Draws the Holo backdrop behind other out-of-world menus. |
| `LoadingOverlayMixin` | `LoadingOverlay.render` / `tick` | Restyles the loading overlay; optional quick hand-off. |
| `MouseHandlerMixin` | `MouseHandler.turnPlayer`, `onScroll` | Applies Aim Assist and Kill Aura turning right after the mouse, scaled by frame time; routes mouse look and the wheel to Freecam. |
| `CameraMixin` | `Camera.setup` | Places the camera at the Freecam position. |
| `KeyboardInputMixin` | `KeyboardInput.tick` | Freecam takes the movement keys (the player stands still); Speed Bridge and Auto Bridge hold sneak at edges. |
| `PlayerMixin` | `Player.isStayingOnGroundSurface` | Safe Walk edge protection, the same check sneaking uses. |
| `EntityRenderDispatcherMixin` | `EntityRenderDispatcher.shouldRender` | FPS Boost entity distances. |
| `ParticleEngineMixin` | `ParticleEngine.add` | FPS Boost particle cap. |
| `FramerateLimitTrackerMixin` | `FramerateLimitTracker.getFramerateLimit` | FPS Boost background frame limit. |

Holo's own shader pipelines are not registered as required pipelines. They compile on first use, so a graphics driver that rejects one only turns off that effect; it can never stop the game from loading.

Character outfits use Fabric API's `EntityModelLayerRegistry` and `LivingEntityFeatureRendererRegistrationCallback`; the 3D figures in menus use its `SpecialGuiElementRegistry`. No mixin is involved.

### Adding a character

1. Add a function to `tools/characters.py` describing its pieces and paint, call it at the bottom, and run `python3 tools/characters.py`.
2. Add an entry to the `Cosmetic` enum with the same id. Its view height should cover hats and horns.

It then appears in Character Studio, the menu options and the in-game layer.

## Performance

Sky Studio is a single full-screen shader pass that replaces vanilla's sky draws. Menu and loading animations allocate nothing per frame and only advance while a menu is visible. Rounded shapes in the menu and HUD use cached corner textures, so each shape is a handful of draws whatever its size.

Frame times below come from the client game test, rendered back to back at 960×540. The best of three rounds is kept.

These were measured on a CPU-only machine (Mesa llvmpipe software rendering), so every pixel costs CPU time. **Real GPUs are far faster**, but the relative differences show where the cost lies. No hardware-GPU measurement has been made, so no FPS gain is promised.

| Scene | Frame time |
|---|---|
| In world, Sky Studio off | 7.8 ms |
| Sky Studio, Vanilla Enhanced | 11.4 ms |
| Sky Studio, Anubis Eclipse, Detail Low | 11.8 ms |
| Sky Studio, Anubis Eclipse, Detail High | 16.9 ms |
| Vanilla title screen | 5.1 ms |
| Holo title screen, without particles and decorations | 7.1 ms |
| Holo title screen, all effects | 8.7 ms |

**1.3.0 against 1.2.0, and the Potato preset** (heavier test scenes; values from two 1.3.0 runs):

| Scene | 1.2.0 | 1.3.0 default | 1.3.0 Potato |
|---|---|---|---|
| Standing still | 19.9 | 21.9 / 22.6 | 18.7 / 18.8 |
| 6000 flame particles | 36.0 | 39.7 / 41.0 | 18.0 / 20.1 |
| Holo menu open | 65.6 (old menu) | 57.4 / 61.4 (new menu) | 24.9 / 25.9 |
| 150 mobs + 200 items, tracers | 38.1 | 39.0 / 37.9 | 27.8 / 28.3 |
| Sky, outfit, blur, tracers + crowd | 33.4 | 34.9 / 34.9 | 27.4 / 30.4 |

**Reading the comparison:**
- **Defaults** cost 1–4 ms more than 1.2.0 in these scenes.
  - 0.9 ms of that was the waypoint compass, which now only shows when there is a waypoint to point at; the 1.3.0 runs above predate that change.
  - New hooks pass straight through while their modules are off.
- **The new menu** is cheaper than the old one.
- **Potato** helps most with particles and crowds.

The nebula noise is the largest sky cost. *Detail: Low* (or the Potato and Low presets) skips it, along with the fine star layer.

## Building

```bash
./gradlew build            # compiles, runs unit tests, produces build/libs/hollow-<version>.jar
./gradlew test             # unit tests only (pure logic: settings, config, profiles, waypoints, aim, bridging, HUD snapping, perf, policy...)
./gradlew runClient        # launches the game with Hollow in a dev environment
./gradlew prodClient         # runs the release jar exactly as players get it (remapped, with Fabric API)
./gradlew runClientGameTest  # launches the real client, exercises the features in a test world, measures frame times and saves screenshots
```

Headless machines can run the client game test under Xvfb:

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest
# screenshots: build/run/clientGameTest/screenshots/
```

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.17.3 or newer for Minecraft 1.21.11.
2. Put `hollow-1.3.0.jar` and Fabric API (0.141.0+1.21.11 or newer) into `.minecraft/mods`. Remove older `hollow-*.jar` files.
3. Start the game and press **Right Shift**.

Upgrading keeps your settings. The config is migrated, a backup is kept (`hollow.json.v3.bak`), and your current setup is saved as the profile **Default**.

## Troubleshooting

| Problem | What to do |
|---|---|
| A module says **BLOCKED** | The server isn't permitted for automation (or for detection, for Freecam and scanners). If the server's rules allow it, add it under *Server Rules*. Single-player always works. |
| A module says **UNAVAILABLE** | Another mod replaced the code Holo hooks into. The reason is shown on the module's page and in `logs/latest.log`. |
| The menu key does nothing | Check *Options → Controls → Key Binds → Holo*. The *Keybinds* page also lists conflicts. |
| A Holo keybind does nothing | Keys are ignored while any menu, chat or text field is open. Check the *Key Mode* (Hold vs Toggle) and the conflict warning. |
| Low FPS | *Performance → Preset: Potato or Low → Apply*, turn on *FPS Boost*, and try *Adaptive Quality*. Sky Studio's *Detail: Low* helps most on weak GPUs. |
| HUD elements are off-screen or overlapping | *HUD Layout → Open Editor → Reset All*. |
| Something looks wrong after many changes | *Profiles → Reset Everything*, or load a saved profile. Deleting `config/hollow/hollow.json` also restores the defaults. |
| Black screen at start-up (older versions) | Update to 1.1.1 or newer. See the 1.1.1 notes in the changelog. |

## Known limitations

- **Performance numbers** were measured with software rendering only. Results on real GPUs will differ.
- **Server permission lists** are your responsibility. Even on a listed server, its anti-cheat may still flag automated actions. Holo does not try to hide them.
- **Kill Aura and Trigger Bot** act through your own player: your view turns and your attack cooldown applies, exactly as if you had attacked yourself.
- **Freecam** can only show what your client has loaded around your player. The camera stops at *Max Distance From Player*.
- **The World Scanner** only sees loaded chunks. **Entity Visibility** cannot see past a server's vanish system; that is intended.
- **Waypoints** on a server are stored per server address. Several worlds behind the same address, such as a hub and a survival world, share one list. Each waypoint records its dimension.
- **Adaptive Quality** only changes Holo's own effects, not Minecraft's video settings.
- **FPS Boost** skips drawing far entities only; they still exist and tick.
- **The Knockback Tester's** resistance and test hit work in single-player only.
- **AMSS:** no utility by this name exists in Holo or its history, so nothing was changed under that name.

## Project layout

```
src/main/java/dev/hollow/
  core/          Minecraft-independent framework: modules, settings, config, profiles, keybinds, waypoints, aim/bridge/HUD maths, easing, colors
  modules/       Module definitions and their settings
  blur/          Motion blur math (pure) and GPU pass
  hands/         Hand animation state machine (pure) and render hooks
  detection/     Block categories, filters, scanner, vein clustering, entity classification and tracking
  alerts/        Alert model, rate-limited feed (pure) and service (sounds, chat)
  policy/        Server rules (pure) and connection context
  render/        Pipelines, world overlay, HUD labels and toasts, theme, 2D drawing
  sky/           Sky Studio renderer, presets and preset store
  character/     Character Studio outfits: model loading, in-game render layer, 3D menu renderer
  menu/          Holo title screen, menu scene, loading screen, buttons, pixel font
  gui/           The Holo menu (HoloScreen), settings panel, keybind and profile pages, setting widgets
  hud/           HUD element system, HUD editor and module HUDs
  combat/        Aim Assist, Kill Aura, Trigger Bot, reach and knockback logic
  bridge/        Auto Bridge and Speed Bridge engine (geometry is in core/bridge)
  camera/        Freecam
  exploration/   Waypoints and Entity Visibility
  perf/          FPS Boost, frame statistics and adaptive quality state
  input/         Key, mouse-button and combination handling, conflict detection
  practice/      Single-player helpers (practice targets, test hits)
  mixin/         Render, input, camera, screen and loading hooks (see the table above)
src/main/resources/assets/hollow/shaders/   Line, motion blur and sky shaders
src/main/resources/assets/hollow/textures/  Menu scenes, character artwork, cosmetic skins, sky texture
tools/generate_art.py                       Regenerates the menu scenes and sky texture (Python + Pillow)
tools/characters.py                         Defines every outfit and writes its 3D model (assets/hollow/characters) and texture
src/test/        Unit tests
src/gametest/    In-game client test with screenshots
```
