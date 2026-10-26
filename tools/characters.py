#!/usr/bin/env python3
"""Generates Holo's Character Studio outfits. Run from the repository root:

    python3 tools/characters.py

Each character is a list of 3D pieces attached to the player's head, body, arms or legs. This
script packs every piece onto a texture, paints each face (shading, trims, eyes, blades...)
and writes two files per character:

    src/main/resources/assets/hollow/characters/<id>.json       geometry, read by the mod
    src/main/resources/assets/hollow/textures/entity/cosmetics/<id>.png

Units are skin pixels, as in vanilla models: x points to the character's left, y points down,
z points backwards (the face looks towards -z). Rotations are in radians. Requires Pillow.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hollow"


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


CLEAR = (0, 0, 0, 0)

# ----------------------------------------------------------------------------------------------
# Model description


class Box:
    def __init__(self, frm, size, paint, inflate=0.0):
        self.frm = frm
        self.size = size
        self.paint = paint
        self.inflate = inflate
        self.uv = None


class Part:
    def __init__(self, parent, name, boxes, pivot=(0, 0, 0), rot=(0, 0, 0)):
        self.parent = parent
        self.name = name
        self.boxes = boxes
        self.pivot = pivot
        self.rot = rot


def paint(color, mat="cloth", **faces):
    """color: base colour; mat: surface material; faces: decals per face.

    Face keys: north (front), south (back), east, west, up, down, sides (all four sides),
    all (every face). Each value is a list of decal functions."""
    return {"color": color, "mat": mat, "decals": faces}


# ----------------------------------------------------------------------------------------------
# Decals: functions(img, x, y, w, h, face, rng) drawing on one face region


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def rows(color, start, count, from_bottom=False):
    def draw(img, x, y, w, h, face, rng):
        for r in range(count):
            ry = (h - 1 - start - r) if from_bottom else (start + r)
            if 0 <= ry < h:
                for c in range(w):
                    px(img, x + c, y + ry, color)
    return draw


def trim_bottom(color, count=1):
    return rows(color, 0, count, from_bottom=True)


def trim_top(color, count=1):
    return rows(color, 0, count)


def hstripes(color, period=2, phase=0):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            if (r + phase) % period == 0:
                for c in range(w):
                    px(img, x + c, y + r, color)
    return draw


def vstripes(color, period=2, phase=0):
    def draw(img, x, y, w, h, face, rng):
        for c in range(w):
            if (c + phase) % period == 0:
                for r in range(h):
                    px(img, x + c, y + r, color)
    return draw


def column(color, width=2):
    def draw(img, x, y, w, h, face, rng):
        left = (w - width) // 2
        for r in range(h):
            for c in range(left, left + width):
                px(img, x + c, y + r, color)
    return draw


def border(color):
    def draw(img, x, y, w, h, face, rng):
        for c in range(w):
            px(img, x + c, y, color)
            px(img, x + c, y + h - 1, color)
        for r in range(h):
            px(img, x, y + r, color)
            px(img, x + w - 1, y + r, color)
    return draw


def tatter(count=2):
    """Ragged hem: transparent notches along the bottom rows."""
    def draw(img, x, y, w, h, face, rng):
        for c in range(w):
            depth = rng.choice([0, 0, 1, 1, 2][: count + 3])
            for r in range(min(depth, h)):
                px(img, x + c, y + h - 1 - r, CLEAR)
    return draw


def eyes(color, row, spacing=None, size=1, glow=None):
    def draw(img, x, y, w, h, face, rng):
        gap = spacing if spacing is not None else max(1, w // 4)
        for cx in (gap, w - gap - size):
            for dy in range(size):
                for dx in range(size):
                    px(img, x + cx + dx, y + row + dy, color)
            if glow:
                px(img, x + cx - 1, y + row, glow)
                px(img, x + cx + size, y + row, glow)
    return draw


def slit(row, height=2):
    """Transparent opening across a face (for eyes showing through a mask)."""
    def draw(img, x, y, w, h, face, rng):
        for r in range(row, min(h, row + height)):
            for c in range(1, w - 1):
                px(img, x + c, y + r, CLEAR)
    return draw


def skull(bone, socket, glow):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            for c in range(w):
                px(img, x + c, y + r, shade_color(bone, 1.0 - 0.05 * (r / max(1, h - 1)), rng, 0.04))
        for sx in (1, w - 3):
            for dy in range(2):
                for dx in range(2):
                    px(img, x + sx + dx, y + 3 + dy, socket)
            px(img, x + sx + (1 if sx == 1 else 0), y + 4, glow)
        px(img, x + w // 2 - 1, y + 5, socket)
        px(img, x + w // 2, y + 5, socket)
        for c in range(1, w - 1):
            px(img, x + c, y + h - 2, socket if c % 2 else shade_color(bone, 0.85, rng, 0.02))
        for c in range(0, w):
            px(img, x + c, y + h - 1, shade_color(bone, 0.7, rng, 0.02))
    return draw


def visor(glass, rim, highlight):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            for c in range(w):
                if r == 0 or r == h - 1 or c == 0 or c == w - 1:
                    px(img, x + c, y + r, rim)
                else:
                    t = r / max(1, h - 1)
                    px(img, x + c, y + r, mix(glass, (0, 0, 0, 255), t * 0.45))
        for i in range(2, min(w, h) - 2):
            px(img, x + i, y + i - 1, highlight)
            px(img, x + i + 1, y + i - 1, mix(highlight, glass, 0.5))
    return draw


def t_visor(dark, glow):
    def draw(img, x, y, w, h, face, rng):
        for c in range(1, w - 1):
            px(img, x + c, y + 3, dark)
        for r in range(3, h - 1):
            px(img, x + w // 2, y + r, dark)
        px(img, x + 2, y + 3, glow)
        px(img, x + w - 3, y + 3, glow)
    return draw


def rivets(color):
    def draw(img, x, y, w, h, face, rng):
        if w < 3 or h < 3:
            return
        for cx, cy in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            px(img, x + cx, y + cy, color)
    return draw


def cracks(color, count=3):
    def draw(img, x, y, w, h, face, rng):
        for _ in range(count):
            cx, cy = rng.randrange(w), rng.randrange(h)
            for _ in range(rng.randint(3, 6)):
                px(img, x + cx, y + cy, color)
                cx = max(0, min(w - 1, cx + rng.choice((-1, 0, 1))))
                cy = max(0, min(h - 1, cy + rng.choice((0, 1))))
    return draw


def dots(color, chance=0.08):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            for c in range(w):
                if rng.random() < chance:
                    px(img, x + c, y + r, color)
    return draw


def diamonds(a, b):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            for c in range(w):
                px(img, x + c, y + r, a if (r + c) % 2 == 0 else b)
    return draw


def gradient(top, bottom):
    def draw(img, x, y, w, h, face, rng):
        for r in range(h):
            t = r / max(1, h - 1)
            for c in range(w):
                px(img, x + c, y + r, shade_color(mix(top, bottom, t), 1.0, rng, 0.03))
    return draw


def edge(color, side="top"):
    """A bright cutting edge along one side of a blade."""
    def draw(img, x, y, w, h, face, rng):
        if side == "top":
            for c in range(w):
                px(img, x + c, y, color)
        else:
            for r in range(h):
                px(img, x + w - 1, y + r, color)
    return draw


def lapel(color):
    def draw(img, x, y, w, h, face, rng):
        mid = w / 2
        for r in range(min(h, int(mid) + 1)):
            px(img, x + int(mid - 1 - r), y + r, color)
            px(img, x + int(mid + r), y + r, color)
    return draw


def scallops(depth=3, period=4):
    """Bat-wing hem: arcs cut out of the bottom edge and the outer top corner."""
    def draw(img, x, y, w, h, face, rng):
        for c in range(w):
            k = (c % period) / period
            cut = int(round(depth * math.sin(math.pi * k)))
            for r in range(cut):
                px(img, x + c, y + h - 1 - r, CLEAR)
        for r in range(h // 3):
            for c in range(w // 3 - r):
                px(img, x + w - 1 - c, y + r, CLEAR) if face == "north" else px(img, x + c, y + r, CLEAR)
    return draw


def ribs(color, count=3):
    def draw(img, x, y, w, h, face, rng):
        for i in range(count + 1):
            tx = (w - 1) * i / count
            for r in range(h):
                t = r / max(1, h - 1)
                cx = int(round(tx * (1 - 0.35 * t)))
                px(img, x + (cx if face == "south" else w - 1 - cx), y + r, color)
        for c in range(w):
            px(img, x + c, y, color)
    return draw


def panel(frame, lights):
    """A chest control panel with a row of coloured lights."""
    def draw(img, x, y, w, h, face, rng):
        x0, y0, pw, ph = 2, 3, w - 4, min(4, h - 4)
        for r in range(ph):
            for c in range(pw):
                px(img, x + x0 + c, y + y0 + r, frame)
        for i, col in enumerate(lights):
            if i < pw - 1:
                px(img, x + x0 + 1 + i, y + y0 + 1, col)
        for c in range(1, pw - 1):
            px(img, x + x0 + c, y + y0 + ph - 2, mix(frame, (0, 0, 0, 255), 0.35))
    return draw


def snowflakes(color):
    def draw(img, x, y, w, h, face, rng):
        for _ in range(max(1, (w * h) // 18)):
            cx, cy = rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1))
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                px(img, x + cx + dx, y + cy + dy, color)
    return draw


# ----------------------------------------------------------------------------------------------
# Colour helpers and materials


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (a[3],)


def shade_color(color, factor, rng, noise):
    n = 1.0 + (rng.random() * 2 - 1) * noise
    return tuple(max(0, min(255, int(color[i] * factor * n))) for i in range(3)) + (color[3],)


FACE_SHADE = {"up": 1.12, "down": 0.62, "north": 1.0, "south": 0.84, "east": 0.9, "west": 0.9}
MATERIAL_NOISE = {"cloth": 0.06, "metal": 0.03, "bone": 0.05, "glass": 0.0, "plain": 0.02, "skin": 0.04}


def base_fill(img, x, y, w, h, face, color, mat, rng):
    for r in range(h):
        for c in range(w):
            factor = FACE_SHADE[face]
            if mat == "metal" and face in ("north", "south", "east", "west"):
                factor *= 1.08 - 0.22 * (r / max(1, h - 1))
                if r == 0:
                    factor *= 1.15
            elif mat == "cloth" and face in ("north", "south", "east", "west"):
                factor *= 0.94 + 0.06 * math.sin(c * 1.7 + r * 0.15)
            elif mat == "glass":
                factor *= 1.1 - 0.3 * (r / max(1, h - 1))
            px(img, x + c, y + r, shade_color(color, factor, rng, MATERIAL_NOISE[mat]))


def face_regions(u, v, w, h, d):
    """Minecraft's box UV layout."""
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


