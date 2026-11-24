"""Текстуры декоративных блоков и колоды карт."""
import os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/illusionist/textures")
random.seed(5)
def noise(c, n=8): 
    k = random.randint(-n, n); return tuple(max(0, min(255, v + k)) for v in c) + (255,)

# полосатый цирковой блок
im = Image.new("RGBA", (16, 16)); px = im.load()
for x in range(16):
    for y in range(16):
        px[x, y] = noise((190, 20, 36) if (x // 2) % 2 == 0 else (236, 232, 222), 6)
im.save(f"{R}/block/circus_block.png")

# бархат с золотой каймой
im = Image.new("RGBA", (16, 16)); px = im.load()
for x in range(16):
    for y in range(16):
        edge = x in (0, 15) or y in (0, 15)
        px[x, y] = noise((205, 165, 50), 5) if edge else noise((120, 6, 22) if (x + y) % 4 else (100, 4, 18), 5)
d = ImageDraw.Draw(im)
for x in range(3, 14, 3):
    d.line([(x, 1), (x, 14)], fill=(88, 3, 14, 255))
im.save(f"{R}/block/stage_velvet.png")

# прожектор
im = Image.new("RGBA", (16, 16), (40, 40, 46, 255)); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 15, 15], outline=(20, 20, 24, 255))
d.rectangle([2, 2, 13, 13], fill=(70, 70, 80, 255))
d.ellipse([3, 3, 12, 12], fill=(255, 236, 150, 255)); d.ellipse([5, 5, 10, 10], fill=(255, 255, 230, 255))
for p in ((1, 1), (14, 1), (1, 14), (14, 14)): d.point(p, fill=(150, 150, 160, 255))
im.save(f"{R}/block/stage_spotlight.png")

# колода карт (предмет)
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
for i, off in enumerate((3, 2, 1, 0)):
    d.rectangle([3 + off, 2 + off, 11 + off, 13 + off], fill=(236, 232, 222, 255), outline=(30, 30, 36, 255))
d.rectangle([3, 2, 11, 13], fill=(150, 14, 26, 255), outline=(20, 20, 24, 255))
d.rectangle([5, 4, 9, 11], outline=(214, 170, 50, 255))
d.point((7, 7), fill=(214, 170, 50, 255)); d.point((7, 6), fill=(214, 170, 50, 255)); d.point((7, 8), fill=(214, 170, 50, 255))
im.save(f"{R}/item/card_deck.png")
print("ok")
