"""Draws a rough isometric preview of a block model, so its shape can be checked without the game.

Not a renderer. It ignores textures and draws each cuboid's three visible faces as flat shaded
polygons, painter-sorted. That is enough to answer the questions worth asking before loading a
world: is the silhouette right, does the roof meet the body, is the raised flag actually above
the roofline, is anything floating.

    python3 devtools/model_preview.py mailbox mailbox_flag mailbox_wall mailbox_wall_flag
"""
import json
import math
import pathlib
import sys

from PIL import Image, ImageDraw

MODELS = pathlib.Path("src/main/resources/assets/notchcurrency/models/block")
SCALE = 26
SIZE = 720

# Rough colours per texture cell, only so the parts can be told apart.
TINT = {
    (0, 0, 4, 4):    (108, 116, 124),   # roof
    (4, 0, 8, 4):    (150, 114, 76),    # gable end
    (8, 0, 12, 4):   (178, 58, 50),     # flag
    (12, 0, 16, 4):  (72, 66, 62),      # pole
    (0, 4, 4, 8):    (168, 130, 88),    # front, lighter so it can be spotted
    (4, 4, 8, 8):    (150, 114, 76),    # side
    (8, 4, 12, 8):   (140, 106, 70),    # back
    (12, 4, 16, 8):  (150, 114, 76),    # body top
    (0, 8, 4, 12):   (150, 114, 76),    # post
    (4, 8, 8, 12):   (176, 138, 96),    # post top
    (8, 8, 12, 12):  (118, 88, 58),     # base
    (12, 8, 16, 12): (118, 88, 58),     # body underside
}


def project(x, y, z):
    """Isometric: x to the right and down, z to the left and down, y straight up."""
    px = (x - z) * math.cos(math.radians(30))
    py = (x + z) * math.sin(math.radians(30)) - y
    return SIZE / 2 + px * SCALE, SIZE * 0.72 + py * SCALE


def rotate(points, rot):
    if not rot:
        return points
    a = math.radians(rot["angle"])
    ox, oy, oz = rot["origin"]
    axis = rot["axis"]
    out = []
    for x, y, z in points:
        if axis == "z":
            dx, dy = x - ox, y - oy
            x, y = ox + dx * math.cos(a) - dy * math.sin(a), oy + dx * math.sin(a) + dy * math.cos(a)
        elif axis == "x":
            dy, dz = y - oy, z - oz
            y, z = oy + dy * math.cos(a) - dz * math.sin(a), oy + dy * math.sin(a) + dz * math.cos(a)
        else:
            dx, dz = x - ox, z - oz
            x, z = ox + dx * math.cos(a) - dz * math.sin(a), oz + dx * math.sin(a) + dz * math.cos(a)
        out.append((x, y, z))
    return out


def shade(colour, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in colour)


def draw(name):
    data = json.loads((MODELS / f"{name}.json").read_text())
    img = Image.new("RGB", (SIZE, SIZE), (238, 238, 242))
    d = ImageDraw.Draw(img)

    # The block's own outline, for scale.
    for a, b in [((0, 0, 0), (16, 0, 0)), ((16, 0, 0), (16, 0, 16)), ((16, 0, 16), (0, 0, 16)),
                 ((0, 0, 16), (0, 0, 0)), ((0, 0, 0), (0, 16, 0)), ((16, 0, 0), (16, 16, 0)),
                 ((0, 0, 16), (0, 16, 16)), ((0, 16, 0), (16, 16, 0)), ((0, 16, 0), (0, 16, 16))]:
        d.line([project(*a), project(*b)], fill=(202, 202, 210), width=1)

    quads = []
    for el in data["elements"]:
        x0, y0, z0 = el["from"]
        x1, y1, z1 = el["to"]
        rot = el.get("rotation")
        # Only the three faces an isometric camera from +x/+y/+z can see.
        sides = {
            "up":    [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
            "east":  [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
        }
        lighting = {"up": 1.0, "south": 0.78, "east": 0.62}
        for side, pts in sides.items():
            face = el["faces"].get(side)
            if face is None:
                continue
            pts = rotate(pts, rot)
            colour = shade(TINT.get(tuple(face["uv"]), (200, 60, 200)), lighting[side])
            # Painter order for this camera: bigger x + y + z is nearer. Height counts as
            # much as the other two, or a roof sorts behind the body it sits on.
            depth = sum(p[0] + p[1] + p[2] for p in pts) / len(pts)
            quads.append((depth, [project(*p) for p in pts], colour))

    for _, poly, colour in sorted(quads, key=lambda q: q[0]):
        d.polygon(poly, fill=colour, outline=shade(colour, 0.7))

    d.text((14, 14), name, fill=(40, 40, 48))
    return img


names = sys.argv[1:] or ["mailbox"]
sheet = Image.new("RGB", (SIZE * len(names), SIZE), (238, 238, 242))
for i, n in enumerate(names):
    sheet.paste(draw(n), (SIZE * i, 0))
out = ("/private/tmp/claude-503/-Users-bjpelicano-Desktop-NEW-MODS-Notch-Currency---Test-2/"
       "e4a58a35-d84d-488a-84c8-80b25e3d5795/scratchpad/mailbox_preview.png")
sheet.resize((min(1600, SIZE * len(names)), int(SIZE * min(1600, SIZE * len(names)) / (SIZE * len(names)))),
             Image.LANCZOS).save(out)
print("preview:", out)