# ----------------------------------------------------------------------------------------------
# Packing and output


def footprint(box):
    w, h, d = box.size
    return 2 * (w + d), d + h


def pack(boxes):
    order = sorted(boxes, key=lambda b: (-footprint(b)[1], -footprint(b)[0]))
    for size in (64, 128, 256):
        x = y = shelf = 0
        placed = {}
        ok = True
        for box in order:
            fw, fh = footprint(box)
            if x + fw > size:
                x, y, shelf = 0, y + shelf, 0
            if y + fh > size or fw > size:
                ok = False
                break
            placed[id(box)] = (x, y)
            x += fw
            shelf = max(shelf, fh)
        if ok:
            for box in boxes:
                box.uv = placed[id(box)]
            return size
    raise ValueError("Character texture does not fit in 256x256")


def build(char_id, parts, seed):
    rng = random.Random(seed)
    boxes = [box for part in parts for box in part.boxes]
    size = pack(boxes)
    img = Image.new("RGBA", (size, size), CLEAR)
    for box in boxes:
        u, v = box.uv
        w, h, d = box.size
        spec = box.paint
        for face, (fx, fy, fw, fh) in face_regions(u, v, w, h, d).items():
            if fw <= 0 or fh <= 0:
                continue
            base_fill(img, fx, fy, fw, fh, face, spec["color"], spec["mat"], rng)
            decals = list(spec["decals"].get("all", []))
            if face in ("north", "south", "east", "west"):
                decals += spec["decals"].get("sides", [])
            decals += spec["decals"].get(face, [])
            for decal in decals:
                decal(img, fx, fy, fw, fh, face, rng)
    out_parts = []
    for part in parts:
        out_parts.append({
            "name": part.name,
            "parent": part.parent,
            "pivot": list(part.pivot),
            "rotation": [round(a, 4) for a in part.rot],
            "cubes": [{"uv": list(b.uv), "from": list(b.frm), "size": list(b.size), "inflate": b.inflate} for b in part.boxes],
        })
    model = {"format": "holo-character", "version": 1, "texture": [size, size], "parts": out_parts}
    model_path = ASSETS / "characters" / f"{char_id}.json"
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.write_text(json.dumps(model, indent=1) + "\n")
    tex_path = ASSETS / "textures/entity/cosmetics" / f"{char_id}.png"
    tex_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(tex_path)
    print(f"wrote {char_id}: {len(boxes)} cubes, {size}x{size} texture")


