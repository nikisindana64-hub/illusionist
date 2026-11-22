"""Текстура 64x64 для объёмного цилиндра (TopHatArmorModel). UV-раскладка boxes: top(u+d,v) bottom(u+d+w,v)
right(u,v+d) front(u+d,v+d) left(u+d+w,v+d) back(u+2d+w,v+d)."""
import os, random
from PIL import Image, ImageDraw
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/illusionist/textures/models/armor/top_hat_layer_1.png")
random.seed(3)
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = im.load()

def box(u, v, w, h, d, base, top=None, bottom=None, noise=6):
    top = top or base; bottom = bottom or base
    def fill(x0, y0, ww, hh, col):
        for x in range(x0, x0 + ww):
            for y in range(y0, y0 + hh):
                n = random.randint(-noise, noise)
                px[x, y] = tuple(max(0, min(255, c + n)) for c in col) + (255,)
    fill(u + d, v, w, d, top); fill(u + d + w, v, w, d, bottom)
    fill(u, v + d, d, h, base); fill(u + d, v + d, w, h, base)
    fill(u + d + w, v + d, d, h, base); fill(u + 2 * d + w, v + d, w, h, base)

BLACK = (22, 22, 28); SILK = (38, 38, 48); RED = (150, 14, 26)
box(0, 0, 12, 1, 12, BLACK, top=(30, 30, 38), bottom=(12, 12, 16))     # поля
box(0, 16, 8, 7, 8, BLACK, top=SILK)                                    # тулья
box(0, 32, 8, 2, 8, RED, top=(120, 10, 20), bottom=(120, 10, 20))       # лента
# шёлковый блик на тулье
d = ImageDraw.Draw(im)
for y in range(16 + 8, 16 + 8 + 7):
    px[8 + 1, y] = (60, 60, 74, 255); px[16 + 8 + 1, y] = (60, 60, 74, 255)
# золотая пряжка на ленте (передняя грань: u+d..u+d+w, v+d..)
d.rectangle([8 + 3, 32 + 8, 8 + 4, 32 + 9], fill=(214, 170, 50, 255))
im.save(OUT)
print("ok", OUT)
