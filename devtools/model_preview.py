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

# Colour by which texture a face uses, so parts can be told apart without reading the atlas.
TINT = {"#0": (150, 152, 156), "#1": (188, 48, 44)}
FALLBACK = (150, 114, 76)


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
            colour = shade(TINT.get(face.get("texture"), FALLBACK), lighting[side])
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