def mirrored(parts):
    """Left/right copies of parts given for the right side (names ending in _r)."""
    result = []
    for p in parts:
        result.append(p)
        if p.name.endswith("_r"):
            parent = p.parent.replace("right_", "left_") if p.parent.startswith("right_") else (
                p.parent[:-2] + "_l" if p.parent.endswith("_r") else p.parent)
            boxes = [Box((-b.frm[0] - b.size[0], b.frm[1], b.frm[2]), b.size, b.paint, b.inflate) for b in p.boxes]
            result.append(Part(parent, p.name[:-2] + "_l", boxes, (-p.pivot[0], p.pivot[1], p.pivot[2]),
                               (p.rot[0], -p.rot[1], -p.rot[2])))
    return result


# ----------------------------------------------------------------------------------------------
# Characters


def pharaoh():
    gold, blue, teal, linen = hexc("E6B54A"), hexc("1F3A8C"), hexc("2E9E8F"), hexc("EDE6D3")
    stripes = paint(gold, "plain", sides=[hstripes(blue, 2)], up=[vstripes(blue, 2)])
    trim = paint(gold, "metal", sides=[dots(teal, 0.25)])
    parts = mirrored([
        Part("head", "nemes", [Box((-4.5, -8.6, -4.5), (9, 4, 9), stripes, 0.2),
                               Box((-4.5, -5.0, 3.8), (9, 8, 1), stripes)]),
        Part("head", "lappet_r", [Box((-5.2, -5.0, -2.0), (1, 10, 3), stripes)]),
        Part("head", "headband", [Box((-4.0, -6.2, -4.0), (8, 1, 8), trim, 0.45)]),
        Part("head", "uraeus", [Box((-0.5, -10.2, -5.0), (1, 2, 1), paint(gold, "metal", north=[rows(teal, 0, 1)]))]),
        Part("body", "collar", [Box((-4.5, -0.2, -2.5), (9, 3, 5), paint(gold, "metal", sides=[hstripes(teal, 3), hstripes(blue, 3, 1)]), 0.1)]),
        Part("body", "belt", [Box((-4.0, 9.0, -2.0), (8, 1, 4), paint(gold, "metal", north=[column(teal, 2)]), 0.35)]),
        Part("body", "kilt", [Box((-4.5, 10.0, -2.5), (9, 5, 5), paint(linen, "cloth", sides=[vstripes(mix(linen, hexc("8C8170"), 0.35), 2)],
                                                                         north=[column(gold, 3), trim_bottom(gold)]))]),
        Part("right_arm", "armband_r", [Box((-3.0, 3.0, -2.0), (4, 1, 4), trim, 0.35)]),
        Part("right_arm", "bracer_r", [Box((-3.0, 7.0, -2.0), (4, 2, 4), paint(gold, "metal", sides=[hstripes(blue, 2, 1)]), 0.3)]),
    ])
    build("pharaoh", parts, 11)


