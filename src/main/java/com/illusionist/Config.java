package com.illusionist;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue NATURAL_SPAWN = B
            .comment("Может ли Фокусник появляться в мире сам (ночью, редко).")
            .define("naturalSpawn", true);

    public static final ModConfigSpec.IntValue SPAWN_CHANCE = B
            .comment("Шанс появления: 1 из N успешных попыток спавна монстра. Больше число = реже.")
            .defineInRange("spawnChance", 12, 1, 1000);

    public static final ModConfigSpec.BooleanValue ALLOW_STEALING = B
            .comment("Может ли Фокусник красть предметы из инвентаря (вернутся после его смерти).")
            .define("allowStealing", true);

    public static final ModConfigSpec.BooleanValue LIGHTS_OUT = B
            .comment("Может ли Фокусник гасить факелы и свечи вокруг (учитывает gamerule mobGriefing).")
            .define("lightsOut", true);

    public static final ModConfigSpec.BooleanValue DIRECTOR_ENABLED = B
            .comment("Режиссёр представления: акты, напряжение, случайные события и визиты Доброго Фокусника.")
            .define("directorEnabled", true);

    public static final ModConfigSpec.BooleanValue KIND_VISITS = B
            .comment("Могут ли днём приходить мирные визиты Доброго Фокусника (он может оказаться ловушкой).")
            .define("kindVisits", true);

    public static final ModConfigSpec.BooleanValue SCREAMERS = B
            .comment("Скримеры: внезапное лицо на весь экран со звуком крика (редко, не чаще раза в 45 сек).")
            .define("screamers", true);

    public static final ModConfigSpec SPEC = B.build();
}
