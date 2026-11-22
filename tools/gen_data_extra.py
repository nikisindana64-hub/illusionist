#!/usr/bin/env python3
"""Добавляет json-ресурсы для Доброго Фокусника, Монокля, Гипноза, занавеса и цилиндра-блока.
Запуск из корня проекта: python3 tools/gen_data_extra.py"""
import json
import os

M = "illusionist"
A = f"src/main/resources/assets/{M}"
D = f"src/main/resources/data/{M}"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


# ---------- модели предметов ----------
w(f"{A}/models/item/monocle_of_truth.json",
  {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{M}:item/monocle_of_truth"}})
w(f"{A}/models/item/kind_magician_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
w(f"{A}/models/item/cursed_top_hat.json", {"parent": f"{M}:block/cursed_top_hat"})
w(f"{A}/models/item/phantom_curtain.json", {"parent": f"{M}:block/phantom_curtain"})

# ---------- блок: фантомный занавес ----------
w(f"{A}/blockstates/phantom_curtain.json", {"variants": {"": {"model": f"{M}:block/phantom_curtain"}}})
w(f"{A}/models/block/phantom_curtain.json",
  {"parent": "minecraft:block/cube_all", "textures": {"all": f"{M}:block/phantom_curtain"}})

# ---------- блок: проклятый цилиндр ----------
w(f"{A}/blockstates/cursed_top_hat.json", {"variants": {"": {"model": f"{M}:block/cursed_top_hat"}}})


def faces(side_uv, top_tex="#top", side_tex="#side"):
    return {
        "north": {"texture": side_tex, "uv": side_uv}, "south": {"texture": side_tex, "uv": side_uv},
        "east": {"texture": side_tex, "uv": side_uv}, "west": {"texture": side_tex, "uv": side_uv},
        "up": {"texture": top_tex}, "down": {"texture": top_tex},
    }


w(f"{A}/models/block/cursed_top_hat.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "#side", "side": f"{M}:block/cursed_top_hat_side", "top": f"{M}:block/cursed_top_hat_top"},
    "elements": [
        {"from": [1.5, 0, 1.5], "to": [14.5, 1, 14.5], "faces": faces([0, 15, 16, 16])},                # поля
        {"from": [4, 1, 4], "to": [12, 12, 12], "faces": faces([4, 0, 12, 11])},                         # тулья
    ],
})

# ---------- lang ----------
ru = {
    "entity.illusionist.kind_magician": "Добрый Фокусник",
    "effect.illusionist.hypnosis": "Гипноз Фокусника",
    "item.illusionist.monocle_of_truth": "Монокль Истины",
    "item.illusionist.monocle_of_truth.tooltip1": "В руке защищает от Гипноза и подмены хотбара",
    "item.illusionist.monocle_of_truth.tooltip2": "ПКМ — развеять иллюзии, подсветить настоящего",
    "item.illusionist.kind_magician_spawn_egg": "Яйцо призыва: Добрый Фокусник",
    "item.illusionist.show_ticket.tooltip2": "ПКМ — следующий акт представления и выход Фокусника.",
    "block.illusionist.cursed_top_hat": "Проклятый цилиндр",
    "block.illusionist.phantom_curtain": "Фантомный занавес",
    "message.illusionist.monocle": "Монокль Истины: развеяно иллюзий — %s",
}
en = {
    "entity.illusionist.kind_magician": "Kind Magician",
    "effect.illusionist.hypnosis": "Magician's Hypnosis",
    "item.illusionist.monocle_of_truth": "Monocle of Truth",
    "item.illusionist.monocle_of_truth.tooltip1": "Held: protects from Hypnosis and hotbar shuffling",
    "item.illusionist.monocle_of_truth.tooltip2": "Right-click: dispel illusions, highlight the real one",
    "item.illusionist.kind_magician_spawn_egg": "Kind Magician Spawn Egg",
    "item.illusionist.show_ticket.tooltip2": "Right-click: next act of the show and the Magician's entrance.",
    "block.illusionist.cursed_top_hat": "Cursed Top Hat",
    "block.illusionist.phantom_curtain": "Phantom Curtain",
    "message.illusionist.monocle": "Monocle of Truth: illusions dispelled - %s",
}
for name, extra in (("ru_ru", ru), ("en_us", en)):
    path = f"{A}/lang/{name}.json"
    data = json.load(open(path, encoding="utf-8"))
    data.update(extra)
    w(path, data)

# ---------- рецепты и лут ----------
w(f"{D}/recipe/monocle_of_truth.json", {
    "type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": [" N ", "NGN", " S "],
    "key": {"N": {"item": "minecraft:gold_nugget"}, "G": {"item": "minecraft:glass_pane"},
            "S": {"item": "minecraft:string"}},
    "result": {"id": f"{M}:monocle_of_truth", "count": 1}})
w(f"{D}/loot_table/blocks/cursed_top_hat.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "bonus_rolls": 0,
               "conditions": [{"condition": "minecraft:survives_explosion"}],
               "entries": [{"type": "minecraft:item", "name": f"{M}:cursed_top_hat"}]}]})

# ---------- обновим лут Фокусника: монокль иногда ----------
path = f"{D}/loot_table/entities/magician.json"
loot = json.load(open(path, encoding="utf-8"))
if not any("monocle" in json.dumps(p) for p in loot["pools"]):
    loot["pools"].append({"rolls": 1,
                          "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}],
                          "entries": [{"type": "minecraft:item", "name": f"{M}:monocle_of_truth"}]})
w(path, loot)

# ---------- достижение: победить доброго обманщика? (первое предательство) ----------
w(f"{D}/advancement/monocle.json", {
    "parent": f"{M}:stage_fright",
    "display": {
        "icon": {"id": f"{M}:monocle_of_truth"},
        "title": {"translate": f"advancements.{M}.monocle.title"},
        "description": {"translate": f"advancements.{M}.monocle.description"},
        "frame": "task", "show_toast": True, "announce_to_chat": True, "hidden": False},
    "criteria": {"has_it": {"trigger": "minecraft:inventory_changed",
                            "conditions": {"items": [{"items": f"{M}:monocle_of_truth"}]}}}})
for name, extra in (("ru_ru", {
        "advancements.illusionist.monocle.title": "Взгляд сквозь обман",
        "advancements.illusionist.monocle.description": "Получи Монокль Истины"}),
        ("en_us", {
        "advancements.illusionist.monocle.title": "Seeing Through the Trick",
        "advancements.illusionist.monocle.description": "Obtain the Monocle of Truth"})):
    path = f"{A}/lang/{name}.json"
    data = json.load(open(path, encoding="utf-8"))
    data.update(extra)
    w(path, data)
print("data extra ok")
