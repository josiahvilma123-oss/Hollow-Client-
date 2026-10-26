#!/usr/bin/env python3
"""Generates Holo's original artwork. Run from the repository root:

    python3 tools/generate_art.py

Every image is drawn from code here (no external sources), so the art can be regenerated or
tweaked at any time. Requires Pillow.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hollow/textures"


def save(img, rel):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ROOT.parent.parent.parent.parent.parent))


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


# ----------------------------------------------------------------------------------------------
# Characters: Minecraft-proportioned pixel figures (1 unit = 1 skin pixel), upscaled nearest-
# neighbour so they keep the blocky look, then given a soft violet rim glow.

GRID_W, GRID_H, SCALE = 32, 44, 12


def figure_canvas():
    return Image.new("RGBA", (GRID_W, GRID_H), (0, 0, 0, 0))


def r(img, x, y, w, h, color):
    ImageDraw.Draw(img).rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def finish_figure(small, glow_color, name):
    big = small.resize((GRID_W * SCALE, GRID_H * SCALE), Image.NEAREST)
    pad = 60
    canvas = Image.new("RGBA", (big.width + pad * 2, big.height + pad * 2), (0, 0, 0, 0))
    mask = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    alpha = big.split()[3].point(lambda a: 255 if a > 0 else 0)
    tint = Image.new("RGBA", big.size, glow_color)
    mask.paste(tint, (pad, pad), alpha)
    glow = mask.filter(ImageFilter.GaussianBlur(22))
    glow.putalpha(glow.split()[3].point(lambda a: int(a * 0.85)))
    canvas = Image.alpha_composite(canvas, glow)
    canvas.alpha_composite(big, (pad, pad))
    save(canvas, f"gui/characters/{name}.png")


def body_base(img, skin, shade):
    # Head 8x8, body 8x12, arms 4x12, legs 4x12 centred at x=16.
    r(img, 12, 8, 8, 8, skin)
    r(img, 12, 15, 8, 1, shade)
    r(img, 12, 16, 8, 12, skin)
    r(img, 8, 16, 4, 12, skin)
    r(img, 20, 16, 4, 12, skin)
    r(img, 12, 28, 4, 12, skin)
    r(img, 16, 28, 4, 12, shade)


def pharaoh():
    img = figure_canvas()
    skin, shade = hexc("C68E5A"), hexc("A8723F")
    gold, gold_d, lapis, linen = hexc("E6B54A"), hexc("A97A22"), hexc("1F3A8C"), hexc("EDE6D4")
    body_base(img, skin, shade)
    # Kohl-lined eyes with a faint violet glint.
    r(img, 13, 11, 2, 1, hexc("15100C"))
    r(img, 17, 11, 2, 1, hexc("15100C"))
    r(img, 14, 11, 1, 1, hexc("B57BFF"))
    r(img, 18, 11, 1, 1, hexc("B57BFF"))
    r(img, 15, 14, 2, 1, hexc("6B3B1F"))
    # Nemes headdress: striped crown and lappets.
    for i in range(10):
        col = gold if i % 2 == 0 else lapis
        r(img, 11 + i, 5, 1, 4, col)
    r(img, 11, 8, 1, 10, gold)
    r(img, 20, 8, 1, 10, gold)
    for y in range(9, 20):
        col = gold if y % 2 == 0 else lapis
        r(img, 9, y, 3, 1, col)
        r(img, 20, y, 3, 1, col)
    # Uraeus on the brow.
    r(img, 15, 3, 2, 3, gold)
    r(img, 15, 2, 2, 1, hexc("C0392B"))
    # Broad collar.
    for i, col in enumerate([gold, lapis, gold, hexc("2E9E8F"), gold]):
        r(img, 10 + i // 2, 16 + i, 12 - i, 1, col)
    # Kilt and belt.
    r(img, 12, 26, 8, 1, gold)
    r(img, 11, 27, 10, 7, linen)
    r(img, 15, 27, 2, 7, gold_d)
    # Sandals and arm bands.
    r(img, 12, 39, 4, 1, gold_d)
    r(img, 16, 39, 4, 1, gold_d)
    r(img, 8, 19, 4, 1, gold)
    r(img, 20, 19, 4, 1, gold)
    # Crook staff.
    r(img, 25, 8, 1, 33, gold)
    for y in range(9, 40, 3):
        r(img, 25, y, 1, 1, lapis)
    r(img, 25, 6, 3, 1, gold)
    r(img, 27, 7, 1, 3, gold)
    r(img, 24, 23, 2, 2, skin)
    finish_figure(img, hexc("9D4EDD", 200), "pharaoh")


def wraith():
    img = figure_canvas()
    cloak, cloak_l, edge, void = hexc("16101F"), hexc("241A33"), hexc("6B3FA0"), hexc("050307")
    # Hood.
    r(img, 11, 5, 10, 11, cloak)
    r(img, 12, 4, 8, 1, cloak)
    r(img, 13, 3, 6, 1, cloak)
    r(img, 13, 8, 6, 7, void)
    r(img, 14, 11, 1, 1, hexc("D7B8FF"))
    r(img, 17, 11, 1, 1, hexc("D7B8FF"))
    r(img, 13, 11, 1, 1, hexc("8F5BE0", 160))
    r(img, 18, 11, 1, 1, hexc("8F5BE0", 160))
    # Cloak flaring out toward a tattered hem.
    for y in range(15, 41):
        spread = 5 + (y - 15) // 3
        r(img, 16 - spread, y, spread * 2, 1, cloak if y % 4 else cloak_l)
    rnd = random.Random(7)
    for x in range(16 - 13, 16 + 13):
        cut = rnd.randint(0, 4)
        r(img, x, 41 - cut, 1, cut + 2, (0, 0, 0, 0))
    # Violet edge trim down the opening.
    for y in range(15, 38):
        r(img, 15 - (y - 15) // 8, y, 1, 1, edge)
        r(img, 16 + (y - 15) // 8, y, 1, 1, edge)
    # Skeletal hands.
    r(img, 7, 26, 3, 2, hexc("CFC6D8"))
    r(img, 22, 26, 3, 2, hexc("CFC6D8"))
    finish_figure(img, hexc("7B3FD0", 220), "wraith")


def hollow_knight():
    img = figure_canvas()
    steel, steel_d, plate, cape, glow = hexc("B9B6C8"), hexc("7E7A90"), hexc("3A3548"), hexc("2A1640"), hexc("C9A0FF")
    # Cape behind.
    r(img, 9, 16, 14, 23, cape)
    r(img, 8, 30, 16, 10, cape)
    # Legs and body armour.
    r(img, 12, 28, 4, 12, steel_d)
    r(img, 16, 28, 4, 12, plate)
    r(img, 12, 16, 8, 12, steel)
    r(img, 12, 20, 8, 1, steel_d)
    r(img, 15, 17, 2, 8, steel_d)
    r(img, 12, 26, 8, 2, plate)
    # Arms and wide pauldrons.
    r(img, 8, 18, 4, 10, steel_d)
    r(img, 20, 18, 4, 10, steel_d)
    r(img, 6, 15, 7, 4, steel)
    r(img, 19, 15, 7, 4, steel)
    r(img, 6, 18, 7, 1, steel_d)
    r(img, 19, 18, 7, 1, steel_d)
    # Helm with T-visor glowing violet, and a swept crest.
    r(img, 11, 6, 10, 10, steel)
    r(img, 11, 15, 10, 1, steel_d)
    r(img, 12, 10, 8, 1, glow)
    r(img, 15, 10, 2, 4, glow)
    for i in range(6):
        r(img, 15 - i // 2, 5 - i, 3, 1, hexc("5B2E8C"))
    # Greatsword planted at the side.
    r(img, 26, 6, 2, 30, hexc("D6D3E2"))
    r(img, 26, 6, 1, 30, hexc("F2F0FA"))
    r(img, 24, 33, 6, 1, steel_d)
    r(img, 26, 34, 2, 5, plate)
    r(img, 24, 23, 2, 3, steel_d)
    finish_figure(img, hexc("9D4EDD", 210), "hollow_knight")


# ----------------------------------------------------------------------------------------------
# Menu scenery: smooth silhouettes with a faint violet rim, drawn at high resolution.

def rim(layer, color, blur=6, strength=0.9):
    alpha = layer.split()[3]
    edge = alpha.filter(ImageFilter.FIND_EDGES).filter(ImageFilter.GaussianBlur(blur))
    tint = Image.new("RGBA", layer.size, color)
    tint.putalpha(edge.point(lambda a: int(min(255, a * 3) * strength)))
    out = Image.alpha_composite(tint, layer)
    return out


def dunes():
    w, h = 2048, 512
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    far, near = hexc("120B1B"), hexc("07050B")
    # Distant pyramids.
    for cx, base, height in [(380, 380, 230), (720, 260, 140), (1500, 420, 260), (1840, 220, 120)]:
        d.polygon([(cx - base // 2, h - 120), (cx, h - 120 - height), (cx + base // 2, h - 120)], fill=far)
    # Near dunes.
    points = [(0, h)]
    for x in range(0, w + 16, 16):
        y = h - 120 + 26 * math.sin(x / 260.0) + 14 * math.sin(x / 97.0 + 1.3)
        points.append((x, y))
    points.append((w, h))
    d.polygon(points, fill=near)
    save(rim(img, hexc("9D4EDD"), 5, 0.6), "gui/menu/dunes.png")


def temple():
    w, h = 2048, 768
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stone = hexc("0E0915")
    ground = h - 40
    # Pylon gate: two tapered towers and a lintel.
    for cx in (w // 2 - 260, w // 2 + 260):
        d.polygon([(cx - 170, ground), (cx - 130, ground - 420), (cx + 130, ground - 420), (cx + 170, ground)], fill=stone)
        d.rectangle([cx - 140, ground - 440, cx + 140, ground - 420], fill=stone)
    d.rectangle([w // 2 - 120, ground - 330, w // 2 + 120, ground - 290], fill=stone)
    # Columns inside the gate.
    for i in range(4):
        x = w // 2 - 90 + i * 60
        d.rectangle([x - 12, ground - 290, x + 12, ground], fill=stone)
        d.rectangle([x - 18, ground - 300, x + 18, ground - 286], fill=stone)
    # Obelisks.
    for cx, height in [(330, 520), (1720, 560)]:
        d.polygon([(cx - 26, ground), (cx - 18, ground - height), (cx, ground - height - 40), (cx + 18, ground - height), (cx + 26, ground)], fill=stone)
    # Seated jackal guardians flanking the gate.
    for cx, flip in [(w // 2 - 560, 1), (w // 2 + 560, -1)]:
        f = flip
        d.rectangle([cx - 120, ground - 70, cx + 120, ground], fill=stone)
        body = [(cx - 70 * f, ground - 70), (cx - 80 * f, ground - 230), (cx - 20 * f, ground - 300),
                (cx + 60 * f, ground - 240), (cx + 90 * f, ground - 70)]
        d.polygon(body, fill=stone)
        head = [(cx - 40 * f, ground - 290), (cx - 50 * f, ground - 380), (cx - 30 * f, ground - 440), (cx - 15 * f, ground - 380),
                (cx + 10 * f, ground - 400), (cx + 4 * f, ground - 450), (cx + 26 * f, ground - 390), (cx + 90 * f, ground - 350),
                (cx + 30 * f, ground - 320), (cx + 10 * f, ground - 280)]
        d.polygon(head, fill=stone)
    d.rectangle([0, ground, w, h], fill=stone)
    out = rim(img, hexc("B57BFF"), 7, 0.75)
    # Glowing glyph band on the lintel.
    g = ImageDraw.Draw(out)
    rnd = random.Random(3)
    for i in range(14):
        x = w // 2 - 112 + i * 16
        shape = rnd.randint(0, 2)
        col = hexc("E6B54A", 200)
        if shape == 0:
            g.ellipse([x, ground - 322, x + 8, ground - 314], outline=col, width=2)
        elif shape == 1:
            g.line([x + 4, ground - 324, x + 4, ground - 312], fill=col, width=2)
        else:
            g.polygon([(x, ground - 312), (x + 4, ground - 324), (x + 8, ground - 312)], outline=col)
    save(out, "gui/menu/temple.png")


# ----------------------------------------------------------------------------------------------
# Sky texture layer: an aurora veil, equirectangular 2:1, mostly transparent.

def aurora():
    w, h = 1024, 512
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for x in range(w):
        a = x / w * math.tau
        centre = 0.34 + 0.06 * math.sin(a * 2.0) + 0.03 * math.sin(a * 5.0 + 1.0)
        intensity = 0.5 + 0.5 * math.sin(a * 3.0 + 0.7)
        for y in range(int(h * 0.12), int(h * 0.5)):
            v = y / h
            band = math.exp(-((v - centre) / 0.045) ** 2)
            curtain = band * (0.6 + 0.4 * math.sin(a * 40.0 + v * 30.0)) * intensity
            if curtain < 0.02:
                continue
            green = 0.6 + 0.4 * (v - centre) / 0.1
            px[x, y] = (int(90 * curtain), int(255 * min(1, green) * curtain), int(220 * curtain), int(255 * min(1, curtain)))
    save(img.filter(ImageFilter.GaussianBlur(2)), "sky/aurora_veil.png")


# ----------------------------------------------------------------------------------------------
# Cosmetic skins (64x64) for the Character Studio models. Each model cube samples a region;
# regions are filled with patterned colour so any face mapping looks intentional.

def cosmetic(name, base, accent, trim, pattern):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(64):
        for x in range(64):
            if pattern == "stripes":
                col = base if (y // 2) % 2 == 0 else accent
            elif pattern == "cloth":
                col = base if (x * 7 + y * 3) % 11 else accent
            else:
                col = base if (x // 4 + y // 4) % 5 else accent
            d.point((x, y), fill=col)
    # Trim border rows used by collars, visors and hems.
    d.rectangle([0, 48, 63, 51], fill=trim)
    d.rectangle([0, 56, 63, 63], fill=accent)
    save(img, f"entity/cosmetics/{name}.png")


if __name__ == "__main__":
    pharaoh()
    wraith()
    hollow_knight()
    dunes()
    temple()
    aurora()
    cosmetic("pharaoh", hexc("E6B54A"), hexc("1F3A8C"), hexc("2E9E8F"), "stripes")
    cosmetic("wraith", hexc("16101F"), hexc("2A1C3A"), hexc("8F5BE0"), "cloth")
    cosmetic("hollow_knight", hexc("B9B6C8"), hexc("8C88A0"), hexc("C9A0FF"), "plates")