def wraith():
    cloth, cloth2, violet, glow = hexc("16101F"), hexc("2A1C3A"), hexc("8F5BE0"), hexc("D8B8FF")
    hood = paint(cloth2, "cloth", sides=[trim_bottom(violet)])
    robe = paint(cloth, "cloth", sides=[tatter(), trim_bottom(violet)], north=[column(violet, 1)])
    parts = mirrored([
        Part("head", "veil", [Box((-4.0, -8.0, -4.5), (8, 8, 1), paint(hexc("07050B"), "plain", north=[eyes(glow, 3, 1, 2, violet)]))]),
        Part("head", "hood", [Box((-5.0, -9.2, -5.2), (10, 1, 10), hood),
                              Box((-5.0, -8.4, 4.0), (10, 9, 1), hood),
                              Box((-4.0, -8.4, -5.2), (8, 2, 1), paint(cloth, "cloth"))]),
        Part("head", "hood_side_r", [Box((-5.2, -8.4, -5.2), (1, 9, 10), hood)]),
        Part("body", "wrap", [Box((-4.0, 0.0, -2.0), (8, 12, 4), paint(cloth, "cloth", north=[column(violet, 1)]), 0.25)]),
        Part("body", "mantle", [Box((-5.0, -0.6, -3.0), (10, 3, 6), paint(cloth2, "cloth", sides=[trim_bottom(violet)]), 0.15)]),
        Part("body", "cloak", [Box((-5.0, -0.4, 2.6), (10, 22, 1), paint(cloth2, "cloth", sides=[tatter(), dots(violet, 0.03)]))]),
        Part("body", "skirt", [Box((-4.5, 12.0, -2.5), (9, 9, 5), robe, 0.1)]),
        Part("right_arm", "sleeve_r", [Box((-3.5, -2.3, -2.5), (5, 11, 5), robe)]),
    ])
    build("wraith", parts, 12)


