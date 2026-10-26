# Hollow

A dark, grim-reaper themed Fabric utility client for **Minecraft 1.21.11**: restrained motion blur, fluid first-person hand animations, configurable block detection, entity tracers and a unified alert system, all inside a black-and-purple module browser.

Open the menu with **Right Shift** (rebind under *Controls → Hollow*).

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.17.3 or newer |
| Fabric API | 0.141.0+1.21.11 or newer |
| Java | 21 |

Install: build the jar (see below) or take `build/libs/hollow-<version>.jar`, then drop it with Fabric API into `.minecraft/mods`.

## Features

### Visuals & Detection

| Module | What it does |
|---|---|
| **Motion Blur** | Frame-accumulation blur that only appears during fast camera turns, plus soft motion trails on Hollow's own panels, pages and toasts. Intensity, duration and sensitivity are separate for the camera and the GUI. The pass runs *before* the HUD, so text, crosshair, chat and menus are never blurred. Pauses automatically while an Iris shader pack is active. |
| **Hollow Hands** | Re-timed swing and attack animations with configurable speed, arc intensity and easing curve, walk bob synced to the game's own step phase, sprint lean, idle sway, smooth blending between idle/walk/sprint, equip styles (smooth dip, side slide, instant), softened transitions into and out of eating/blocking/drawing, adjustable position, scale and rotation, and optional purple swing trails. Presets: **Subtle**, **Smooth**, **Stylized**. Attack timing and gameplay are untouched: only the drawing changes. |
| **Block Finder** | Scans chunks the client already has loaded for the selected blocks: amethyst clusters, large, medium and small buds, and budding amethyst (each separately), every ore, ancient debris, chests (including copper chests), barrels, ender chests, shulker boxes, spawners, vaults, plus any block or `#tag` you add. Each category has its own color, range, tracer and alert toggle. Whitelist and blacklist are supported. Work is spread over ticks, sections without targets are skipped via the palette, and single-block updates are applied incrementally. |
| **Block ESP** | Draws Block Finder results as outlines, fills or both, optionally through walls, with distance fade, tracers, labels, and **vein merging** (touching ores become one box, e.g. `Diamond Ore ×6`). Per-category and total limits keep the screen clean. Turn it off to use Block Finder as alerts-only. |
| **Entity Tracers** | Lines, hitbox outlines and labels (name, health, distance) for hostile mobs, passive mobs and animals, villagers and neutral mobs, players where permitted, and a custom id filter. Each category has its own color and approach alert. Thickness, opacity, target point and range are adjustable. |
| **Hollow Alerts** | One notification system for every detector, for example `HOLLOW ALERT — Amethyst Cluster Detected · 12.4m`. Toasts slide in with a purple glow and merge repeats into a count (`×3`) instead of spamming. Each source (blocks, hostile, passive, neutral, players) has its own toggle, sound and cooldown. Optional chat echo, five sounds, and a preview button. |

### Client

| Module | What it does |
|---|---|
| **Interface** | Accent color, panel opacity, backdrop dim, descriptions, animation speed. |
| **Server Rules** | Lists of servers where detection must stay off, and servers where player detection is permitted. |

## Fair-play boundaries

- Detection only reads data the client already has. Chunks are looked up with `getChunk(..., false)`, which returns nothing for unloaded chunks and never requests them.
- Entity tracking uses the client's render list, and skips invisible entities and spectators.
- **Player detection is off on every multiplayer server** unless you add that server under *Server Rules → Servers Allowing Player Detection*. Single-player always allows it.
- Servers listed under *Restricted Servers* (wildcards like `*.example.net` work) turn off Block Finder, Block ESP and Entity Tracers entirely.
- Many servers forbid ESP, tracers or ore finders. Check the rules before using them online.

## Using the menu

- Click a module on the left to open its page. **Right-click** it to toggle the module.
- Type anywhere to search. Matching counts appear next to each module. `Ctrl+F` focuses the search box and `Esc` clears it.
- Sliders: drag, **right-click to reset**, or `Shift` + scroll for single steps.
- Enum pickers: left-click for next, right-click for previous.
- Colors: click the swatch for an HSV picker with alpha and hex entry. Right-click the swatch to reset.
- Keybinds: click, then press a key. `Backspace` unbinds.
- Section headers collapse. Block Finder's categories start collapsed.
- `↑`/`↓` switch modules. `Esc` or Right Shift closes the menu.

Settings save automatically to `config/hollow/hollow.json`, debounced and written atomically. A damaged file is moved to `hollow.json.broken` and defaults are restored.

## Compatibility

- **Missing hooks:** if another mod replaces the game renderer or first-person rendering so that Hollow's hook never runs, the affected module disables itself and shows why in the menu, rather than failing silently. Mixin injections are optional (`defaultRequire: 0`), so an incompatible renderer cannot crash the game.
- **Iris:** camera blur pauses while a shader pack is active. This can be toggled under *Motion Blur → Compatibility*.
- **Sodium:** Block Finder and the overlays use their own render hooks and work alongside Sodium.
- **Restoring vanilla:** disabling a module restores vanilla rendering. Hook code passes straight through when its module is off, and motion blur frees its GPU buffers.

## Building

```bash
./gradlew build            # compiles, runs unit tests, produces build/libs/hollow-<version>.jar
./gradlew test             # unit tests only (pure logic: settings, config, blur math, hands, filters, alerts, policy)
./gradlew runClient        # launches the game with Hollow in a dev environment
./gradlew runClientGameTest  # launches the real client, builds a test scene, asserts behaviour and saves screenshots
```

Headless machines can run the client game test under Xvfb:

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest
# screenshots: build/run/clientGameTest/screenshots/
```

## Project layout

```
src/main/java/dev/hollow/
  core/          Minecraft-independent framework: modules, settings, config, easing, colors
  modules/       Module definitions and their settings
  blur/          Motion blur math (pure) and GPU pass
  hands/         Hand animation state machine (pure) and render hooks
  detection/     Block categories, filters, scanner, vein clustering, entity classification and tracking
  alerts/        Alert model, rate-limited feed (pure) and service (sounds, chat)
  policy/        Server rules (pure) and connection context
  render/        Pipelines, world overlay, HUD labels and toasts, theme, 2D drawing
  gui/           Module browser screen and setting widgets
  mixin/         GameRenderer, LevelRenderer, ItemInHandRenderer, ClientLevel hooks
src/main/resources/assets/hollow/shaders/   Fog-free line shader and motion blur shader
src/test/        Unit tests
src/gametest/    In-game client test with screenshots
```
