#!/usr/bin/env python3
"""Генерирует текстуры, модели, рецепты, лут и переводы. Запуск из корня проекта: python3 tools/gen_assets.py"""
import json, os, random, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/winterapocalypse')
D = os.path.join(RES, 'data/winterapocalypse')
MC = os.path.join(RES, 'data/minecraft')

def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)

def wj(path, obj):
    w(path, json.dumps(obj, ensure_ascii=False, indent=2) + '\n')

# ======================================================================= PNG canvas
class Canvas:
    def __init__(self, w_, h_):
        self.w, self.h = w_, h_
        self.px = [[(0, 0, 0, 0)] * w_ for _ in range(h_)]
    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            if len(c) == 3: c = (c[0], c[1], c[2], 255)
            self.px[y][x] = c
    def get(self, x, y):
        return self.px[y][x]
    def rect(self, x0, y0, x1, y1, c, noise=0):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.set(x, y, jitter(c, noise))
    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        raw = b''
        for row in self.px:
            raw += b'\x00' + b''.join(bytes(p) for p in row)
        def chunk(t, d):
            c = struct.pack('>I', len(d)) + t + d
            return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
        png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 6, 0, 0, 0)) \
              + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
        open(path, 'wb').write(png)

def clamp(v): return max(0, min(255, int(v)))
def jitter(c, n):
    if n == 0: return c
    d = random.randint(-n, n)
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d)) + ((c[3],) if len(c) > 3 else ())
def shade(c, k): return (clamp(c[0] * k), clamp(c[1] * k), clamp(c[2] * k))

# ======================================================================= item icons
random.seed(11)
TEX_I = os.path.join(A, 'textures/item')
TEX_B = os.path.join(A, 'textures/block')
TEX_E = os.path.join(A, 'textures/entity')
TEX_ARM = os.path.join(A, 'textures/models/armor')