def hollow_knight():
    steel, dark, violet, visor_dark = hexc("B9B6C8"), hexc("8C88A0"), hexc("C9A0FF"), hexc("1E1B29")
    plate = paint(steel, "metal", sides=[rivets(dark)])
    parts = mirrored([
        Part("head", "helm", [Box((-4.5, -8.6, -4.5), (9, 9, 9), paint(steel, "metal", north=[t_visor(visor_dark, violet), rivets(dark)],
                                                                                     sides=[trim_bottom(dark)]), 0.15)]),
        Part("head", "crest", [Box((-0.5, -12.5, -3.0), (1, 4, 7), paint(hexc("7A4CC9"), "cloth", sides=[hstripes(violet, 3)]))]),
        Part("body", "breastplate", [Box((-4.0, 0.0, -2.0), (8, 12, 4), paint(steel, "metal", north=[column(hexc("D7D4E4"), 2), rows(dark, 9, 1)],
                                                                                         sides=[rows(dark, 9, 1)]), 0.4)]),
        Part("body", "cape", [Box((-4.5, 0.2, 2.6), (9, 18, 1), paint(hexc("4B2C86"), "cloth", sides=[trim_bottom(violet, 2)]))]),
        Part("right_arm", "pauldron_r", [Box((-4.0, -2.8, -3.0), (5, 3, 6), plate, 0.1)]),
        Part("right_arm", "gauntlet_r", [Box((-3.0, 7.0, -2.0), (4, 5, 4), paint(steel, "metal", sides=[trim_top(dark)]), 0.3)]),
        Part("right_leg", "greave_r", [Box((-2.0, 5.0, -2.0), (4, 7, 4), paint(steel, "metal", sides=[trim_top(dark), rivets(dark)]), 0.3)]),
    ])
    build("hollow_knight", parts, 13)


