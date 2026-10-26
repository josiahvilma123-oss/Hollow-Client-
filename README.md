# Holo

A dark-fantasy Fabric client for **Minecraft 1.21.11** (formerly *Hollow*). It includes:

- Restrained motion blur and fluid first-person hand animations.
- Configurable block detection, entity tracers and a unified alert system.
- Original custom skies, client-side character cosmetics and coordinated theme packs.
- An animated Egyptian-themed main menu and loading screen.

Everything lives in a black-and-purple module browser.

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

### Visuals & Detection

| Module | What it does |
|---|---|
| **Motion Blur** | Frame-accumulation blur that only appears during fast camera turns, plus soft motion trails on Hollow's own panels, pages and toasts. Intensity, duration and sensitivity are separate for the camera and the GUI. The pass runs *before* the HUD, so text, crosshair, chat and menus are never blurred. Pauses automatically while an Iris shader pack is active. |
| **Hollow Hands** | Re-timed swing and attack animations with configurable speed, arc intensity and easing curve, walk bob synced to the game's own step phase, sprint lean, idle sway, smooth blending between idle/walk/sprint, equip styles (smooth dip, side slide, instant), softened transitions into and out of eating/blocking/drawing, adjustable position, scale and rotation, and optional purple swing trails. Presets: **Subtle**, **Smooth**, **Stylized**. Attack timing and gameplay are untouched: only the drawing changes. |
| **Block Finder** | Scans chunks the client already has loaded for the selected blocks: amethyst clusters, large, medium and small buds, and budding amethyst (each separately), every ore, ancient debris, chests (including copper chests), barrels, ender chests, shulker boxes, spawners, vaults, plus any block or `#tag` you add. Each category has its own color, range, tracer and alert toggle. Whitelist and blacklist are supported. Work is spread over ticks, sections without targets are skipped via the palette, and single-block updates are applied incrementally. |
| **Block ESP** | Draws Block Finder results as outlines, fills or both, optionally through walls, with distance fade, tracers, labels, and **vein merging** (touching ores become one box, e.g. `Diamond Ore ×6`). Per-category and total limits keep the screen clean. Turn it off to use Block Finder as alerts-only. |
| **Entity Tracers** | Lines, hitbox outlines and labels (name, health, distance) for hostile mobs, passive mobs and animals, villagers and neutral mobs, players where permitted, and a custom id filter. Each category has its own color and approach alert. Thickness, opacity, target point and range are adjustable. |
| **Hollow Alerts** | One notification system for every detector, for example `HOLLOW ALERT — Amethyst Cluster Detected · 12.4m`. Toasts slide in with a purple glow and merge repeats into a count (`×3`) instead of spamming. Each source (blocks, hostile, passive, neutral, players) has its own toggle, sound and cooldown. Optional chat echo, five sounds, and a preview button. |

### Studio

| Module | What it does |
|---|---|
| **Holo Sky Studio** | Replaces the overworld sky with a layered procedural sky. Layers: gradient and horizon glow, optional resource-pack texture, nebula, two star layers, planets with rings, sun and moon styles (disc, halo, crescent, eclipse), an ornamental celestial ring, and horizon silhouettes. Ships seven original presets: Anubis Eclipse, Void Realm, Pharaoh's Night, Astral Horizon, Crimson Dusk, Frozen Cosmos and Vanilla Enhanced. The library has search, Save, Save As, Rename, Duplicate, Delete, Revert, Export, Import and *Restore Vanilla Sky*. Simple mode shows the essentials; *Advanced Controls* shows every parameter. Off by default. |
| **Character Studio** | Original Pharaoh, Wraith and Hollow Knight cosmetic sets drawn on your own player (third person and inventory). Client-side only: nobody else sees them and nothing is sent to servers. Includes a live 3D preview. |
| **Theme Packs** | Pharaoh, Wraith and Hollow Knight collections. Each bundles a sky, character set, interface accent, hand-animation preset with trail color, and menu scene. Apply only the ticked components, or the whole collection. Applying a sky never changes your character or interface, and the reverse holds too. |

### Client

| Module | What it does |
|---|---|
| **Main Menu** | The Holo title screen. Scenes: Temple of Anubis, Pyramids at Night, Void Realm, your Sky Studio sky, or a custom PNG from `config/hollow/backgrounds/`. It adds a featured character, particles, and a glowing logo with a celestial ring. It keeps every vanilla entry (Singleplayer, Multiplayer, Realms, Options, Language, Accessibility, Quit, Credits) and adds Character Studio, Sky Studio and Client Settings. Other out-of-world menus share its backdrop. *Vanilla Menu* or turning the module off restores Minecraft's own title screen. |
| **Loading Screen** | Restyles Minecraft's loading overlay with a logo reveal and a ring that fills with the **real** reload progress, plus a status line, particles and a comet trail. Offers reduced motion, a speed setting, and *Skip Fade-Out* for a quick hand-off to the menu. It never delays loading. |
| **Performance** | Quality, Balanced and Low Performance profiles that scale sky detail, menu and loading effects, motion blur and swing trails. |
| **Interface** | Accent color, panel opacity, backdrop dim, descriptions, animation speed. |
| **Server Rules** | Lists of servers where detection must stay off, and servers where player detection is permitted. |