def piece_icon(kind, base, trim, accent=None, long=False):
    """Силуэт предмета одежды 16x16: base - основной цвет, trim - мех, accent - детали."""
    mask, hole, trimmask, accmask = set(), set(), set(), set()
    def R(s, x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                s.add((x, y))
    if kind == 'helmet':
        if not long:   # шапка с помпоном и меховым отворотом
            R(mask, 4, 3, 12, 9); R(mask, 3, 8, 13, 11); R(mask, 3, 11, 5, 14); R(mask, 11, 11, 13, 14)
            R(mask, 7, 1, 9, 3)
            R(trimmask, 3, 8, 13, 11); R(trimmask, 7, 1, 9, 3)
        else:          # капюшон с меховой окантовкой лица
            R(mask, 3, 2, 13, 14); R(hole, 5, 6, 11, 13)
            mask -= hole
            for q in [(3, 2), (4, 2), (11, 2), (12, 2), (3, 3), (12, 3), (3, 13), (12, 13)]: mask.discard(q)
            for (x, y) in list(mask):
                if any((x + dx, y + dy) in hole for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                    trimmask.add((x, y))
            R(accmask, 6, 2, 10, 3)
    elif kind == 'chest':
        bottom = 15 if long else 12
        R(mask, 4, 3, 12, bottom); R(mask, 1, 3, 4, 10 if not long else 11); R(mask, 12, 3, 15, 10 if not long else 11)
        R(hole, 6, 3, 10, 5); mask -= hole
        for (x, y) in list(mask):
            if any((x + dx, y + dy) in hole for dx in (-1, 0, 1) for dy in (-1, 0, 1)): trimmask.add((x, y))
            if y == bottom - 1 and 4 <= x < 12: trimmask.add((x, y))
            if y == (9 if not long else 10) and (x < 4 or x >= 12): trimmask.add((x, y))
        if long:
            for y in range(5, bottom): accmask.add((8, y))   # застёжка
            for x in range(4, 12): accmask.add((x, 9))       # пояс
        else:
            for y in range(5, bottom - 1): accmask.add((8, y))
    elif kind == 'legs':
        R(mask, 4, 2, 12, 6); R(mask, 4, 6, 7, 14); R(mask, 9, 6, 12, 14); R(mask, 7, 6, 9, 8)
        for (x, y) in mask:
            if y == 13: trimmask.add((x, y))
            if y == 2: trimmask.add((x, y))
        if long:
            for (x, y) in mask:
                if y == 9: accmask.add((x, y))
    elif kind == 'boots':
        R(mask, 3, 5, 7, 12); R(mask, 2, 11, 8, 14); R(mask, 9, 5, 13, 12); R(mask, 8, 11, 14, 14)
        for (x, y) in mask:
            if y in (5, 6): trimmask.add((x, y))
            if y == 13: accmask.add((x, y))
    c = Canvas(16, 16)
    for (x, y) in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        col = base
        if (x, y) in trimmask: col = trim
        if accmask and (x, y) in accmask and accent: col = accent
        col = jitter(col, 8)
        if edge: col = shade(col, 0.6)
        c.set(x, y, col)
    return c

SETS = {
    'winter':   dict(base=(150, 98, 62), trim=(236, 236, 242), accent=(95, 60, 38)),
    'blizzard': dict(base=(52, 78, 122), trim=(226, 236, 248), accent=(235, 150, 55)),
}
items = [
    ('winter_hat', 'helmet', 'winter', False), ('winter_jacket', 'chest', 'winter', False),
    ('winter_pants', 'legs', 'winter', False), ('winter_boots', 'boots', 'winter', False),
    ('blizzard_hood', 'helmet', 'blizzard', True), ('blizzard_parka', 'chest', 'blizzard', True),
    ('blizzard_leggings', 'legs', 'blizzard', True), ('blizzard_boots', 'boots', 'blizzard', True),
]
for name, kind, st, long in items:
    s = SETS[st]
    piece_icon(kind, s['base'], s['trim'], s['accent'], long).save(os.path.join(TEX_I, name + '.png'))

# frost shard (кристалл)
c = Canvas(16, 16)
crystal = [(8, 1), (7, 2), (8, 2), (9, 2), (7, 3), (8, 3), (9, 3), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4),
           (6, 5), (7, 5), (8, 5), (9, 5), (10, 5), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
           (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9),
           (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11), (7, 12), (8, 12), (9, 12), (7, 13), (8, 13), (8, 14)]
cs = set(crystal)
for (x, y) in crystal:
    edge = any((x + dx, y + dy) not in cs for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    col = (140, 205, 245) if x < 8 else (95, 165, 225)
    if x == 7 and 3 < y < 9: col = (225, 245, 255)
    c.set(x, y, shade(col, 0.65) if edge else jitter(col, 6))
c.save(os.path.join(TEX_I, 'frost_shard.png'))

# frost heart (сердце)
heart = ["................", "..####....####..", ".######..######.", ".##############.", ".##############.", ".##############.",
         "..############..", "...##########...", "....########....", ".....######.....", "......####......", ".......##.......", "................"]
c = Canvas(16, 16)
hs = set((x, y + 1) for y, row in enumerate(heart) for x, ch in enumerate(row) if ch == '#')
for (x, y) in hs:
    edge = any((x + dx, y + dy) not in hs for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    col = jitter((120, 190, 245), 10)
    if (x, y) in ((4, 4), (5, 4), (4, 5), (11, 4)): col = (235, 250, 255)
    if abs((x - 8)) + abs(y - 6) <= 1: col = (210, 240, 255)
    c.set(x, y, shade(col, 0.55) if edge else col)
c.save(os.path.join(TEX_I, 'frost_heart.png'))

# warmth core (светящийся шар)
c = Canvas(16, 16)
for y in range(16):
    for x in range(16):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d <= 6.2:
            t = d / 6.2
            col = (255, clamp(235 - 140 * t), clamp(130 - 110 * t))
            if d > 5.2: col = (120, 50, 20)
            c.set(x, y, jitter(col, 6))
for (x, y) in [(5, 5), (6, 4), (5, 6)]: c.set(x, y, (255, 255, 230))
for ang in range(0, 360, 45):
    import math
    x = int(round(7.5 + 7.4 * math.cos(math.radians(ang)))); y = int(round(7.5 + 7.4 * math.sin(math.radians(ang))))
    c.set(x, y, (255, 190, 70))
c.save(os.path.join(TEX_I, 'warmth_core.png'))

# ======================================================================= armor layers 64x32
def armor_layer(path, base, trim, accent, parka):
    c = Canvas(64, 32)
    for y in range(32):
        for x in range(64):
            col = jitter(base, 9)
            if (x // 2 + y // 2) % 5 == 0: col = shade(col, 0.9)   # «вязка»/стёжка
            c.set(x, y, col)
    for y in (14, 15, 20, 21, 30, 31):            # меховые края: шея/пояс/манжеты
        for x in range(64): c.set(x, y, jitter(trim, 7))
    for y in range(8, 11):                        # головной убор: мех у лба
        for x in range(0, 32): c.set(x, y, jitter(trim, 7))
    if parka:
        for y in range(22, 30):                   # застёжка по центру груди
            for x in (23, 24): c.set(x, y, shade(accent, 0.9))
        for x in range(16, 40): c.set(x, 26, accent)   # пояс
        for x in range(0, 16): c.set(x, 26, accent)    # ремешок на ногах
    else:
        for x in range(16, 40): c.set(x, 26, accent)
    c.save(path)

for st, parka in (('winter', False), ('blizzard', True)):
    s = SETS[st]
    armor_layer(os.path.join(TEX_ARM, f'{st}_layer_1.png'), s['base'], s['trim'], s['accent'], parka)
    armor_layer(os.path.join(TEX_ARM, f'{st}_layer_2.png'), shade(s['base'], 0.85), s['trim'], s['accent'], parka)

# ======================================================================= heater block textures
def metal(c, base):
    for y in range(16):
        for x in range(16):
            c.set(x, y, jitter(base, 7))
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)): c.set(x, y, (150, 150, 160))
    for i in range(16):
        c.set(i, 0, shade(base, 1.3)); c.set(0, i, shade(base, 1.3))
        c.set(i, 15, shade(base, 0.6)); c.set(15, i, shade(base, 0.6))

c = Canvas(16, 16); metal(c, (84, 84, 94))
for y in range(4, 13, 2):                       # решётка
    for x in range(3, 13): c.set(x, y, (30, 30, 36))
c.save(os.path.join(TEX_B, 'winter_heater_top.png'))

c = Canvas(16, 16); metal(c, (78, 78, 88))
for y in range(3, 13): c.set(7, y, (55, 55, 64)); c.set(8, y, (110, 110, 120))
c.save(os.path.join(TEX_B, 'winter_heater_side.png'))

def heater_front(lit):
    c = Canvas(16, 16); metal(c, (86, 86, 96))
    c.rect(3, 4, 13, 13, (20, 18, 20))
    if lit:
        for y in range(5, 13):
            for x in range(4, 12):
                t = (12 - y) / 8.0
                c.set(x, y, (255, clamp(120 + 110 * t * random.random()), clamp(30 + 40 * t)))
    for x in (5, 8, 11): 
        for y in range(4, 13): c.set(x, y, (45, 45, 52))           # решётка перед огнём
    c.rect(3, 2, 13, 4, (60, 60, 70))
    return c
heater_front(False).save(os.path.join(TEX_B, 'winter_heater_front.png'))
heater_front(True).save(os.path.join(TEX_B, 'winter_heater_front_on.png'))

# ======================================================================= entity textures
def humanoid(path, skin, cloth, cloth2, eyes, frost, seed, cracks=False):
    random.seed(seed)
    c = Canvas(64, 64)
    regions = {
        'head': (0, 0, 32, 16), 'body': (16, 16, 40, 32), 'rarm': (40, 16, 56, 32),
        'rleg': (0, 16, 16, 32), 'larm': (32, 48, 48, 64), 'lleg': (16, 48, 32, 64),
    }
    def fill(name, col, n=10):
        x0, y0, x1, y1 = regions[name]; c.rect(x0, y0, x1, y1, col, n)
    fill('head', skin); fill('body', cloth); fill('rarm', skin); fill('larm', skin)
    fill('rleg', cloth2); fill('lleg', cloth2)
    # рукава (верх рук) — лохмотья одежды
    for (x0, y0) in ((40, 16), (32, 48)):
        c.rect(x0, y0 + 4, x0 + 16, y0 + 10, cloth, 10)
    # босые ступни
    for (x0, y0) in ((0, 16), (16, 48)):
        c.rect(x0, y0 + 12, x0 + 16, y0 + 16, skin, 8)
    # лицо: передняя грань головы x 8..16, y 8..16
    c.set(9, 12, (30, 40, 60)); c.set(10, 12, (30, 40, 60)); c.set(13, 12, (30, 40, 60)); c.set(14, 12, (30, 40, 60))
    c.set(9, 12, eyes); c.set(14, 12, eyes); c.set(10, 12, eyes); c.set(13, 12, eyes)
    for x in range(10, 14): c.set(x, 14, shade(skin, 0.55))
    for x in range(9, 15): c.set(x, 11, shade(skin, 0.7))
    # иней
    for _ in range(140):
        x, y = random.randrange(64), random.randrange(64)
        if c.get(x, y)[3] and random.random() < 0.55:
            c.set(x, y, jitter(frost, 10))
    if cracks:
        for _ in range(14):
            x, y = random.randrange(64), random.randrange(64)
            for i in range(random.randint(3, 6)):
                if c.get(x, y)[3]:
                    c.set(x, y, shade(skin, 0.45))
                x += random.choice((-1, 0, 1)); y += random.choice((0, 1))
                x = max(0, min(63, x)); y = max(0, min(63, y))
    c.save(path)

humanoid(os.path.join(TEX_E, 'frost_walker.png'), skin=(150, 190, 205), cloth=(62, 84, 112),
         cloth2=(46, 58, 82), eyes=(120, 240, 255), frost=(225, 245, 255), seed=5)
humanoid(os.path.join(TEX_E, 'frost_giant.png'), skin=(118, 176, 228), cloth=(80, 128, 190),
         cloth2=(60, 100, 160), eyes=(255, 80, 60), frost=(235, 250, 255), seed=9, cracks=True)

# Мастер по зиме (раскладка деревенского жителя)
random.seed(21)
c = Canvas(64, 64)
skin, nose = (214, 172, 142), (190, 146, 116)
coat, coat_dark, fur = (62, 92, 126), (45, 68, 96), (232, 234, 240)
c.rect(0, 0, 32, 18, skin, 6)                 # голова
c.rect(24, 0, 32, 6, nose, 5)                 # нос
c.rect(24, 8, 32, 18, (225, 225, 230), 6)     # затылок/волосы
c.rect(0, 8, 8, 18, (225, 225, 230), 6)       # бока головы — седые волосы (правая)
c.rect(16, 8, 24, 18, (225, 225, 230), 6)     # (левая)
# лицо: передняя грань x 8..16, y 8..18
for x in range(9, 15): c.set(x, 11, (200, 200, 205))        # брови
for (x, y) in ((9, 12), (10, 12), (13, 12), (14, 12)): c.set(x, y, (250, 250, 250))
for (x, y) in ((10, 12), (13, 12)): c.set(x, y, (60, 120, 190))
c.rect(8, 15, 16, 18, (236, 236, 240), 5)     # борода
c.rect(10, 14, 14, 15, (200, 120, 110))       # рот
# меховая шапка (hat): верхняя грань + верх боковых граней
c.rect(32, 0, 64, 18, (0, 0, 0, 0))
c.rect(40, 0, 56, 8, fur, 7)                  # верх шапки
for (x0, x1) in ((32, 40), (40, 48), (48, 56), (56, 64)):
    c.rect(x0, 8, x1, 13, fur, 7)             # боковые грани: мех до бровей
    c.rect(x0, 12, x1, 13, (190, 192, 200), 5)
# тело + халат (длинная парка)
c.rect(16, 20, 44, 38, coat, 6)
c.rect(0, 38, 28, 64, coat, 6)
for x in range(0, 28): 
    for y in (60, 61, 62, 63): c.set(x, y, jitter(fur, 6))   # меховой подол
for y in range(44, 60): c.set(9, y, coat_dark); c.set(10, y, coat_dark)  # застёжка (перед халата)
for x in range(0, 28): c.set(x, 50, (200, 140, 60))          # пояс
# руки
c.rect(44, 22, 60, 34, coat, 6)
c.rect(44, 30, 60, 34, fur, 6)                # меховые манжеты
c.rect(40, 38, 64, 46, coat, 6)
c.rect(40, 42, 64, 46, fur, 6)
# ноги
c.rect(0, 22, 16, 38, (42, 46, 62), 5)
c.rect(0, 33, 16, 38, (70, 48, 34), 5)
c.save(os.path.join(TEX_E, 'winter_master.png'))

# ======================================================================= blockstate / models
wj(os.path.join(A, 'blockstates/winter_heater.json'), {"variants": {
    f"facing={f},lit={'true' if lit else 'false'}": {
        "model": "winterapocalypse:block/winter_heater" + ("_on" if lit else ""), **({"y": y} if y else {})}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for lit in (False, True)}})
for suffix, front in (("", "winter_heater_front"), ("_on", "winter_heater_front_on")):
    wj(os.path.join(A, f'models/block/winter_heater{suffix}.json'), {
        "parent": "minecraft:block/orientable",
        "textures": {"top": "winterapocalypse:block/winter_heater_top",
                     "front": "winterapocalypse:block/" + front,
                     "side": "winterapocalypse:block/winter_heater_side"}})
wj(os.path.join(A, 'models/item/winter_heater.json'), {"parent": "winterapocalypse:block/winter_heater"})

for n in ['frost_shard', 'frost_heart', 'warmth_core'] + [i[0] for i in items]:
    wj(os.path.join(A, f'models/item/{n}.json'),
       {"parent": "minecraft:item/generated", "textures": {"layer0": f"winterapocalypse:item/{n}"}})
for n in ('frost_walker', 'frost_giant', 'winter_master'):
    wj(os.path.join(A, f'models/item/{n}_spawn_egg.json'), {"parent": "minecraft:item/template_spawn_egg"})

# ======================================================================= lang
en = {
    "itemGroup.winterapocalypse": "Winter Apocalypse",
    "block.winterapocalypse.cold_block": "Cold Block",
    "block.winterapocalypse.winter_heater": "Winter Heater",
    "item.winterapocalypse.frost_shard": "Frost Shard",
    "item.winterapocalypse.frost_heart": "Heart of Frost",
    "item.winterapocalypse.warmth_core": "Warmth Core",
    "item.winterapocalypse.winter_hat": "Winter Hat",
    "item.winterapocalypse.winter_jacket": "Winter Jacket",
    "item.winterapocalypse.winter_pants": "Winter Pants",
    "item.winterapocalypse.winter_boots": "Winter Boots",
    "item.winterapocalypse.blizzard_hood": "Blizzard Hood",
    "item.winterapocalypse.blizzard_parka": "Long Blizzard Parka",
    "item.winterapocalypse.blizzard_leggings": "Blizzard Leggings",
    "item.winterapocalypse.blizzard_boots": "Blizzard Boots",
    "item.winterapocalypse.frost_walker_spawn_egg": "Frostbitten Spawn Egg",
    "item.winterapocalypse.frost_giant_spawn_egg": "Frost Giant Spawn Egg",
    "item.winterapocalypse.winter_master_spawn_egg": "Winter Master Spawn Egg",
    "entity.winterapocalypse.frost_walker": "Frostbitten",
    "entity.winterapocalypse.frost_giant": "Frost Giant",
    "entity.winterapocalypse.winter_master": "Winter Master",
    "tooltip.winterapocalypse.warmth": "+%s °C warmth",
    "tooltip.winterapocalypse.wind": "%s%% blizzard protection",
    "tooltip.winterapocalypse.warmth_core": "Right-click a Cold Block to slow its growth",
    "hud.winterapocalypse.body": "Body: %s °C",
    "hud.winterapocalypse.air": "Air: %s °C",
    "hud.winterapocalypse.day": "Winter day: %s",
    "hud.winterapocalypse.blizzard": "BLIZZARD! Protection: %s%%",
    "msg.winterapocalypse.intro": "The Winter Apocalypse has begun... A Cold Block has appeared 10–100 blocks from the world spawn and its snow spreads wider every day. Find the Winter Master near spawn — he sells warm clothes and heaters.",
    "msg.winterapocalypse.cold_info": "Cold Block | radius: %s | slowed for: %s days",
    "msg.winterapocalypse.core_used": "The Cold Block's growth slows by %s%% for %s days",
    "msg.winterapocalypse.heater_burn": "Fuel left: %s s (right-click with fuel)",
    "msg.winterapocalypse.heater_full": "The heater is full",
    "command.winterapocalypse.status": "Winter day: %s | Sources: %s | Nearest source radius: %s | Air here: %s °C | Body: %s °C",
    "command.winterapocalypse.locate": "Cold Block at %s, %s, %s (radius %s)",
    "command.winterapocalypse.none": "No Cold Blocks in this world.",
    "command.winterapocalypse.setday": "Winter day set to %s",
}
# в английском «core_used» — аргументы (дни, процент): подправим порядок текста
en["msg.winterapocalypse.core_used"] = "Cold Block slowed for %s days (growth at %s%% speed)"
ru = {
    "itemGroup.winterapocalypse": "Зимний апокалипсис",
    "block.winterapocalypse.cold_block": "Блок холода",
    "block.winterapocalypse.winter_heater": "Печь-обогреватель",
    "item.winterapocalypse.frost_shard": "Ледяной осколок",
    "item.winterapocalypse.frost_heart": "Сердце льда",
    "item.winterapocalypse.warmth_core": "Ядро тепла",
    "item.winterapocalypse.winter_hat": "Зимняя шапка",
    "item.winterapocalypse.winter_jacket": "Зимняя куртка",
    "item.winterapocalypse.winter_pants": "Зимние штаны",
    "item.winterapocalypse.winter_boots": "Зимние сапоги",
    "item.winterapocalypse.blizzard_hood": "Капюшон метели",
    "item.winterapocalypse.blizzard_parka": "Длинная парка метели",
    "item.winterapocalypse.blizzard_leggings": "Штаны метели",
    "item.winterapocalypse.blizzard_boots": "Сапоги метели",
    "item.winterapocalypse.frost_walker_spawn_egg": "Яйцо призыва: Обмороженный",
    "item.winterapocalypse.frost_giant_spawn_egg": "Яйцо призыва: Ледяной великан",
    "item.winterapocalypse.winter_master_spawn_egg": "Яйцо призыва: Мастер по зиме",
    "entity.winterapocalypse.frost_walker": "Обмороженный",
    "entity.winterapocalypse.frost_giant": "Ледяной великан",
    "entity.winterapocalypse.winter_master": "Мастер по зиме",
    "tooltip.winterapocalypse.warmth": "+%s °C тепла",
    "tooltip.winterapocalypse.wind": "Защита от метели: %s%%",
    "tooltip.winterapocalypse.warmth_core": "ПКМ по Блоку холода — замедляет его рост",
    "hud.winterapocalypse.body": "Тело: %s °C",
    "hud.winterapocalypse.air": "Воздух: %s °C",
    "hud.winterapocalypse.day": "День зимы: %s",
    "hud.winterapocalypse.blizzard": "МЕТЕЛЬ! Защита: %s%%",
    "msg.winterapocalypse.intro": "Зимний апокалипсис начался... В 10–100 блоках от спавна появился Блок холода, и с каждым днём снег расползается шире и быстрее. Найди Мастера по зиме возле спавна — он продаёт тёплую одежду и печи.",
    "msg.winterapocalypse.cold_info": "Блок холода | радиус: %s | замедлен ещё на: %s дн.",
    "msg.winterapocalypse.core_used": "Блок холода замедлен на %s дн. (рост на %s%% скорости)",
    "msg.winterapocalypse.heater_burn": "Топлива осталось: %s с (ПКМ топливом)",
    "msg.winterapocalypse.heater_full": "Печь полностью заправлена",
    "command.winterapocalypse.status": "День зимы: %s | Источников: %s | Радиус ближайшего: %s | Воздух здесь: %s °C | Тело: %s °C",
    "command.winterapocalypse.locate": "Блок холода: %s, %s, %s (радиус %s)",
    "command.winterapocalypse.none": "В этом мире нет блоков холода.",
    "command.winterapocalypse.setday": "День зимы установлен: %s",
}
wj(os.path.join(A, 'lang/en_us.json'), en)
wj(os.path.join(A, 'lang/ru_ru.json'), ru)

# ======================================================================= recipes
def item(i): return {"item": i}
def shaped(name, pattern, key, result, count=1, cat="equipment"):
    wj(os.path.join(D, f'recipes/{name}.json'), {
        "type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
        "key": {k: item(v) for k, v in key.items()}, "result": {"item": result, "count": count}})

L, W = "minecraft:leather", "minecraft:white_wool"
shaped('winter_hat', ["LWL", "W W"], {"L": L, "W": W}, "winterapocalypse:winter_hat")
shaped('winter_jacket', ["L L", "WLW", "LWL"], {"L": L, "W": W}, "winterapocalypse:winter_jacket")
shaped('winter_pants', ["LWL", "W W", "L L"], {"L": L, "W": W}, "winterapocalypse:winter_pants")
shaped('winter_boots', ["L L", "W W"], {"L": L, "W": W}, "winterapocalypse:winter_boots")

F = "winterapocalypse:frost_shard"
for res, base in (("blizzard_hood", "winter_hat"), ("blizzard_leggings", "winter_pants"), ("blizzard_boots", "winter_boots")):
    shaped(res, ["FWF", "WXW", "FWF"], {"F": F, "W": W, "X": f"winterapocalypse:{base}"}, f"winterapocalypse:{res}")
shaped('blizzard_parka', ["FHF", "WXW", "FWF"],
       {"F": F, "H": "winterapocalypse:frost_heart", "W": W, "X": "winterapocalypse:winter_jacket"},
       "winterapocalypse:blizzard_parka")
shaped('winter_heater', ["III", "IFI", "ICI"],
       {"I": "minecraft:iron_ingot", "F": "minecraft:furnace", "C": "minecraft:coal_block"},
       "winterapocalypse:winter_heater", cat="misc")
shaped('warmth_core', ["CGC", "GSG", "CGC"],
       {"C": "minecraft:coal", "G": "minecraft:gold_ingot", "S": F}, "winterapocalypse:warmth_core", cat="misc")

# ======================================================================= loot
def loot_item(name, lo, hi, looting=0):
    fn = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if looting:
        fn.append({"function": "minecraft:looting_enchant", "count": {"min": 0, "max": looting}})
    return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": name, "functions": fn}]}

wj(os.path.join(D, 'loot_tables/entities/frost_walker.json'), {"type": "minecraft:entity", "pools": [
    loot_item("winterapocalypse:frost_shard", 0, 2, 1), loot_item("minecraft:snowball", 1, 3)]})
wj(os.path.join(D, 'loot_tables/entities/frost_giant.json'), {"type": "minecraft:entity", "pools": [
    loot_item("winterapocalypse:frost_shard", 3, 6, 2),
    loot_item("winterapocalypse:frost_heart", 1, 1, 1),
    loot_item("minecraft:packed_ice", 1, 3)]})
wj(os.path.join(D, 'loot_tables/blocks/winter_heater.json'), {"type": "minecraft:block", "pools": [{
    "rolls": 1, "bonus_rolls": 0,
    "entries": [{"type": "minecraft:item", "name": "winterapocalypse:winter_heater"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(os.path.join(MC, 'tags/blocks/mineable/pickaxe.json'), {"replace": False, "values": ["winterapocalypse:winter_heater"]})

# ======================================================================= natural spawns (Forge biome modifiers)
for name, weight, mn, mx in (("frost_walker", 25, 1, 3), ("frost_giant", 3, 1, 1)):
    wj(os.path.join(D, f'forge/biome_modifier/{name}_spawns.json'), {
        "type": "forge:add_spawns", "biomes": "#minecraft:is_overworld",
        "spawners": {"type": f"winterapocalypse:{name}", "weight": weight, "minCount": mn, "maxCount": mx}})

print("assets generated")
