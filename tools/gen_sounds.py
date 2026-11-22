#!/usr/bin/env python3
"""
Синтезирует звуки мода (numpy/scipy) и кодирует их в mono ogg через ffmpeg (libvorbis).
Запуск из корня проекта:  python3 tools/gen_sounds.py
Результат: src/main/resources/assets/illusionist/sounds/*.ogg

Звуки: music_box_note, applause, scream, laugh, drumroll.
Всё синтетическое, поэтому его можно спокойно заменить настоящими записями с теми же именами.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt, fftconvolve

SR = 44100
OUT = "src/main/resources/assets/illusionist/sounds"
rng = np.random.default_rng(42)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def bandpass(x, lo, hi, order=2):
    sos = butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return sosfilt(sos, x)


def lowpass(x, fc, order=2):
    return sosfilt(butter(order, fc, btype="low", fs=SR, output="sos"), x)


def highpass(x, fc, order=2):
    return sosfilt(butter(order, fc, btype="high", fs=SR, output="sos"), x)


def reverb(x, sec=1.2, wet=0.25, decay=4.0):
    n = int(SR * sec)
    ir = rng.standard_normal(n) * np.exp(-decay * np.arange(n) / n * 3)
    ir = lowpass(ir, 6000)
    ir /= np.max(np.abs(ir)) + 1e-9
    y = fftconvolve(x, ir)[: len(x) + n // 2]
    y = y / (np.max(np.abs(y)) + 1e-9) * np.max(np.abs(x))
    out = np.zeros(len(y))
    out[: len(x)] += x * (1 - wet)
    out += y * wet
    return out


def save(name, x, peak=0.9):
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    fade = int(SR * 0.01)
    x[-fade:] *= np.linspace(1, 0, fade)
    pcm = (x * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + ".wav")
        wavfile.write(wav, SR, pcm)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis",
                        "-q:a", "4", f"{OUT}/{name}.ogg"], check=True)
    print(f"{name}.ogg  {len(x) / SR:.2f}s")


# ---------------------------------------------------------------- музыкальная шкатулка
def music_box_note():
    t = t_axis(1.8)
    f0 = 523.25  # C5, высоту меняет pitch в игре
    x = np.zeros_like(t)
    # гребёнка шкатулки: гармонически-неточные обертоны, как у металлических язычков
    for mult, amp, tau in ((1.0, 1.0, 0.55), (2.76, 0.35, 0.28), (5.4, 0.18, 0.15), (8.9, 0.08, 0.08)):
        x += amp * np.sin(2 * np.pi * f0 * mult * t + rng.uniform(0, 6.28)) * np.exp(-t / tau)
    # лёгкая расстройка — жутковатый «плывущий» оттенок
    x += 0.25 * np.sin(2 * np.pi * f0 * 1.006 * t) * np.exp(-t / 0.5)
    click = bandpass(rng.standard_normal(len(t)), 2500, 7000) * np.exp(-t / 0.004) * 0.35
    x += click
    return x


# ---------------------------------------------------------------- аплодисменты
def applause():
    dur = 5.5
    n = int(SR * dur)
    x = np.zeros(n)
    t = t_axis(dur)
    # плотность хлопков: нарастание → плато → затухание
    dens = np.interp(t, [0, 0.4, 1.2, 3.8, dur], [10, 70, 190, 150, 0])
    clap = bandpass(rng.standard_normal(int(SR * 0.05)), 900, 5200) * np.exp(-np.arange(int(SR * 0.05)) / (SR * 0.009))
    i = 0
    while i < n:
        rate = dens[min(i, n - 1)] + 1e-3
        i += max(1, int(rng.exponential(SR / rate)))
        if i + len(clap) >= n:
            break
        gain = rng.uniform(0.25, 1.0)
        cp = clap * gain
        if rng.random() < 0.3:  # «удвоенный» хлопок
            cp = cp + np.roll(cp, int(SR * rng.uniform(0.004, 0.012))) * 0.5
        x[i: i + len(clap)] += cp
    x += bandpass(rng.standard_normal(n), 1500, 6000) * 0.02 * np.interp(t, [0, 1.5, dur], [0.2, 1, 0])
    return reverb(x, 1.4, 0.3)


# ---------------------------------------------------------------- скример (стингер)
def scream():
    dur = 2.4
    t = t_axis(dur)
    # низкий удар
    boom = np.sin(2 * np.pi * (70 * np.exp(-t * 3.0) + 38) * t) * np.exp(-t / 0.35)
    # крик: пила с глиссандо вверх, вибрато и формантами
    f = 520 + 520 * np.clip(t / 0.35, 0, 1) ** 0.6 + 40 * np.sin(2 * np.pi * 8.5 * t)
    phase = 2 * np.pi * np.cumsum(f) / SR
    saw = sum(np.sin(k * phase) / k for k in range(1, 28))
    voice = bandpass(saw, 700, 1300) * 1.0 + bandpass(saw, 2000, 3200) * 0.8 + bandpass(saw, 3400, 5200) * 0.4
    # диссонансный кластер (тритон и малая секунда)
    cl = sum(np.sin(2 * np.pi * fr * t + rng.uniform(0, 6)) for fr in (466, 659, 698, 988, 1319)) * 0.25
    breath = highpass(rng.standard_normal(len(t)), 2500) * 0.35
    env = np.where(t < 0.02, t / 0.02, np.exp(-(t - 0.02) / 0.9))
    x = (voice + cl + breath) * env
    x = np.tanh(x * 1.8) * 0.9  # перегруз
    x += boom * 1.4
    return reverb(x, 1.0, 0.18)


# ---------------------------------------------------------------- смех
def laugh():
    dur = 2.6
    n = int(SR * dur)
    x = np.zeros(n)
    formants = [(730, 90, 1.0), (1090, 110, 0.7), (2440, 160, 0.35), (3400, 200, 0.2)]  # гласная «а»
    start = 0.12
    for k in range(6):
        syl = 0.16 + 0.01 * k
        s = int(SR * (start + k * 0.27))
        ln = int(SR * syl)
        if s + ln >= n:
            break
        tt = np.arange(ln) / SR
        f0 = (150 - 7 * k) * (1.0 + 0.18 * np.exp(-tt / 0.05))  # падение тона в слоге
        f0 += 3 * np.sin(2 * np.pi * 5.5 * tt)
        ph = 2 * np.pi * np.cumsum(f0) / SR
        src = sum(np.sin(h * ph) / h ** 0.9 for h in range(1, 40))
        voiced = sum(a * bandpass(src, fc - bw, fc + bw, 2) for fc, bw, a in formants)
        breath = bandpass(rng.standard_normal(ln), 1200, 5000) * np.exp(-tt / 0.03) * 0.5  # придыхание «х»
        env = np.sin(np.pi * np.clip(tt / syl, 0, 1)) ** 0.7 * (1 - 0.09 * k)
        syl_sig = (voiced * env + breath) * (1.0 - 0.07 * k)
        x[s: s + ln] += syl_sig
    # «хор» — второй голос на октаву ниже и с задержкой, чтобы смех звучал нечеловечески
    low = np.roll(lowpass(x, 700), int(SR * 0.012)) * 0.6
    y = np.tanh((x + low) * 1.2)
    return reverb(y, 1.2, 0.3)


# ---------------------------------------------------------------- барабанная дробь
def drumroll():
    dur = 3.2
    n = int(SR * dur)
    x = np.zeros(n)
    t = t_axis(dur)
    hit_len = int(SR * 0.06)
    hit = (bandpass(rng.standard_normal(hit_len), 1200, 7000) * np.exp(-np.arange(hit_len) / (SR * 0.012))
           + np.sin(2 * np.pi * 190 * np.arange(hit_len) / SR) * np.exp(-np.arange(hit_len) / (SR * 0.02)) * 0.6)
    roll_end = 2.75
    pos = 0.0
    while pos < roll_end:
        i = int(pos * SR)
        prog = pos / roll_end
        gain = 0.12 + 0.88 * prog ** 1.5
        x[i: i + hit_len] += hit * gain * rng.uniform(0.7, 1.0)
        pos += 1.0 / (22 + 26 * prog)
    # финальный удар + тарелка
    i = int(roll_end * SR)
    x[i: i + hit_len] += hit * 1.6
    crash_len = n - i
    crash = highpass(rng.standard_normal(crash_len), 3500) * np.exp(-np.arange(crash_len) / (SR * 0.25)) * 0.9
    x[i:] += crash
    thud = np.sin(2 * np.pi * 55 * np.arange(crash_len) / SR) * np.exp(-np.arange(crash_len) / (SR * 0.2))
    x[i:] += thud * 0.9
    return reverb(x, 0.9, 0.22)


if __name__ == "__main__":
    save("music_box_note", music_box_note(), 0.8)
    save("applause", applause(), 0.9)
    save("scream", scream(), 0.95)
    save("laugh", laugh(), 0.9)
    save("drumroll", drumroll(), 0.9)
