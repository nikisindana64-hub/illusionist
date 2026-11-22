#!/usr/bin/env python3
"""
Грубое ортографическое превью модели Фокусника (спереди / сзади / сбоку) по готовым скинам.
Нужно, чтобы проверить UV-раскладку без запуска Minecraft.
Запуск:  python3 tools/preview_magician.py src/main/resources/assets/illusionist/textures/entity preview.png
Размеры/позиции должны совпадать с MagicianModel.java (статичная поза, без анимации).
"""
import math
import sys
from PIL import Image

TEX_DIR = sys.argv[1]
OUT = sys.argv[2]
S = 7  # пикселей экрана на пиксель модели

# (имя UV-куба, x0, y0, z0, w, h, d, inflate)  — координаты модели, y вверх отрицательный
BOXES = [
    ("head", -4, -38, -4, 8, 8, 8, 0),
    ("brim", -6.5, -38.5, -6.5, 13, 1, 13, 0),
    ("crown", -4, -48.5, -4, 8, 10, 8, 0.2),
    ("torso", -4, -30, -2, 8, 14, 4, 0),
    ("ruff", -5, -31, -5, 10, 2, 10, 0),
    ("tails", -4, -18, 2, 8, 12, 1, 0),
    ("r_arm", -7, -30, -1.5, 3, 16, 3, 0),
    ("l_arm", 4, -30, -1.5, 3, 16, 3, 0),
    ("cane", -6, -23, -4, 1, 20, 1, 0),
    ("r_leg", -3.5, -16, -1.5, 3, 16, 3, 0),
    ("l_leg", 0.5, -16, -1.5, 3, 16, 3, 0),
]
UV = {
    "head": (0, 0, 8, 8, 8), "brim": (0, 16, 13, 1, 13), "crown": (0, 30, 8, 10, 8),
    "torso": (32, 30, 8, 14, 4), "r_arm": (56, 30, 3, 16, 3), "l_arm": (68, 30, 3, 16, 3),
    "r_leg": (80, 30, 3, 16, 3), "l_leg": (92, 30, 3, 16, 3), "tails": (104, 30, 8, 12, 1),
    "cane": (0, 50, 1, 20, 1), "card": (10, 50, 3, 4, 1), "ruff": (20, 50, 10, 2, 10),
}


def face_region(name, face):
    u, v, w, h, d = UV[name]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }[face]


def render_view(tex, view):
    W, H = 34 * S, 56 * S
    canvas = Image.new("RGBA", (W, H), (58, 70, 78, 255))
    items = []
    for (n, x0, y0, z0, w, h, d, inf) in BOXES:
        items.append((n, x0 - inf, y0 - inf, z0 - inf, w + 2 * inf, h + 2 * inf, d + 2 * inf))
    # карты: условно по кругу, повёрнутые к зрителю
    for i in range(5):
        a = math.pi * 2 * i / 5
        items.append(("card", math.cos(a) * 10 - 1.5, -22 + (-2 if i % 2 == 0 else 2) - 2,
                      math.sin(a) * 10 - 0.5, 3, 4, 1))

    def depth(it):
        n, x0, y0, z0, w, h, d = it
        zc = z0 + d / 2
        xc = x0 + w / 2
        # расстояние до зрителя: дальние рисуем первыми
        return {"front": zc, "back": -zc, "left": -xc, "right": xc}[view]

    for it in sorted(items, key=depth, reverse=True):
        n, x0, y0, z0, w, h, d = it
        face = view
        reg = face_region(n, face)
        tile = tex.crop((reg[0], reg[1], reg[0] + reg[2], reg[1] + reg[3]))
        if view == "front":
            sx0, sx1 = x0, x0 + w
        elif view == "back":
            sx0, sx1 = -(x0 + w), -x0
        elif view == "left":
            sx0, sx1 = z0, z0 + d
        else:
            sx0, sx1 = -(z0 + d), -z0
        px = int(round((sx0 + 17) * S))
        py = int(round((y0 + 52) * S))
        tw = max(1, int(round((sx1 - sx0) * S)))
        th = max(1, int(round(h * S)))
        tile = tile.resize((tw, th), Image.NEAREST)
        canvas.alpha_composite(tile, (px, py))
    return canvas


def main():
    rows = []
    for name in ("magician", "magician_enraged", "magician_kind"):
        tex = Image.open(f"{TEX_DIR}/{name}.png").convert("RGBA")
        glow = Image.open(f"{TEX_DIR}/{name}_eyes.png").convert("RGBA")
        merged = tex.copy()
        merged.alpha_composite(glow)
        views = [render_view(merged, v) for v in ("front", "back", "left")]
        row = Image.new("RGBA", (sum(v.width for v in views) + 20, views[0].height), (30, 30, 30, 255))
        x = 0
        for v in views:
            row.alpha_composite(v, (x, 0))
            x += v.width + 10
        rows.append(row)
    sheet = Image.new("RGBA", (rows[0].width * len(rows) + 20 * (len(rows) - 1), rows[0].height), (20, 20, 20, 255))
    x = 0
    for r in rows:
        sheet.alpha_composite(r, (x, 0))
        x += r.width + 20
    sheet.save(OUT)
    print("preview ->", OUT, sheet.size)


if __name__ == "__main__":
    main()
