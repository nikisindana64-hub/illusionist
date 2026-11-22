#!/usr/bin/env python3
"""
Генератор скинов Фокусника (128x128) + светящихся слоёв.
Запуск:  python3 tools/gen_magician_skins.py src/main/resources/assets/illusionist/textures/entity

Раскладка UV (u,v) должна совпадать с MagicianModel.java:
  голова 0,0 | поля 0,16 | тулья 0,30 | торс 32,30 | пр.рука 56,30 | лев.рука 68,30
  пр.нога 80,30 | лев.нога 92,30 | хвосты 104,30 | трость 0,50 | карта 10,50 | жабо 20,50
"""
import random
import sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "."

PALETTES = {
    "magician": dict(
        base=(22, 20, 28), base2=(34, 30, 42), accent=(150, 18, 32), accent2=(100, 10, 22),
        trim=(200, 158, 48), mask=(238, 234, 226), mask_sh=(210, 204, 196), hair=(14, 12, 18),
        shirt=(240, 240, 244), line=(14, 12, 18),
        eye=(255, 40, 24), eye_core=(255, 226, 120), smile=(160, 20, 30), glow=(255, 40, 24)),
    "magician_enraged": dict(
        base=(16, 12, 16), base2=(30, 20, 24), accent=(190, 30, 20), accent2=(120, 12, 10),
        trim=(230, 120, 30), mask=(226, 220, 210), mask_sh=(190, 180, 170), hair=(10, 8, 10),
        shirt=(220, 214, 206), line=(10, 8, 10),
        eye=(255, 110, 20), eye_core=(255, 240, 160), smile=(200, 30, 20), glow=(255, 120, 20)),
    "magician_kind": dict(
        base=(34, 48, 112), base2=(46, 64, 140), accent=(24, 160, 132), accent2=(16, 110, 92),
        trim=(244, 200, 64), mask=(250, 238, 214), mask_sh=(226, 210, 182), hair=(60, 40, 30),
        shirt=(252, 250, 244), line=(30, 40, 90),
        eye=(60, 220, 255), eye_core=(220, 255, 255), smile=(214, 70, 70), glow=(60, 220, 255)),
}


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


# (имя, u, v, w, h, d)
BOXES = {
    "head": (0, 0, 8, 8, 8),
    "brim": (0, 16, 13, 1, 13),
    "crown": (0, 30, 8, 10, 8),
    "torso": (32, 30, 8, 14, 4),
    "r_arm": (56, 30, 3, 16, 3),
    "l_arm": (68, 30, 3, 16, 3),
    "r_leg": (80, 30, 3, 16, 3),
    "l_leg": (92, 30, 3, 16, 3),
    "tails": (104, 30, 8, 12, 1),
    "cane": (0, 50, 1, 20, 1),
    "card": (10, 50, 3, 4, 1),
    "ruff": (20, 50, 10, 2, 10),
}


class Painter:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
        self.px = self.img.load()
        self.glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
        self.gx = self.glow.load()

    def put(self, x, y, c):
        self.px[x, y] = (c[0], c[1], c[2], 255)

    def gput(self, x, y, c):
        self.gx[x, y] = (c[0], c[1], c[2], 255)

    def fill(self, rect, c, jit=0):
        x, y, w, h = rect
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                j = self.rng.randint(-jit, jit) if jit else 0
                self.put(xx, yy, tuple(max(0, min(255, k + j)) for k in c))

    def rows(self, rect, y0, y1, c, jit=0):
        x, y, w, h = rect
        self.fill((x, y + y0, w, y1 - y0), c, jit)

    def cols(self, rect, x0, x1, c, jit=0):
        x, y, w, h = rect
        self.fill((x + x0, y, x1 - x0, h), c, jit)


