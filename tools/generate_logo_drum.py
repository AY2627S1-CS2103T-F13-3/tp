#!/usr/bin/env python3
"""Original PonHub percussion cue. No reference audio or external samples used.

SPDX-License-Identifier: MIT
"""
import hashlib
import math
from pathlib import Path
import random
import struct
import wave

RATE = 44100
DURATION = 3.0
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/audio/logo-drum.wav"
rng = random.Random(2510432)
audio = [[0.0, 0.0] for _ in range(round(RATE * DURATION))]


def hit(kind, start, gain=1.0, pan=0.0):
    duration = {'kick': .30, 'snare': .19, 'hat': .06, 'tom': .20, 'clap': .13}[kind]
    previous = 0.0
    for i in range(round(duration * RATE)):
        t = i / RATE
        noise = rng.uniform(-1, 1)
        high = noise - previous
        previous = noise
        if kind == 'kick':
            phase = 2 * math.pi * (43*t + 100*.020*(1-math.exp(-t/.020)))
            value = .96*math.sin(phase)*math.exp(-t/.09) + .07*noise*math.exp(-t/.004)
        elif kind == 'snare':
            body = math.sin(2*math.pi*192*t) + .25*math.sin(2*math.pi*327*t)
            value = .32*body*math.exp(-t/.036) + .43*high*math.exp(-t/.036)
        elif kind == 'hat':
            metal = sum(math.sin(2*math.pi*f*t) for f in (4257, 6071, 8393))/3
            value = (.12*high + .05*metal)*math.exp(-t/.010)
        elif kind == 'clap':
            envelope = sum(math.exp(-(t-p)/.022) if t >= p else 0 for p in (0, .012, .024))
            value = .10*high*envelope
        else:
            phase = 2*math.pi*(105*t + 70*.025*(1-math.exp(-t/.025)))
            value = .45*math.sin(phase)*math.exp(-t/.058)
        attack = min(1, t/.001)
        index = round(start*RATE) + i
        if index < len(audio):
            audio[index][0] += value*gain*attack*(1-max(0,pan))
            audio[index][1] += value*gain*attack*(1+min(0,pan))


# New pattern on a 108 BPM grid. No reference onset/tune transcription.
beat = 60/100
for position, gain in ((0, 1), (.75, .58), (2, .9), (2.5, .65)):
    hit('kick', position*beat, gain)
for position, gain in ((1, .92), (2.75, .25), (3.5, .85)):
    hit('snare', position*beat, gain, -.06)
for step in range(8):
    if step != 6:
        hit('hat', step*.5*beat, .6 if step%2 else .85, .12)
hit('tom', 3*beat, .7, -.10)
hit('clap', 3.5*beat, .24, .10)
hit('hat', 3.25*beat, .30, .08)

# Newly composed muted bass accents: B1, D2, A1. No reference audio inputs.
for start_beat, duration_beats, frequency, gain in (
        (0, 1.4, 61.7354, .29), (2, .7, 73.4162, .24), (3.5, .8, 55.0, .27)):
    duration = duration_beats*beat
    for i in range(round(duration*RATE)):
        t = i/RATE
        phase = 2*math.pi*(frequency*t + 8*.018*(1-math.exp(-t/.018)))
        tone = math.sin(phase) + .22*math.sin(2*phase) + .18*math.sin(3*phase)
        envelope = min(1, t/.004)*min(1, (duration-t)/.04)*math.exp(-t/.40)
        value = gain*math.tanh(1.2*tone)*envelope
        index = round(start_beat*beat*RATE)+i
        if index < len(audio):
            audio[index][0] += value
            audio[index][1] += value

# Conservative peak headroom; natural envelopes decay into trailing silence.
peak = max(abs(s) for frame in audio for s in frame)
scale = .76/peak
pcm = b''.join(struct.pack('<hh', *(round(s*scale*32767) for s in frame)) for frame in audio)
with wave.open(str(OUTPUT), 'wb') as output:
    output.setnchannels(2)
    output.setsampwidth(2)
    output.setframerate(RATE)
    output.writeframes(pcm)
print(OUTPUT)
print('SHA-256:', hashlib.sha256(OUTPUT.read_bytes()).hexdigest())
