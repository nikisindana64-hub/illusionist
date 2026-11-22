"""Генерирует структуры: Цирк-шапито (circus_tent) и Фургон фокусника (circus_wagon) в формате structure NBT 1.21."""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
from nbtlite import write_nbt, Byte

DATA = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/data/illusionist")
os.makedirs(f"{DATA}/structure", exist_ok=True)
DATA_VERSION = 3955  # 1.21.1

class Grid:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz); self.b = {}
    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.b[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)
    def has(self, x, y, z): return (x, y, z) in self.b
    def save(self, path):
        palette, index, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.b.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                e = {"Name": name}
                if props: e["Properties"] = {k: str(v) for k, v in props}
                palette.append(e)
            blk = {"pos": [x, y, z], "state": index[key]}
            if nbt: blk["nbt"] = nbt
            blocks.append(blk)
        write_nbt(path, {"DataVersion": DATA_VERSION, "size": list(self.size),
                         "palette": palette, "blocks": blocks, "entities": []})
        print(path, len(blocks), "blocks")

def facing_to(dx, dz):
    if abs(dx) > abs(dz): return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"

CHEST_NBT = lambda: {"id": "minecraft:chest", "LootTable": "illusionist:chests/circus_tent"}

# ============================================================ ШАПИТО
def tent():
    N, FLOOR = 25, 3
    g = Grid(N, 24, N)
    c = 12.0
    rnd = random.Random(1337)

    def polar(x, z):
        dx, dz = x - c, z - c
        return math.hypot(dx, dz), math.atan2(dz, dx)

    def stripe(ang, n=20):
        return "minecraft:red_wool" if int((ang + math.pi) / (2 * math.pi) * n) % 2 == 0 else "minecraft:white_wool"

    def roof_y(r):
        return FLOOR + 6 + (10.5 - r) * 0.85

    for x in range(N):
        for z in range(N):
            r, a = polar(x, z)
            # фундамент (уходит в землю)
            if r <= 11.5:
                g.set(x, 0, z, "minecraft:dirt"); g.set(x, 1, z, "minecraft:dirt")
                g.set(x, 2, z, "minecraft:coarse_dirt")
            if r > 10.5 and r <= 11.5:
                g.set(x, FLOOR, z, "minecraft:coarse_dirt")
            # пол
            if r <= 9.5:
                if r <= 3.5:
                    g.set(x, FLOOR, z, "minecraft:dark_oak_planks")
                elif r <= 6.4:
                    g.set(x, FLOOR, z, "minecraft:red_terracotta" if (x + z) % 2 == 0 else "minecraft:black_terracotta")
                else:
                    g.set(x, FLOOR, z, "minecraft:spruce_planks")
            # стены
            if 9.5 <= r < 10.5:
                for y in range(FLOOR + 1, FLOOR + 6):
                    g.set(x, y, z, stripe(a))
            # купол (оболочка) вместе со свесом
            if r <= 11.3:
                lo, hi = round(roof_y(r + 0.5)), round(roof_y(r - 0.5))
                for y in range(lo, hi + 1):
                    col = "minecraft:black_wool" if r > 10.6 else stripe(a)
                    g.set(x, y, z, col)
            # воздух внутри
            if r < 9.5:
                for y in range(FLOOR + 1, round(roof_y(r + 0.5))):
                    if not g.has(x, y, z):
                        g.set(x, y, z, "minecraft:air")

    # сцена
    for x in range(N):
        for z in range(N):
            r, a = polar(x, z)
            if r <= 3.5:
                g.set(x, FLOOR + 1, z, "minecraft:dark_oak_planks")
    # зрительские ряды
    for x in range(N):
        for z in range(N):
            r, a = polar(x, z)
            aisle = z > 12 and abs(x - 12) <= 1
            if aisle: continue
            f = facing_to(c - x, c - z)
            if 6.5 < r <= 7.5:
                g.set(x, FLOOR + 1, z, "minecraft:dark_oak_stairs", {"facing": f, "half": "bottom"})
                if rnd.random() < 0.28:
                    g.set(x, FLOOR + 2, z, "minecraft:carved_pumpkin", {"facing": f})
            elif 7.5 < r <= 8.7:
                g.set(x, FLOOR + 1, z, "minecraft:dark_oak_planks")
                g.set(x, FLOOR + 2, z, "minecraft:dark_oak_stairs", {"facing": f, "half": "bottom"})
                if rnd.random() < 0.30:
                    g.set(x, FLOOR + 3, z, "minecraft:carved_pumpkin", {"facing": f})
                elif rnd.random() < 0.10:
                    g.set(x, FLOOR + 3, z, "minecraft:candle", {"candles": 3, "lit": "true"})
    # вход
    for x in (11, 12, 13):
        for z in range(N):
            r, a = polar(x, z)
            if z >= 18 and 9.0 <= r < 11.0:
                for y in range(FLOOR + 1, FLOOR + 5):
                    g.set(x, y, z, "minecraft:air")
    for z in range(16, 24):
        for x in (11, 12, 13):
            g.set(x, FLOOR + 1, z, "minecraft:red_carpet")
    for x in (10, 14):
        g.set(x, FLOOR + 1, 22, "minecraft:spruce_fence")
        g.set(x, FLOOR + 2, 22, "minecraft:spruce_fence")
        g.set(x, FLOOR + 3, 22, "minecraft:lantern")
    # артисты сцены
    g.set(12, FLOOR + 2, 12, "illusionist:cursed_top_hat")
    for dx, dz in ((2, 2), (-2, 2), (2, -2), (-2, -2)):
        g.set(12 + dx, FLOOR + 2, 12 + dz, "illusionist:trick_box")
    # закулисье (север): сундуки и бочка
    g.set(12, FLOOR + 1, 3, "minecraft:chest", {"facing": "south", "type": "single"}, CHEST_NBT())
    g.set(9, FLOOR + 1, 4, "minecraft:chest", {"facing": "south", "type": "single"}, CHEST_NBT())
    g.set(15, FLOOR + 1, 4, "minecraft:barrel", {"facing": "up"})
    # подвесные души-фонари
    for k in range(6):
        ang = k * math.pi / 3 + 0.4
        x, z = round(c + math.cos(ang) * 5), round(c + math.sin(ang) * 5)
        rr, _ = polar(x, z)
        top = round(roof_y(rr + 0.5))
        for y in range(FLOOR + 7, top):
            g.set(x, y, z, "minecraft:chain", {"axis": "y"})
        g.set(x, FLOOR + 6, z, "minecraft:soul_lantern", {"hanging": "true"})
    # шест с флагом на макушке
    peak = round(roof_y(0.0 - 0.5))
    for y in range(peak + 1, peak + 4):
        g.set(12, y, 12, "minecraft:spruce_fence")
    g.set(13, peak + 3, 12, "minecraft:red_wool"); g.set(14, peak + 3, 12, "minecraft:red_wool")
    # паутина по углам рядов
    for _ in range(26):
        x, z = rnd.randrange(N), rnd.randrange(N)
        r, a = polar(x, z)
        if 5 < r < 9.3:
            y = FLOOR + 5 + rnd.randint(0, 1)
            if g.b.get((x, y, z), ("minecraft:air",))[0] == "minecraft:air":
                g.set(x, y, z, "minecraft:cobweb")
    if os.environ.get("DEBUG_LAYERS"):
        sym = {"minecraft:air": ".", "minecraft:dark_oak_stairs": "S", "minecraft:carved_pumpkin": "P",
               "minecraft:chest": "C", "illusionist:trick_box": "T", "illusionist:cursed_top_hat": "H",
               "minecraft:red_carpet": "=", "minecraft:red_wool": "R", "minecraft:white_wool": "W",
               "minecraft:chain": "|", "minecraft:soul_lantern": "L", "minecraft:dark_oak_planks": "#"}
        for y in (FLOOR, FLOOR + 1, FLOOR + 2, FLOOR + 6, FLOOR + 9):
            print("y =", y)
            for z in range(N):
                print("".join(sym.get(g.b.get((x, y, z), ("",))[0], "?" if (x, y, z) in g.b else " ") for x in range(N)))
    g.save(f"{DATA}/structure/circus_tent.nbt")