## Fair-play boundaries

- Detection only reads data the client already has. Chunks are looked up with `getChunk(..., false)`, which returns nothing for unloaded chunks and never requests them.
- Entity tracking uses the client's render list, and skips invisible entities and spectators.
- **Player detection is off on every multiplayer server** unless you add that server under *Server Rules → Servers Allowing Player Detection*. Single-player always allows it.
- Servers listed under *Restricted Servers* (wildcards like `*.example.net` work) turn off Block Finder, Block ESP and Entity Tracers entirely.
- Many servers forbid ESP, tracers or ore finders. Check the rules before using them online.

## Using the menu

The menu is a click GUI: floating, rounded category windows over the game, in the style of Meteor.

- **Left-click** a module to toggle it. **Right-click** it to open its settings window. Always-on modules open their settings on any click.
- **Drag** a window's header to move it. Click or right-click a header to collapse it. Positions are remembered; *Interface → Reset Window Layout* restores them.
- Hovering a module shows its description.
- The settings window can be dragged by its title bar. It has an on/off switch, a close button and a live preview at the top.
- Type anywhere to search. With no settings window open, the category windows filter to matching modules and show match counts. With one open, its settings are filtered instead. `Ctrl+F` focuses the search box and `Esc` clears it.
- Sliders: drag, **right-click to reset**, or `Shift` + scroll for single steps.
- Enum pickers: left-click for next, right-click for previous.
- Colors: click the swatch for an HSV picker with alpha and hex entry. Right-click the swatch to reset.
- Keybinds: click, then press a key. `Backspace` unbinds.
- Section headers collapse. Block Finder's categories start collapsed.
- `Esc` closes the settings window first, then the menu. Right Shift also closes the menu.

Settings save automatically to `config/hollow/hollow.json`, debounced and written atomically. A damaged file is moved to `hollow.json.broken` and defaults are restored. When the format changes, the old file is migrated in place and a copy is kept (for example `hollow.json.v1.bak`). Existing settings and keybinds carry over.

Other files:

| Path | Contents |
|---|---|
| `config/hollow/skies/*.json` | Your custom sky presets. One file per preset, format `holo-sky` v1. |
| `config/hollow/skies/exports/` | Skies written by *Export*. The same JSON is also copied to the clipboard. |
| `config/hollow/backgrounds/` | PNGs for the *Custom Image* menu scene (16–4096 px per side). |

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

Character cosmetics use Fabric API's `EntityModelLayerRegistry` and `LivingEntityFeatureRendererRegistrationCallback`. No mixin is involved.

## Performance

Sky Studio is a single full-screen shader pass that replaces vanilla's sky draws. Menu and loading animations allocate nothing per frame and only advance while a menu is visible.

Frame times below come from the client game test, rendered back to back at 960×540. The best of three interleaved rounds is kept.

These were measured on a CPU-only machine (Mesa llvmpipe software rendering), so every shader pixel costs CPU time. **Real GPUs are far faster**, but the relative differences show where the cost lies. No hardware-GPU measurement has been made yet.

| Scene | Frame time |
|---|---|
| In world, Sky Studio off | 11.1 ms |
| Sky Studio, Vanilla Enhanced | 16.4 ms |
| Sky Studio, Anubis Eclipse, Detail Low | 17.7 ms |
| Sky Studio, Anubis Eclipse, Detail High | 23.1 ms |
| Vanilla title screen | 6.1 ms |
| Holo title screen, without particles and decorations | 7.3 ms |
| Holo title screen, all effects | 10.3 ms |

The nebula noise is the largest sky cost. *Detail: Low* (or the *Low Performance* profile) skips it, along with the fine star layer.

## Building

```bash
./gradlew build            # compiles, runs unit tests, produces build/libs/hollow-<version>.jar
./gradlew test             # unit tests only (pure logic: settings, config, blur math, hands, filters, alerts, policy)
./gradlew runClient        # launches the game with Hollow in a dev environment
./gradlew runClientGameTest  # launches the real client, checks the menus, loading screen, studios and detection, measures frame times and saves screenshots
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
  sky/           Sky Studio renderer, presets and preset store
  character/     Character Studio cosmetics: models and render layer
  menu/          Holo title screen, menu scene, loading screen, buttons, pixel font
  gui/           Module browser screen and setting widgets
  mixin/         Render, screen and loading hooks (see the table above)
src/main/resources/assets/hollow/shaders/   Line, motion blur and sky shaders
src/main/resources/assets/hollow/textures/  Menu scenes, character artwork, cosmetic skins, sky texture
tools/generate_art.py                       Regenerates the original artwork (Python + Pillow)
src/test/        Unit tests
src/gametest/    In-game client test with screenshots
```
