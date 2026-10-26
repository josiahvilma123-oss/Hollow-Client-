#!/usr/bin/env python3
"""Generates Holo's original artwork. Run from the repository root:

    python3 tools/generate_art.py

Every image is drawn from code here (no external sources), so the art can be regenerated or
tweaked at any time. Character outfits are generated separately by tools/characters.py.
Requires Pillow.
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


if __name__ == "__main__":
    dunes()
    temple()
    aurora()