def grim_reaper():
    robe, robe2, bone, glow = hexc("15121A"), hexc("221C28"), hexc("D9D2C2"), hexc("B57BFF")
    wood, steel = hexc("3A2A22"), hexc("B8BCC8")
    hood = paint(robe2, "cloth", sides=[trim_bottom(robe)])
    ragged = paint(robe, "cloth", sides=[tatter(3)])
    blade = paint(steel, "metal", all=[edge(hexc("F2F4FF"))], north=[dots(glow, 0.12)])
    parts = mirrored([
        Part("head", "mask", [Box((-4.0, -8.0, -4.6), (8, 8, 1), paint(bone, "bone", north=[skull(bone, hexc("120E16"), glow)]))]),
        Part("head", "hood", [Box((-5.0, -9.6, -5.0), (10, 1, 10), hood),
                              Box((-5.0, -9.0, 4.4), (10, 10, 1), hood),
                              Box((-5.0, -9.6, -5.6), (10, 2, 1), paint(robe, "cloth"))]),
        Part("head", "hood_side_r", [Box((-5.4, -9.0, -5.0), (1, 10, 10), hood)]),
        Part("head", "hood_peak", [Box((-2.5, -2.0, -2.0), (5, 2, 5), hood)], pivot=(0, -9.6, 2), rot=(-0.5, 0, 0)),
        Part("body", "robe", [Box((-4.5, -0.3, -2.5), (9, 13, 5), paint(robe, "cloth", north=[column(hexc("0A080D"), 1)]), 0.1)]),
        Part("body", "rope", [Box((-4.5, 7.0, -2.5), (9, 1, 5), paint(hexc("8C7B5E"), "cloth", sides=[vstripes(hexc("6B5D45"), 2)]), 0.2)]),
        Part("body", "skirt", [Box((-5.0, 12.0, -3.0), (10, 11, 6), ragged)]),
        Part("body", "scythe", [Box((-0.5, -13.0, 0.0), (1, 24, 1), paint(wood, "plain", sides=[hstripes(hexc("2A1E18"), 3)])),
                                Box((0.5, -13.5, 0.0), (6, 2, 1), blade),
                                Box((6.0, -12.0, 0.0), (2, 2, 1), blade),
                                Box((7.0, -10.5, 0.0), (1, 2, 1), blade)],
             pivot=(0, 5, 3.6), rot=(0, 0, 0.62)),
        Part("right_arm", "sleeve_r", [Box((-3.5, -2.3, -2.5), (5, 11, 5), ragged)]),
    ])
    build("grim_reaper", parts, 21)


def shadow_ninja():
    cloth, cloth2, red, steel, gold = hexc("1E1E26"), hexc("2A2A35"), hexc("B8322F"), hexc("A9B3C1"), hexc("C9A24B")
    wrap = paint(hexc("3A3A46"), "cloth", sides=[hstripes(hexc("26262F"), 2)])
    parts = mirrored([
        Part("head", "wrap", [Box((-4.0, -8.0, -4.0), (8, 8, 8), paint(cloth, "cloth", north=[slit(3, 2)]), 0.35)]),
        Part("head", "headband", [Box((-4.0, -7.0, -4.0), (8, 1, 8), paint(red, "cloth", north=[column(steel, 3)]), 0.5)]),
        Part("head", "band_tail_a", [Box((-0.5, -0.5, 0.0), (1, 1, 7), paint(red, "cloth"))], pivot=(1, -6.5, 4.5), rot=(0.45, 0.25, 0)),
        Part("head", "band_tail_b", [Box((-0.5, -0.5, 0.0), (1, 1, 6), paint(red, "cloth"))], pivot=(-1, -6.5, 4.5), rot=(0.65, -0.2, 0)),
        Part("body", "gi", [Box((-4.0, 0.0, -2.0), (8, 12, 4), paint(cloth2, "cloth", north=[lapel(cloth)]), 0.3)]),
        Part("body", "obi", [Box((-4.0, 7.0, -2.0), (8, 2, 4), paint(red, "cloth", north=[column(hexc("7E1F1D"), 2)]), 0.45)]),
        Part("body", "scarf", [Box((-4.5, -0.6, -2.5), (9, 2, 5), paint(red, "cloth"), 0.2)]),
        Part("body", "scarf_tail", [Box((-1.0, 0.0, 0.0), (2, 8, 1), paint(red, "cloth", sides=[tatter(1)]))], pivot=(2.5, 1, 2.7), rot=(0.25, 0, 0.12)),
        Part("body", "katana", [Box((-0.5, -5.0, 0.0), (1, 14, 1), paint(hexc("141418"), "metal", sides=[hstripes(gold, 5)])),
                                Box((-1.5, -6.0, -0.5), (3, 1, 2), paint(gold, "metal")),
                                Box((-0.5, -11.0, 0.0), (1, 5, 1), paint(red, "cloth", sides=[diamonds(red, hexc("141418"))]))],
             pivot=(0, 6, 3.2), rot=(0, 0, -0.7)),
        Part("left_arm", "guard_l", [Box((-1.0, -3.0, -3.0), (5, 3, 6), paint(hexc("4A4F5C"), "metal", sides=[hstripes(hexc("343843"), 2)]), 0.1)]),
        Part("right_arm", "armwrap_r", [Box((-3.0, 5.0, -2.0), (4, 6, 4), wrap, 0.3)]),
        Part("right_leg", "legwrap_r", [Box((-2.0, 6.0, -2.0), (4, 6, 4), wrap, 0.3)]),
        Part("right_leg", "pants_r", [Box((-2.0, 0.0, -2.0), (4, 6, 4), paint(cloth, "cloth"), 0.25)]),
    ])
    build("shadow_ninja", parts, 22)


