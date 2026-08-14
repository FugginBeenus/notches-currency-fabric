"""Draws the mailbox block sheet.

A 128x128 atlas laid out as a 4x4 grid of cells. A block model's UVs are always in 0..16
space whatever the image resolution, so each cell is 4x4 in UV terms and 32x32 in pixels.
The model refers to the cells by the UV boxes named in CELLS below; keep the two in step.

Kept so the art can be nudged without redrawing it, the same way the coin and parcel are.
"""
from PIL import Image

SHEET = 128
CELL_PX = 32          # 4 UV units at 8 pixels each

# Cell -> (column, row) in the 4x4 grid. The model's uv boxes are these times 4.
CELLS = {
    "roof":       (0, 0),   # corrugated slope, ridge along the top edge
    "roof_end":   (1, 0),   # the gable end seen from front or back
    "flag":       (2, 0),   # the red flag panel
    "flag_pole":  (3, 0),
    "front":      (0, 1),   # the face with the letter slot
    "side":       (1, 1),
    "back":       (2, 1),
    "body_top":   (3, 1),
    "post":       (0, 2),   # the post, grain running up
    "post_top":   (1, 2),
    "base":       (2, 2),   # the flared foot where the post meets the ground
    "body_under": (3, 2),
}

# Warm painted wood, a little cooler and greyer than plain oak so it reads as a made thing
# rather than a log somebody stood up.
WOOD_LIT   = (176, 138, 96)
WOOD       = (150, 114, 76)
WOOD_DARK  = (118, 88, 58)
WOOD_LINE  = (96, 70, 44)

ROOF_LIT   = (108, 116, 124)
ROOF       = (86, 94, 102)
ROOF_DARK  = (66, 73, 80)
ROOF_LINE  = (52, 58, 64)

FLAG_LIT   = (198, 74, 62)
FLAG       = (168, 54, 46)
FLAG_DARK  = (128, 38, 32)

SLOT_DARK  = (44, 36, 30)
BRASS      = (198, 160, 78)

img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
px = img.load()


def origin(name):
    col, row = CELLS[name]
    return col * CELL_PX, row * CELL_PX


def rect(name, x0, y0, x1, y1, colour):
    """A filled box in cell-local pixel coordinates, ends inclusive."""
    ox, oy = origin(name)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < CELL_PX and 0 <= y < CELL_PX:
                px[ox + x, oy + y] = colour


def panel(name, base, lit, dark, line):
    """Fills a cell and puts a light edge on top and left, a dark one on bottom and right.

    The bevel is what stops a flat colour reading as a sticker. Every face here gets one, so
    the block keeps its shape when the light is flat.
    """
    rect(name, 0, 0, CELL_PX - 1, CELL_PX - 1, base)
    rect(name, 0, 0, CELL_PX - 1, 1, lit)
    rect(name, 0, 0, 1, CELL_PX - 1, lit)
    rect(name, 0, CELL_PX - 2, CELL_PX - 1, CELL_PX - 1, dark)
    rect(name, CELL_PX - 2, 0, CELL_PX - 1, CELL_PX - 1, dark)
    rect(name, 0, 0, CELL_PX - 1, 0, line)
    rect(name, 0, CELL_PX - 1, CELL_PX - 1, CELL_PX - 1, line)
    rect(name, 0, 0, 0, CELL_PX - 1, line)
    rect(name, CELL_PX - 1, 0, CELL_PX - 1, CELL_PX - 1, line)


def grain(name, step=6, colour=WOOD_LINE, vertical=False):
    """Plank seams. Vertical for the post, horizontal for the body."""
    for i in range(step, CELL_PX - 2, step):
        if vertical:
            rect(name, i, 2, i, CELL_PX - 3, colour)
        else:
            rect(name, 2, i, CELL_PX - 3, i, colour)


# ---- roof: corrugation running down the slope, so the ridge reads as a ridge ----
panel("roof", ROOF, ROOF_LIT, ROOF_DARK, ROOF_LINE)
for x in range(3, CELL_PX - 2, 4):
    rect("roof", x, 1, x, CELL_PX - 2, ROOF_DARK)
    rect("roof", x + 1, 1, x + 1, CELL_PX - 2, ROOF_LIT)
# The ridge itself, brightest along the top edge where the two slopes meet.
rect("roof", 0, 0, CELL_PX - 1, 2, ROOF_LIT)
rect("roof", 0, 0, CELL_PX - 1, 0, ROOF_LINE)

# ---- the gable end, a triangle of roof over a sliver of body ----
panel("roof_end", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
for y in range(1, 15):
    half = int((y / 15.0) * 15)
    rect("roof_end", 15 - half, y, 16 + half, y, ROOF)
    rect("roof_end", 15 - half, y, 15 - half + 1, y, ROOF_LIT)
    rect("roof_end", 16 + half - 1, y, 16 + half, y, ROOF_DARK)
rect("roof_end", 0, 15, CELL_PX - 1, 16, ROOF_LINE)

# ---- flag ----
panel("flag", FLAG, FLAG_LIT, FLAG_DARK, (92, 26, 22))
rect("flag", 4, 12, CELL_PX - 5, 13, FLAG_LIT)
rect("flag", 4, 18, CELL_PX - 5, 19, FLAG_DARK)
panel("flag_pole", (72, 66, 62), (96, 90, 86), (52, 48, 44), (36, 33, 30))

# ---- body front: the letter slot and a little brass handle ----
panel("front", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
grain("front", step=8)
rect("front", 6, 11, CELL_PX - 7, 15, SLOT_DARK)
rect("front", 6, 10, CELL_PX - 7, 10, WOOD_DARK)
rect("front", 6, 16, CELL_PX - 7, 16, WOOD_LIT)
rect("front", 13, 20, 18, 22, BRASS)
rect("front", 13, 20, 18, 20, (232, 200, 128))

# ---- the other body faces ----
panel("side", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
grain("side", step=8)
panel("back", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
grain("back", step=8)
rect("back", 12, 12, CELL_PX - 13, 20, WOOD_DARK)   # a hinge plate, so the back is not blank
panel("body_top", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
panel("body_under", WOOD_DARK, WOOD, (96, 70, 44), (78, 56, 36))

# ---- post ----
panel("post", WOOD, WOOD_LIT, WOOD_DARK, WOOD_LINE)
grain("post", step=7, vertical=True)
panel("post_top", WOOD_LIT, (200, 162, 118), WOOD, WOOD_LINE)
panel("base", WOOD_DARK, WOOD, (96, 70, 44), (78, 56, 36))
grain("base", step=8)

img.save("src/main/resources/assets/notchcurrency/textures/block/mailbox.png")

# The flag is its own texture because the model tints it separately from the body.
flag = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
img_flag_src = img.crop((CELLS["flag"][0] * CELL_PX, CELLS["flag"][1] * CELL_PX,
                         CELLS["flag"][0] * CELL_PX + CELL_PX, CELLS["flag"][1] * CELL_PX + CELL_PX))
flag.paste(img_flag_src, (0, 0))
flag.save("src/main/resources/assets/notchcurrency/textures/block/mailbox_flag.png")

img.resize((512, 512), Image.NEAREST).save(
    "/private/tmp/claude-503/-Users-bjpelicano-Desktop-NEW-MODS-Notch-Currency---Test-2/"
    "e4a58a35-d84d-488a-84c8-80b25e3d5795/scratchpad/mailbox_sheet.png")
print("mailbox.png and mailbox_flag.png written")