def paint(name, P, seed):
    p = Painter(seed)
    F = {k: faces(*v) for k, v in BOXES.items()}
    enraged = name == "magician_enraged"
    kind = name == "magician_kind"

    # ---------------- голова: фарфоровая маска ----------------
    h = F["head"]
    for f in h.values():
        p.fill(f, P["hair"], 2)
    front = h["front"]
    p.fill(front, P["mask"], 3)
    fx, fy = front[0], front[1]
    for side in ("right", "left"):
        sx, sy, sw, sh = h[side]
        p.fill((sx + 2, sy, sw - 2, sh), P["mask"], 3) if side == "right" else p.fill((sx, sy, sw - 2, sh), P["mask"], 3)
        p.fill((sx + (3 if side == "right" else 0), sy + 4, 2, 2), P["mask_sh"])
    p.fill((fx, fy, 8, 1), P["hair"])                      # чёлка под шляпой
    # брови
    for x in (1, 2, 5, 6):
        p.put(fx + x, fy + 2, P["line"])
    if kind:
        p.put(fx + 1, fy + 1, P["line"]); p.put(fx + 6, fy + 1, P["line"])
    # глазницы
    for ex in (1, 5):
        p.fill((fx + ex, fy + 3, 2, 2), P["line"])
    # нос
    p.put(fx + 3, fy + 5, P["mask_sh"]); p.put(fx + 4, fy + 5, P["mask_sh"])
    # слеза-ромб под левым глазом (арлекин)
    p.put(fx + 1, fy + 5, P["accent"]); p.put(fx + 1, fy + 6, P["accent"])
    # румяна
    p.put(fx + 6, fy + 5, P["accent"]) if not kind else p.put(fx + 6, fy + 5, (240, 150, 140))
    # улыбка
    if kind:
        for x in range(2, 6):
            p.put(fx + x, fy + 6, P["smile"])
        p.put(fx + 1, fy + 5, P["smile"]); p.put(fx + 6, fy + 5, P["smile"])
        p.put(fx + 3, fy + 7, P["smile"]); p.put(fx + 4, fy + 7, P["smile"])
    else:
        for x in range(0, 8):
            p.put(fx + x, fy + 6, P["smile"])
        p.put(fx, fy + 5, P["smile"]); p.put(fx + 7, fy + 5, P["smile"])
        for x in (1, 3, 5):                                # зубы
            p.put(fx + x, fy + 6, P["mask"])
        for x in (2, 4, 6):
            p.put(fx + x, fy + 7, P["line"])
    if enraged:                                            # трещины маски
        for (x, y) in ((5, 1), (5, 2), (4, 2), (4, 3), (3, 3), (6, 0), (7, 4), (7, 5), (2, 0), (2, 1)):
            p.put(fx + x, fy + y, P["line"])
            p.gput(fx + x, fy + y, P["glow"])
    # свечение глаз
    for ex in (1, 5):
        w = 2
        for yy in (3, 4):
            for xx in range(ex, ex + w):
                p.gput(fx + xx, fy + yy, P["eye"])
        p.gput(fx + ex + 1, fy + 3, P["eye_core"])
    if enraged:
        p.gput(fx + 0, fy + 3, P["eye"]); p.gput(fx + 7, fy + 3, P["eye"])
    if kind:
        for x in range(2, 6):
            p.gput(fx + x, fy + 6, P["trim"])

    # ---------------- поля шляпы ----------------
    for k, f in F["brim"].items():
        p.fill(f, P["base"], 2)
    t = F["brim"]["top"]
    p.fill((t[0] + 1, t[1] + 1, t[2] - 2, t[3] - 2), P["base2"], 2)
    for k in ("right", "front", "left", "back"):
        p.fill(F["brim"][k], P["accent"] if not kind else P["trim"])

    # ---------------- тулья ----------------
    c = F["crown"]
    for k, f in c.items():
        p.fill(f, P["base"], 2)
    for k in ("right", "front", "left", "back"):
        p.rows(c[k], 7, 9, P["accent"], 2)                 # лента
        p.rows(c[k], 9, 10, P["accent2"])
    cf = c["front"]
    p.fill((cf[0] + 3, cf[1] + 7, 2, 2), P["trim"])        # пряжка
    p.put(cf[0] + 3, cf[1] + 8, P["base"]); p.put(cf[0] + 4, cf[1] + 8, P["base"])
    p.fill((cf[0] + 6, cf[1] + 4, 2, 3), P["shirt"])      # карта за лентой
    p.put(cf[0] + 6, cf[1] + 5, P["accent"])
    p.fill((c["top"][0] + 1, c["top"][1] + 1, 6, 6), P["base2"], 2)
    if enraged:
        for x in (1, 2, 5):
            p.put(cf[0] + x, cf[1] + 7, P["trim"]); p.gput(cf[0] + x, cf[1] + 7, P["glow"])

    # ---------------- жабо ----------------
    r = F["ruff"]
    p.fill(r["top"], P["shirt"], 4)
    p.fill(r["bottom"], P["base2"])
    for k in ("right", "front", "left", "back"):
        x, y, w, hh = r[k]
        for i in range(w):
            col = P["shirt"] if (i // 2) % 2 == 0 else P["accent"]
            for j in range(hh):
                p.put(x + i, y + j, col)

    # ---------------- торс (фрак) ----------------
    tq = F["torso"]
    for k, f in tq.items():
        p.fill(f, P["base"], 3)
    tf = tq["front"]
    p.fill((tf[0] + 3, tf[1] + 1, 2, 8), P["shirt"])      # манишка
    p.fill((tf[0] + 2, tf[1] + 1, 1, 6), P["accent"])     # лацканы
    p.fill((tf[0] + 5, tf[1] + 1, 1, 6), P["accent"])
    p.put(tf[0] + 3, tf[1] + 3, P["trim"]); p.put(tf[0] + 4, tf[1] + 6, P["trim"])
    p.fill((tf[0], tf[1] + 13, 8, 1), P["accent"])         # кайма
    p.fill((tf[0] + 3, tf[1] + 9, 2, 5), P["base"])        # разрез фрака
    tb = tq["back"]
    p.fill((tb[0] + 3, tb[1], 2, 14), P["accent2"])
    for k in ("right", "left"):
        p.fill((tq[k][0], tq[k][1] + 13, tq[k][2], 1), P["accent"])

    # ---------------- хвосты ----------------
    tl = F["tails"]
    for k, f in tl.items():
        p.fill(f, P["base"], 2)
    p.fill(tl["front"], P["accent"], 3)
    p.fill((tl["back"][0] + 3, tl["back"][1], 2, 12), P["accent2"])
    p.fill((tl["back"][0], tl["back"][1] + 11, 8, 1), P["trim"])

    # ---------------- руки ----------------
    for name_ in ("r_arm", "l_arm"):
        a = F[name_]
        for k, f in a.items():
            p.fill(f, P["base"], 3)
        for k in ("right", "front", "left", "back"):
            p.rows(a[k], 11, 12, P["accent"])
            p.rows(a[k], 12, 16, P["shirt"])
        p.fill(a["bottom"], P["shirt"])
        p.fill(a["top"], P["base"])

    # ---------------- ноги ----------------
    for name_ in ("r_leg", "l_leg"):
        lg = F[name_]
        for k, f in lg.items():
            p.fill(f, P["base"], 3)
        for k in ("right", "front", "left", "back"):
            p.rows(lg[k], 11, 13, P["shirt"])               # гетры
            p.rows(lg[k], 13, 16, P["base2"])               # лаковые туфли
        p.fill((lg["front"][0], lg["front"][1], 1, 11), P["base2"])
        p.fill(lg["bottom"], P["base2"])

    # ---------------- трость ----------------
    cn = F["cane"]
    for k, f in cn.items():
        p.fill(f, P["line"] if not kind else P["base2"])
    for k in ("right", "front", "left", "back"):
        x, y, w, hh = cn[k]
        p.fill((x, y, w, 3), P["trim"])
        p.fill((x, y + 10, w, 1), P["trim"])
        p.fill((x, y + hh - 2, w, 2), P["trim"])
    p.fill(cn["top"], P["trim"]); p.fill(cn["bottom"], P["trim"])

    # ---------------- карта ----------------
    cd = F["card"]
    for k, f in cd.items():
        p.fill(f, P["shirt"])
    cfr = cd["front"]
    p.fill(cfr, P["shirt"])
    p.put(cfr[0] + 1, cfr[1] + 1, P["accent"]); p.put(cfr[0], cfr[1] + 2, P["accent"])
    p.put(cfr[0] + 2, cfr[1] + 2, P["accent"]); p.put(cfr[0] + 1, cfr[1] + 2, P["accent"])
    cbk = cd["back"]
    p.fill(cbk, P["accent2"])
    p.put(cbk[0] + 1, cbk[1] + 1, P["trim"]); p.put(cbk[0] + 1, cbk[1] + 2, P["trim"])
    p.put(cbk[0], cbk[1] + 1, P["trim"]); p.put(cbk[0] + 2, cbk[1] + 2, P["trim"])
    # свечение карт
    p.gput(cfr[0] + 1, cfr[1] + 1, P["glow"]); p.gput(cfr[0] + 1, cfr[1] + 2, P["glow"])
    p.gput(cbk[0] + 1, cbk[1] + 1, P["trim"]); p.gput(cbk[0] + 1, cbk[1] + 2, P["trim"])

    return p


def main():
    for i, (name, pal) in enumerate(PALETTES.items()):
        p = paint(name, pal, 100 + i)
        p.img.save(f"{OUT}/{name}.png")
        p.glow.save(f"{OUT}/{name}_eyes.png")
    print("skins ok ->", OUT)


if __name__ == "__main__":
    main()
