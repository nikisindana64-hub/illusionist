package com.illusionist.entity;

/** Что модели и рендереру нужно знать о Фокуснике (общий интерфейс для злого и доброго). */
public interface MagicianPose {
    /** Замер «та-да» — на него смотрят. */
    boolean isWatchedPose();

    /** Оставшиеся тики анимации «колдует» (руки вверх). */
    int getCastTicks();

    /** 0 — обычный, 1 — одержимый (треснувшая маска), 2 — добрый. */
    int getSkinVariant();
}