# ============================================================ ФУРГОН
def wagon():
    SX, SY, SZ = 9, 9, 14
    g = Grid(SX, SY, SZ)
    # колёса (спицы) у x=0 и x=8
    for wx in (0, 8):
        for wz in (4, 10):
            cy = 2
            g.set(wx, cy, wz, "minecraft:dark_oak_planks")
            for d in (1, 2):
                for dy, dz in ((d, 0), (-d, 0), (0, d), (0, -d)):
                    g.set(wx, cy + dy, wz + dz, "minecraft:dark_oak_fence")
    # пол и стены
    for x in range(1, 8):
        for z in range(1, 13):
            g.set(x, 2, z, "minecraft:dark_oak_planks")
            for y in (3, 4, 5):
                edge = x in (1, 7) or z in (1, 12)
                g.set(x, y, z, "minecraft:spruce_planks" if edge else "minecraft:air")
    # крыша в полоску с навесом
    for x in range(0, 9):
        for z in range(0, 14):
            g.set(x, 6, z, "minecraft:red_wool" if z % 2 == 0 else "minecraft:white_wool")
    for x in range(1, 8):
        for z in range(1, 13):
            if (x in (1, 7) or z in (1, 12)):
                g.set(x, 6, z, "minecraft:red_wool" if z % 2 == 0 else "minecraft:white_wool")
    # окна
    for x in (1, 7):
        for z in (5, 6, 9, 10):
            g.set(x, 4, z, "minecraft:glass_pane")
    # вход спереди (z=1) и ступеньки
    for x in (3, 4, 5):
        for y in (3, 4):
            g.set(x, y, 1, "minecraft:air")
    g.set(4, 2, 0, "minecraft:dark_oak_stairs", {"facing": "north", "half": "bottom"})
    g.set(3, 2, 0, "minecraft:dark_oak_stairs", {"facing": "north", "half": "bottom"})
    g.set(5, 2, 0, "minecraft:dark_oak_stairs", {"facing": "north", "half": "bottom"})
    # оглобли
    g.set(4, 1, 0, "minecraft:dark_oak_fence")
    # интерьер
    for z in range(2, 12):
        for x in (3, 4, 5):
            g.set(x, 3, z, "minecraft:red_carpet")
    g.set(6, 3, 11, "minecraft:chest", {"facing": "north", "type": "single"}, CHEST_NBT())
    g.set(2, 3, 11, "illusionist:trick_box")
    g.set(4, 3, 11, "minecraft:barrel", {"facing": "up"})
    g.set(4, 4, 11, "illusionist:cursed_top_hat")
    for z in (3, 4, 5):
        g.set(2, 3, z, "minecraft:bookshelf"); g.set(2, 4, z, "minecraft:bookshelf")
    g.set(4, 5, 6, "minecraft:soul_lantern", {"hanging": "true"})
    g.set(6, 5, 2, "minecraft:cobweb"); g.set(2, 5, 11, "minecraft:cobweb")
    g.set(6, 3, 3, "minecraft:candle", {"candles": 2, "lit": "true"})
    g.save(f"{DATA}/structure/circus_wagon.nbt")

tent()
wagon()