def frost_witch():
    navy, deep, ice, silver = hexc("23306B"), hexc("141A40"), hexc("9FE6FF"), hexc("E6F2FF")
    hat = paint(navy, "cloth", sides=[dots(hexc("3A4A9A"), 0.06)])
    crystal = paint(ice, "glass", sides=[vstripes(silver, 2)], up=[dots(silver, 0.5)])
    parts = mirrored([
        Part("head", "brim", [Box((-7.0, -8.6, -7.0), (14, 1, 14), paint(navy, "cloth", up=[border(silver)], sides=[rows(silver, 0, 1)]))]),
        Part("head", "crown", [Box((-4.0, -11.6, -4.0), (8, 3, 8), hat),
                               Box((-4.0, -9.6, -4.0), (8, 1, 8), paint(ice, "glass"), 0.3),
                               Box((-1.0, -10.4, -4.8), (2, 2, 1), crystal)]),
        Part("head", "cone_a", [Box((-3.0, -3.0, -3.0), (6, 3, 6), hat)], pivot=(0, -11.6, 0), rot=(-0.2, 0, 0.12)),
        Part("cone_a", "cone_b", [Box((-2.0, -3.0, -2.0), (4, 3, 4), hat)], pivot=(0, -3, 0), rot=(-0.3, 0, 0.18)),
        Part("cone_b", "cone_c", [Box((-1.0, -3.0, -1.0), (2, 3, 2), paint(navy, "cloth", sides=[trim_top(ice, 1)]))], pivot=(0, -3, 0), rot=(-0.35, 0, 0.2)),
        Part("body", "dress", [Box((-4.0, 0.0, -2.0), (8, 12, 4), paint(navy, "cloth", north=[column(silver, 1), hstripes(deep, 3)]), 0.3)]),
        Part("body", "skirt", [Box((-5.0, 10.0, -3.0), (10, 9, 6), paint(navy, "cloth", sides=[trim_bottom(ice, 2), snowflakes(silver)]))]),
        Part("body", "frost_collar", [Box((-5.0, -3.0, 1.5), (10, 4, 1), paint(ice, "glass", sides=[vstripes(silver, 2)], up=[dots(silver, 0.5)]))]),
        Part("body", "cape", [Box((-5.0, -0.3, 2.7), (10, 22, 1), paint(deep, "cloth", sides=[dots(silver, 0.04), trim_bottom(ice)]))]),
        Part("right_arm", "sleeve_r", [Box((-3.5, 4.0, -2.5), (5, 7, 5), paint(navy, "cloth", sides=[trim_bottom(ice)]))]),
        Part("right_arm", "shard_r", [Box((-1.0, -4.0, -1.0), (2, 4, 2), crystal)], pivot=(-2, -2.5, 0), rot=(0, 0, 0.4)),
    ])
    build("frost_witch", parts, 23)


