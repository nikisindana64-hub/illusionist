"""Текстуры: шёлковый платок, монета, стол с напёрстками (верх и стакан)."""
import os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/illusionist/textures")
random.seed(11)
def n(c, k=6):
    d = random.randint(-k, k); return tuple(max(0, min(255, v + d)) for v in c) + (255,)

# платок: развевающаяся красная ткань с белой каймой
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
for y in range(3, 13):
    for x in range(2, 14):
        wave = int(2 * __import__("math").sin(x / 2.0))
        if 3 + wave <= y <= 11 + wave:
            border = y in (3 + wave, 11 + wave)
            px[x, y] = n((240, 236, 226) if border else (186, 18, 34), 5)
d = ImageDraw.Draw(im); d.point((8, 7), fill=(214, 170, 50, 255))
im.save(f"{R}/item/silk_scarf.png")

# монета
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.ellipse([2, 2, 13, 13], fill=(214, 170, 50, 255), outline=(120, 86, 20, 255))
d.ellipse([4, 4, 11, 11], outline=(250, 215, 100, 255))
d.rectangle([6, 5, 9, 6], fill=(120, 86, 20, 255)); d.rectangle([7, 7, 8, 10], fill=(120, 86, 20, 255))
d.point((5, 5), fill=(255, 240, 170, 255))
im.save(f"{R}/item/lucky_coin.png")

# сукно стола (верх)
im = Image.new("RGBA", (16, 16)); px = im.load()
for x in range(16):
    for y in range(16):
        edge = x in (0, 15) or y in (0, 15)
        px[x, y] = n((205, 165, 50), 5) if edge else n((20, 96, 52), 5)
im.save(f"{R}/block/cup_table_top.png")

# стакан: чёрный с красной полосой
im = Image.new("RGBA", (16, 16)); px = im.load()
for x in range(16):
    for y in range(16):
        px[x, y] = n((150, 14, 26), 4) if 6 <= y <= 9 else n((24, 24, 30), 4)
im.save(f"{R}/block/cup.png")
print("ok")
