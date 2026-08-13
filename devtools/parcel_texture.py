"""Draws the parcel item icon.

Kept so the icon can be nudged later without redrawing it by hand. Kraft paper with an
address label and a stamp: a twine cross was tried first and read as a window frame,
because it cuts the box into four even panes.
"""
from PIL import Image

ART = [
    "................",
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhso.",
    ".obbbbbbbbSSSso.",
    ".oblllllllSSSso.",
    ".oblLLLLLlSSSso.",
    ".oblLLLlllbbbso.",
    ".oblllllllbbbso.",
    ".obbbbbbbbbbbso.",
    ".oTTTTTTTTTTTTo.",
    ".osssssssssssso.",
    ".osssssssssssso.",
    "..oooooooooooo..",
    "................",
    "................",
]

PALETTE = {
    ".": (0, 0, 0, 0),
    "o": (86, 64, 38, 255),      # outline
    "h": (214, 180, 128, 255),   # lit top
    "b": (193, 156, 105, 255),   # paper
    "s": (168, 132, 82, 255),    # shade down the right and along the bottom
    "T": (146, 108, 60, 255),     # twine
    "l": (243, 238, 224, 255),   # address label
    "L": (140, 128, 105, 255),   # writing on the label
    "S": (172, 86, 74, 255),     # stamp
}

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ART):
    for x, ch in enumerate(row):
        img.putpixel((x, y), PALETTE[ch])

img.save("src/main/resources/assets/notchcurrency/textures/item/parcel.png")
img.resize((256, 256), Image.NEAREST).save("/private/tmp/claude-503/-Users-bjpelicano-Desktop-NEW-MODS-Notch-Currency---Test-2/e4a58a35-d84d-488a-84c8-80b25e3d5795/scratchpad/parcel_preview.png")
print("parcel.png written")