def infernal_demon():
    crimson, dark, ember, horn = hexc("8B1428"), hexc("160A0E"), hexc("FF7A2A"), hexc("2A1A1A")
    membrane = hexc("5A0E1C")
    horn_paint = paint(horn, "bone", up=[dots(ember, 0.5)])
    armor = paint(dark, "metal", sides=[cracks(ember, 3)])
    parts = mirrored([
        Part("head", "horn_r", [Box((-1.0, -3.0, -1.0), (2, 3, 2), horn_paint)], pivot=(-2.5, -8, -1), rot=(-0.35, 0, 0.45)),
        Part("horn_r", "horn_tip_r", [Box((-0.5, -3.0, -0.5), (1, 3, 1), paint(horn, "bone", sides=[trim_top(ember)]))],
             pivot=(0, -3, 0), rot=(-0.55, 0, -0.25)),
        Part("head", "brow", [Box((-4.0, -5.0, -4.3), (8, 2, 1), paint(dark, "metal", north=[eyes(ember, 0, 1, 2)]))]),
        Part("body", "armor", [Box((-4.0, 0.0, -2.0), (8, 8, 4), armor, 0.35)]),
        Part("body", "belt", [Box((-4.0, 8.0, -2.0), (8, 1, 4), paint(crimson, "metal", north=[column(ember, 2)]), 0.45)]),
        Part("body", "loincloth", [Box((-3.0, 9.0, -2.6), (6, 6, 1), paint(crimson, "cloth", sides=[tatter(1), trim_top(dark)]))]),
        Part("body", "wing_r", [Box((-11.0, -4.0, 0.0), (11, 11, 0), paint(membrane, "cloth", all=[ribs(hexc("2A0810"), 3), scallops()]))],
             pivot=(-1.5, 2, 2.3), rot=(0.15, -0.55, 0.2)),
        Part("body", "tail_a", [Box((-0.5, -0.5, 0.0), (1, 1, 5), paint(crimson, "cloth"))], pivot=(0, 11, 2), rot=(0.7, 0, 0)),
        Part("tail_a", "tail_b", [Box((-0.5, -0.5, 0.0), (1, 1, 5), paint(crimson, "cloth"))], pivot=(0, 0, 5), rot=(-0.6, 0, 0)),
        Part("tail_b", "tail_tip", [Box((-1.5, -1.0, 0.0), (3, 2, 2), paint(ember, "plain", sides=[trim_bottom(crimson)]))], pivot=(0, 0, 5), rot=(0.3, 0, 0)),
        Part("right_arm", "spike_r", [Box((-1.0, -3.0, -1.0), (2, 3, 2), horn_paint)], pivot=(-3, -2, 0), rot=(0, 0, 0.6)),
        Part("right_arm", "bracer_r", [Box((-3.0, 6.0, -2.0), (4, 4, 4), armor, 0.3)]),
        Part("right_leg", "greave_r", [Box((-2.0, 6.0, -2.0), (4, 6, 4), armor, 0.3)]),
    ])
    build("infernal_demon", parts, 24)


def star_voyager():
    white, gray, dgray, orange = hexc("E9EDF3"), hexc("9AA3B1"), hexc("5B6474"), hexc("F08A2B")
    glass, gold = hexc("1C2A55"), hexc("D9A441")
    suit = paint(white, "plain", sides=[dots(hexc("D5DAE2"), 0.08)])
    parts = mirrored([
        Part("head", "helmet", [Box((-4.0, -8.0, -4.0), (8, 8, 8), paint(white, "plain", north=[visor(glass, gold, hexc("9FC4FF"))],
                                                                                         east=[rivets(gray)], west=[rivets(gray)]), 1.1)]),
        Part("head", "antenna", [Box((-0.5, -4.0, -0.5), (1, 4, 1), paint(gray, "metal", up=[dots(hexc("FF3B3B"), 1.0)], sides=[trim_top(hexc("FF3B3B"))]))],
             pivot=(3, -9, 1), rot=(0, 0, -0.2)),
        Part("body", "suit", [Box((-4.0, 0.0, -2.0), (8, 12, 4), paint(white, "plain", north=[panel(dgray, [hexc("3BE07A"), hexc("FFD23B"), hexc("FF3B3B"), hexc("3B9BFF")]),
                                                                                         rows(orange, 9, 1)], sides=[rows(orange, 9, 1)]), 0.35)]),
        Part("body", "collar_ring", [Box((-4.5, -1.0, -2.5), (9, 2, 5), paint(gray, "metal"), 0.3)]),
        Part("body", "backpack", [Box((-3.5, 1.0, 2.3), (7, 9, 3), paint(white, "plain", south=[hstripes(gray, 2, 1), border(dgray)], sides=[trim_bottom(dgray)]))]),
        Part("right_arm", "sleeve_r", [Box((-3.0, -2.0, -2.0), (4, 12, 4), paint(white, "plain", sides=[rows(orange, 3, 1), trim_bottom(gray, 3)]), 0.3)]),
        Part("right_leg", "leg_r", [Box((-2.0, 0.0, -2.0), (4, 12, 4), paint(white, "plain", sides=[rows(orange, 5, 1), trim_bottom(gray, 4)]), 0.3)]),
    ])
    build("star_voyager", parts, 25)


if __name__ == "__main__":
    pharaoh()
    wraith()
    hollow_knight()
    grim_reaper()
    shadow_ninja()
    frost_witch()
    infernal_demon()
    star_voyager()
