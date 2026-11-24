#!/usr/bin/env python3
"""Дополнительный звук: медленный скрип двери (door_creak.ogg, ~3.6 c). Запуск из корня: python3 tools/gen_sounds_door.py"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
import gen_sounds as g

def door_creak():
    dur = 3.7
    n = int(g.SR * dur)
    t = g.t_axis(dur)
    # частота «прилипания-скольжения» плавно гуляет: дверь движется неравномерно
    f = 16 + 12 * np.sin(np.pi * t / dur) ** 1.5 + 5 * np.sin(2 * np.pi * 0.7 * t) + g.lowpass(g.rng.standard_normal(n), 6) * 25
    f = np.clip(f, 6, 60)
    phase = np.cumsum(f) / g.SR
    saw = (phase % 1.0) * 2 - 1
    pulses = np.maximum(0, saw) ** 6                        # короткие щелчки трения
    exc = pulses + 0.15 * g.rng.standard_normal(n) * pulses
    # резонансы дерева/петель, чуть «плывут» вверх
    y = (g.bandpass(exc, 380, 520) * 1.0 + g.bandpass(exc, 860, 1150) * 0.8 + g.bandpass(exc, 1700, 2300) * 0.45)
    y += 0.5 * g.bandpass(g.rng.standard_normal(n) * pulses, 2800, 4200) * 0.3
    # низкий стон древесины
    groan = g.lowpass(g.rng.standard_normal(n), 140) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.45 * t + 1.0))
    y += groan * 3.0
    env = np.minimum(1, t / 0.5) ** 1.5 * np.minimum(1, (dur - t) / 0.9) ** 1.2
    env *= 0.75 + 0.25 * np.sin(2 * np.pi * 1.3 * t)
    return g.reverb(y * env, 1.0, 0.22)

if __name__ == "__main__":
    g.save("door_creak", door_creak(), 0.85)
