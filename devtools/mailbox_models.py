"""Writes the four mailbox block models.

Four, because the block has two booleans: standing on the ground or hung on a wall, and the
flag up or down. Generated rather than hand-written so the shared parts cannot drift apart,
which is exactly what happens when four files each hold their own copy of the same roof.

The UV cells here must match CELLS in mailbox_texture.py. A block model's UVs are in 0..16
whatever the image resolution, so a 4x4 cell of that grid is 4 UV units wide.
"""
import json
import math
import pathlib

# The roof, as a few numbers rather than as magic in the middle of a list.
RIDGE = 8.0        # the ridge runs up the middle of the block
BODY_HALF = 4.0    # half the body's width, which the slope has to clear
PITCH = 22.5       # the only pitches a block model may use are 22.5 and 45
THICK = 0.9        # how thick the roof slabs are
REACH = 4.55       # how far each slab runs from the ridge, giving a small eave overhang

OUT = pathlib.Path("src/main/resources/assets/notchcurrency/models/block")

# name -> uv box, from the 4x4 grid the texture script draws.
UV = {
    "roof":       [0, 0, 4, 4],
    "roof_end":   [4, 0, 8, 4],
    "flag":       [8, 0, 12, 4],
    "flag_pole":  [12, 0, 16, 4],
    "front":      [0, 4, 4, 8],
    "side":       [4, 4, 8, 8],
    "back":       [8, 4, 12, 8],
    "body_top":   [12, 4, 16, 8],
    "post":       [0, 8, 4, 12],
    "post_top":   [4, 8, 8, 12],
    "base":       [8, 8, 12, 12],
    "body_under": [12, 8, 16, 12],
}

TEX = "#body"


def faces(**by_side):
    """Face map from side -> uv cell name. Anything left out is not drawn."""
    out = {}
    for side, cell in by_side.items():
        out[side] = {"uv": UV[cell], "texture": TEX}
    return out


def box(frm, to, sides, rotation=None):
    el = {"from": frm, "to": to, "faces": faces(**sides)}
    if rotation:
        el["rotation"] = rotation
    return el


# All six sides of a body-shaped box: the front carries the letter slot, the back a hinge.
BODY_SIDES = dict(north="front", south="back", east="side", west="side",
                  up="body_top", down="body_under")
POST_SIDES = dict(north="post", south="post", east="post", west="post",
                  up="post_top", down="post_top")
BASE_SIDES = dict(north="base", south="base", east="base", west="base",
                  up="post_top", down="base")
# The gable end is what a roof slab shows front and back; its long faces are the slope.
ROOF_SIDES = dict(north="roof_end", south="roof_end", east="roof", west="roof",
                  up="roof", down="roof")
FLAG_SIDES = dict(north="flag", south="flag", east="flag", west="flag",
                  up="flag", down="flag")
POLE_SIDES = dict(north="flag_pole", south="flag_pole", east="flag_pole", west="flag_pole",
                  up="flag_pole", down="flag_pole")


def roof_pair(body_top, z0, z1, origin_z):
    """The two slopes of the gable, meeting at a ridge that runs front to back.

    Rotated about Z, so the slopes fall left and right and the ridge points the way the mailbox
    faces.

    The ridge height is not a free choice. A tilted slab is lower at its edges than at its
    middle, so a roof laid flat on the body cuts straight through the body's own top corners and
    the lid pokes out through the slope. The ridge has to sit a further half-width times
    tan(pitch) up, which is what RIDGE_LIFT is. Getting that wrong is invisible in the numbers
    and obvious the moment it is drawn.
    """
    lift = (BODY_HALF + 0) * math.tan(math.radians(PITCH))
    origin_y = body_top + lift + THICK / 2
    y_low = origin_y - THICK / 2
    y_high = origin_y + THICK / 2
    return [
        box([RIDGE - REACH, y_low, z0], [RIDGE + 0.2, y_high, z1], ROOF_SIDES,
            {"origin": [RIDGE, origin_y, origin_z], "axis": "z", "angle": PITCH}),
        box([RIDGE - 0.2, y_low, z0], [RIDGE + REACH, y_high, z1], ROOF_SIDES,
            {"origin": [RIDGE, origin_y, origin_z], "axis": "z", "angle": -PITCH}),
    ]


