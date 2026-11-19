import json, os
from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO, "src/main/resources/assets/notchcurrency")
TEX_OUT = os.path.join(ASSETS, "textures/block/faction_board")
MODEL_OUT = os.path.join(ASSETS, "models/block/faction_board")

COLORS = {
    "none": (226, 222, 210),
    "black": (36, 36, 42),
    "dark_blue": (53, 60, 160),
    "dark_green": (84, 112, 26),
    "dark_aqua": (22, 140, 145),
    "dark_red": (140, 32, 32),
    "dark_purple": (122, 44, 168),
    "gold": (230, 150, 40),
    "gray": (157, 157, 151),
    "dark_gray": (74, 80, 84),
    "blue": (72, 108, 222),
    "green": (110, 182, 30),
    "aqua": (58, 179, 218),
    "red": (176, 46, 38),
    "light_purple": (199, 78, 189),
    "yellow": (236, 196, 52),
    "white": (233, 236, 236),
}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
CREAM = (226, 222, 210)


def tone(c, k):
    if k <= 1:
        return tuple(max(0, min(255, int(v * k))) for v in c)
    return tuple(max(0, min(255, int(v + (255 - v) * (k - 1) * 1.6))) for v in c)


def trim_for(c):
    return (232, 196, 92) if sum(c) < 600 else (150, 120, 60)


def is_coin(c):
    r, g, b = c
    return (r > 180 and g > 120 and b < 120 and r - b > 90) or c in ((0x8a, 0x5a, 0x1e), (0xc4, 0x84, 0x2a))


def board_sheet(base, color):
    im = base.copy()
    px = im.load()
    trim = trim_for(color)
    for x in range(0, 76):
        for y in range(104, 128):
            r, g, b, a = px[x, y]
            if a == 0 or is_coin((r, g, b)) or (r, g, b) == (0, 0, 0):
                continue
            c = color
            if y < 112:
                row = y - 104
                if row == 0:
                    c = tone(color, 1.12)
                elif row == 7:
                    c = tone(color, 0.55)
                elif row in (1, 6):
                    c = trim
                elif x % 6 == 0:
                    c = tone(color, 0.8)
            elif x % 6 == 0:
                c = tone(color, 0.8)
            px[x, y] = c + (255,)
    return im


def banner(color):
    W, H, side = 32, 22, 14
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    edge, dark, light, trim = tone(color, 0.5), tone(color, 0.75), tone(color, 1.12), trim_for(color)
    for x in range(W):
        cx = abs(x - (W - 1) / 2)
        bottom = side + int(round((H - 1 - side) * (1 - cx / ((W - 1) / 2))))
        for y in range(bottom + 1):
            c = color
            if x in (9, 22):
                c = dark
            if x in (10, 23):
                c = light
            if y == 0:
                c = edge
            if y == 2:
                c = trim
            if x == 0 or x == W - 1 or y == bottom:
                c = edge
            px[x, y] = c + (255,)
    mx, my = 16, 9
    for dy in range(-3, 4):
        span = 3 - abs(dy)
        for dx in range(-span - 1, span + 1):
            px[mx + dx, my + dy] = (trim if dx in (-span - 1, span) else light) + (255,)
    return im


def house_banner(color):
    accent = color if sum(color) < 600 else tone(color, 0.62)
    W, H, side = 32, 22, 14
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    edge, dark, light = tone(accent, 0.6), tone(CREAM, 0.82), tone(CREAM, 1.1)
    for x in range(W):
        cx = abs(x - (W - 1) / 2)
        bottom = side + int(round((H - 1 - side) * (1 - cx / ((W - 1) / 2))))
        for y in range(bottom + 1):
            c = CREAM
            if x in (9, 22):
                c = dark
            if x in (10, 23):
                c = light
            if y == 0:
                c = edge
            if y in (2, 3):
                c = accent
            if x == 0 or x == W - 1 or y == bottom:
                c = edge
            px[x, y] = c + (255,)
    mx, my = 16, 10
    for dy in range(-4, 5):
        span = 4 - abs(dy)
        for dx in range(-span - 1, span + 1):
            px[mx + dx, my + dy] = (tone(accent, 0.7) if dx in (-span - 1, span) else accent) + (255,)
    return im


def model(base_model, name, banner_name=None):
    m = json.loads(json.dumps(base_model))
    m["textures"] = {
        "0": f"notchcurrency:block/faction_board/{name}",
        "1": f"notchcurrency:block/faction_board/{banner_name or 'banner_' + name}",
        "particle": f"notchcurrency:block/faction_board/{name}",
    }
    m["elements"].append({
        "name": "banner rod", "from": [3.5, 5.25, 7.5], "to": [12.5, 6, 8.5],
        "faces": {f: {"uv": [9.5, 13, 10.5, 14], "texture": "#0"} for f in ("north", "east", "south", "west", "down")},
    })
    m["elements"].append({
        "name": "banner", "from": [4, 0.5, 7.85], "to": [12, 6, 8.15],
        "faces": {
            "north": {"uv": [0, 0, 16, 11], "texture": "#1"},
            "south": {"uv": [16, 0, 0, 11], "texture": "#1"},
        },
    })
    return m


def blockstate():
    variants = {}
    for facing, y in FACINGS.items():
        for name in COLORS:
            for house in (False, True):
                v = {"model": f"notchcurrency:block/faction_board/{'house_' + name if house else name}"}
                if y:
                    v["y"] = y
                variants[f"color={name},facing={facing},half=lower,house={str(house).lower()}"] = v
    for facing, y in FACINGS.items():
        v = {"model": "notchcurrency:block/bounty_board_upper"}
        if y:
            v["y"] = y
        variants[f"facing={facing},half=upper"] = v
    return {"variants": variants}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    os.makedirs(TEX_OUT, exist_ok=True)
    base = Image.open(os.path.join(ASSETS, "textures/block/bounty_board.png")).convert("RGBA")
    base_model = json.load(open(os.path.join(ASSETS, "models/block/bounty_board.json")))
    for name, color in COLORS.items():
        board_sheet(base, color).save(os.path.join(TEX_OUT, f"{name}.png"))
        banner(color).save(os.path.join(TEX_OUT, f"banner_{name}.png"))
        write_json(os.path.join(MODEL_OUT, f"{name}.json"), model(base_model, name))
        house_banner(color).save(os.path.join(TEX_OUT, f"banner_house_{name}.png"))
        write_json(os.path.join(MODEL_OUT, f"house_{name}.json"), model(base_model, name, f"banner_house_{name}"))
    write_json(os.path.join(ASSETS, "blockstates/faction_board.json"), blockstate())
    write_json(os.path.join(ASSETS, "models/item/faction_board.json"), {"parent": "notchcurrency:block/faction_board/none"})
    write_json(os.path.join(ASSETS, "items/faction_board.json"),
               {"model": {"type": "minecraft:model", "model": "notchcurrency:item/faction_board"}})
    print(f"wrote {len(COLORS)} colors")


main()
