"""Генерирует лицо скримера (256x256) и иконку эффекта."""
import os, random
from PIL import Image, ImageDraw, ImageFilter
ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/illusionist/textures")
os.makedirs(f"{ROOT}/gui", exist_ok=True)
random.seed(7)
S = 256
im = Image.new("RGBA", (S, S), (8, 0, 0, 255))
d = ImageDraw.Draw(im)
# фон: тёмно-багровый туннель
for r in range(S, 0, -4):
    c = int(10 + (S - r) * 0.12)
    d.ellipse([S/2 - r, S/2 - r, S/2 + r, S/2 + r], fill=(c, 0, 0, 255))
# цилиндр
d.rectangle([84, 4, 172, 52], fill=(14, 14, 18, 255))
d.rectangle([84, 38, 172, 48], fill=(120, 10, 20, 255))
d.rectangle([56, 52, 200, 62], fill=(14, 14, 18, 255))
# лицо
d.ellipse([60, 50, 196, 232], fill=(232, 228, 216, 255))
# трещины на белой краске
for _ in range(14):
    x, y = random.randint(80, 176), random.randint(70, 210)
    pts = [(x, y)]
    for _ in range(4):
        x += random.randint(-10, 10); y += random.randint(4, 14); pts.append((x, y))
    d.line(pts, fill=(150, 140, 130, 255), width=1)
# глаза — чёрные провалы с красными зрачками
for cx in (102, 154):
    d.ellipse([cx - 22, 96, cx + 22, 150], fill=(0, 0, 0, 255))
    d.ellipse([cx - 5, 116, cx + 5, 128], fill=(255, 20, 20, 255))
# ромбы клоуна/арлекина под глазами
for cx in (102, 154):
    d.polygon([(cx, 150), (cx + 8, 168), (cx, 186), (cx - 8, 168)], fill=(150, 10, 20, 255))
# нос
d.polygon([(128, 140), (120, 168), (136, 168)], fill=(170, 160, 150, 255))
# улыбка от уха до уха
d.chord([76, 150, 180, 234], 10, 170, fill=(10, 0, 0, 255))
for i in range(16):
    x = 82 + i * 6
    d.rectangle([x, 186, x + 3, 198 if i % 2 == 0 else 194], fill=(236, 232, 214, 255))
d.arc([70, 150, 186, 236], 12, 168, fill=(150, 10, 20, 255), width=4)
# швы по краям улыбки
for x in (78, 178):
    for k in range(4):
        d.line([(x - 6, 196 + k * 5), (x + 6, 196 + k * 5 + 2)], fill=(10, 0, 0, 255), width=1)
# кровавые слёзы
for cx in (102, 154):
    d.rectangle([cx - 2, 150, cx, 214], fill=(140, 0, 8, 255))
im = im.filter(ImageFilter.GaussianBlur(0.6))
im.save(f"{ROOT}/gui/screamer_face.png")

# иконка эффекта 18x18
ic = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
di = ImageDraw.Draw(ic)
di.ellipse([2, 1, 15, 16], fill=(232, 228, 216, 255))
di.ellipse([4, 6, 7, 9], fill=(0, 0, 0, 255)); di.ellipse([10, 6, 13, 9], fill=(0, 0, 0, 255))
di.arc([4, 8, 13, 15], 10, 170, fill=(150, 10, 20, 255))
ic.save(f"{ROOT}/mob_effect/jumpscare.png")
print("ok")