def gable_fill(body_top, z0, z1):
    """Fills the wedge between the flat top of the body and the underside of the sloping roof.

    A gable roof leaves a triangle of open air at each end, and a block model cannot cut a
    triangle. Two steps get close enough that it reads as the end wall of a little house rather
    than as a hole with the lid visible through it, which is what it looks like without them.

    The heights come from the pitch rather than being typed in, so they stay right if the roof
    ever changes.
    """
    tan = math.tan(math.radians(PITCH))
    steps = []
    for half_width in (3.0, 1.6):
        low = body_top if not steps else steps[-1][1]
        high = body_top + (BODY_HALF - half_width) * tan
        steps.append((half_width, high, low))
    return [
        box([RIDGE - hw, low, z0], [RIDGE + hw, high, z1],
            dict(north="front", south="back", east="side", west="side",
                 up="body_top", down="body_under"))
        for hw, high, low in steps
    ]


def model(elements):
    return {
        "parent": "block/block",
        "textures": {
            "body": "notchcurrency:block/mailbox",
            "particle": "notchcurrency:block/mailbox",
        },
        "elements": elements,
    }


# ---- standing on the ground: a flared foot, a post, the body, the roof ----
FLOOR_BODY_TOP = 12.0
FLOOR = [
    box([5, 0, 5], [11, 1, 11], BASE_SIDES),
    box([6.5, 1, 6.5], [9.5, 7, 9.5], POST_SIDES),
    box([4, 7, 5.5], [12, FLOOR_BODY_TOP, 10.5], BODY_SIDES),
] + gable_fill(FLOOR_BODY_TOP, 5.5, 10.5) + roof_pair(FLOOR_BODY_TOP, 5, 11, 8)

# ---- hung on a wall: the same body and roof, a backplate instead of a post ----
WALL_BODY_TOP = 10.5
WALL = [
    box([4, 5, 14.5], [12, 11, 16], BASE_SIDES),
    box([4, 5.5, 9], [12, WALL_BODY_TOP, 15], BODY_SIDES),
] + gable_fill(WALL_BODY_TOP, 9, 15) + roof_pair(WALL_BODY_TOP, 8.5, 15.5, 12)


def flag(pivot_y, z_mid, raised):
    """The flag and its pin, on the right-hand side as you face the mailbox.

    Down it lies flat along the side, up it stands on end. Both stay under the eaves rather than
    rising past them: the roof overhangs the body, so a flag tall enough to clear it would be a
    flag growing through it. The signal is the change from a flat red bar to an upright one,
    which is legible at a distance without needing the height.
    """
    pole = box([11.95, pivot_y - 0.5, z_mid - 0.3], [12.35, pivot_y + 0.5, z_mid + 0.3], POLE_SIDES)
    if raised:
        panel = box([12.0, pivot_y, z_mid - 0.7], [12.4, pivot_y + 3.6, z_mid + 0.7], FLAG_SIDES)
    else:
        panel = box([12.0, pivot_y, z_mid - 1.9], [12.4, pivot_y + 1.0, z_mid + 1.9], FLAG_SIDES)
    return [pole, panel]


FILES_NOTE = "flag pivots sit low on the body so both positions clear the eaves"


FILES = {
    "mailbox":           FLOOR + flag(7.8, 8.0, raised=False),
    "mailbox_flag":      FLOOR + flag(7.8, 8.0, raised=True),
    "mailbox_wall":      WALL + flag(6.2, 12.0, raised=False),
    "mailbox_wall_flag": WALL + flag(6.2, 12.0, raised=True),
}

for name, elements in FILES.items():
    path = OUT / (name + ".json")
    path.write_text(json.dumps(model(elements), indent=2) + "\n")
    print(f"{name}: {len(elements)} elements")
