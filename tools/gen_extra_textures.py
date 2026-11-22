#!/usr/bin/env python3
"""Текстуры: монокль, проклятый цилиндр (блок), фантомный занавес, иконка Гипноза.
Запуск: python3 tools/gen_extra_textures.py src/main/resources/assets/illusionist/textures"""
import random
import sys
from PIL import Image

OUT = sys.argv[1]
random.seed(5)
BLACK = (20, 18, 24); DARK = (40, 34, 48); RED = (170, 22, 36); DRED = (110, 12, 24)
GOLD = (214, 170, 52); DGOLD = (150, 112, 30); WHITE = (244, 242, 236); CYAN = (120, 220, 255)


def new(n=16):
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    return im, im.load()


def rect(p, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            p[xx, yy] = (*c, 255)


def outline(im, color=BLACK):
    src = im.copy().load(); p = im.load(); w, h = im.size
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                        p[x, y] = (*color, 255); break


# --- монокль: золотое кольцо, голубая линза, цепочка ---
im, p = new()
ring = [(6, 3), (7, 3), (8, 3), (9, 3), (5, 4), (10, 4), (4, 5), (11, 5), (4, 6), (11, 6),
        (4, 7), (11, 7), (5, 8), (10, 8), (6, 9), (7, 9), (8, 9), (9, 9)]
for x in range(5, 11):
    for y in range(4, 9):
        p[x, y] = (*CYAN, 140 + 0 if False else 255)
for x, y in ring:
    p[x, y] = (*GOLD, 255)
p[6, 5] = (*WHITE, 255); p[7, 5] = (*WHITE, 255); p[6, 6] = (*WHITE, 255)
for i, (x, y) in enumerate([(7, 10), (7, 11), (8, 12), (8, 13), (9, 14)]):
    p[x, y] = (*GOLD, 255) if i % 2 == 0 else (*DGOLD, 255)
outline(im)
im.save(f"{OUT}/item/monocle_of_truth.png")


# --- блок: проклятый цилиндр ---
def tile(base, jit=5):
    im, p = new()
    for y in range(16):
        for x in range(16):
            j = random.randint(-jit, jit)
            p[x, y] = tuple(max(0, min(255, c + j)) for c in base) + (255,)
    return im, p


im, p = tile((22, 20, 28))
rect(p, 0, 9, 16, 3, RED); rect(p, 0, 12, 16, 1, DRED)
rect(p, 6, 9, 4, 3, GOLD); rect(p, 7, 10, 2, 1, BLACK)
rect(p, 0, 13, 16, 3, (30, 26, 38))
im.save(f"{OUT}/block/cursed_top_hat_side.png")
im, p = tile((26, 24, 34))
rect(p, 0, 0, 16, 1, (40, 36, 52)); rect(p, 0, 15, 16, 1, (40, 36, 52))
rect(p, 0, 0, 1, 16, (40, 36, 52)); rect(p, 15, 0, 1, 16, (40, 36, 52))
im.save(f"{OUT}/block/cursed_top_hat_top.png")

# --- блок: фантомный занавес (бархатные складки + золотая бахрома) ---
im, p = new()
for x in range(16):
    fold = [0, 6, 12, 6][x % 4]
    for y in range(16):
        j = random.randint(-4, 4)
        c = (max(0, min(255, 110 + fold * 5 + j)), 10 + fold // 2, 24 + fold // 2)
        p[x, y] = (*c, 255)
for x in range(16):
    p[x, 0] = (*GOLD, 255); p[x, 1] = (*DGOLD, 255)
    if x % 2 == 0:
        p[x, 14] = (*GOLD, 255); p[x, 15] = (*GOLD, 255)
    else:
        p[x, 14] = (*DGOLD, 255)
im.save(f"{OUT}/block/phantom_curtain.png")

# --- иконка эффекта «Гипноз» 18x18: красно-чёрная спираль ---
im, p = new(18)
import math
for y in range(18):
    for x in range(18):
        dx, dy = x - 8.5, y - 8.5
        r = math.hypot(dx, dy)
        if r <= 8.2:
            ang = math.atan2(dy, dx)
            band = int((r * 0.9 + ang * 1.3 / math.pi * 2) ) % 2
            p[x, y] = (*(RED if band == 0 else BLACK), 255)
p[8, 8] = (*WHITE, 255); p[9, 9] = (*WHITE, 255)
im.save(f"{OUT}/mob_effect/hypnosis.png")
print("extra ok")
