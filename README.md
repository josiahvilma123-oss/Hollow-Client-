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

Sky Studio is a single full-screen shader pass that replaces vanilla's sky draws. Menu and loading animations allocate nothing per frame and only advance while a menu is visible. Rounded shapes in the menu and HUD use cached corner textures, so each shape is a handful of draws whatever its size.

Frame times below come from the client game test, rendered back to back at 960×540. The best of three rounds is kept.

These were measured on a CPU-only machine (Mesa llvmpipe software rendering), so every pixel costs CPU time. **Real GPUs are far faster**, but the relative differences show where the cost lies. No hardware-GPU measurement has been made, so no FPS gain is promised.

PERF_TABLES

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
