"""Generate original, quiet UI effects for Nexus Tycoon. Python stdlib only.

Outputs 16 kHz mono PCM WAV. All synthesis parameters and source are retained so
the shipped sounds can be recreated or replaced without external assets.
"""
from pathlib import Path
import math
import random
import struct
import wave

RATE = 16000
DEST = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
DEST.mkdir(parents=True, exist_ok=True)


def tone(name, notes, duration, noise=0.0, sweep=0.0, gain=0.22):
    rng = random.Random(1917)
    count = int(RATE * duration)
    samples = []
    for i in range(count):
        t = i / RATE
        attack = min(1.0, t / 0.012)
        release = max(0.0, 1.0 - t / duration) ** 1.65
        sample = sum(
            math.sin(2 * math.pi * (frequency * t + sweep * t * t) + phase) * weight
            for frequency, weight, phase in notes
        )
        sample += noise * (rng.random() * 2 - 1)
        sample *= attack * release * gain
        samples.append(max(-0.82, min(0.82, sample)))
    with wave.open(str(DEST / (name + ".wav")), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(b"".join(struct.pack("<h", int(value * 32767)) for value in samples))


tone("sfx_ui", [(560, .75, 0), (840, .15, 0)], .065, gain=.14)
tone("sfx_confirm", [(440, .55, 0), (660, .4, 0)], .18)
tone("sfx_unlock", [(300, .4, 0), (600, .4, 0), (900, .15, 0)], .45, sweep=85)
tone("sfx_milestone", [(392, .45, 0), (588, .35, 0), (784, .2, 0)], .50, sweep=48)
tone("sfx_pack", [(210, .55, 0), (420, .25, 0)], .40, noise=.04, sweep=155)
tone("sfx_reveal_common", [(523, .55, 0), (659, .35, 0)], .32)
tone("sfx_reveal_rare", [(494, .45, 0), (740, .35, 0), (988, .12, 0)], .42)
tone("sfx_reveal_epic", [(523, .35, 0), (784, .35, 0), (1047, .25, 0)], .54)
tone("sfx_reveal_supreme", [(392, .25, 0), (588, .3, 0), (784, .25, 0), (1175, .2, 0)], .74)
tone("sfx_ascend", [(180, .55, 0), (270, .25, 0)], .85, noise=.025, sweep=115)
tone("sfx_attack", [(170, .5, 0), (310, .18, 0)], .13, noise=.22, sweep=-140)
tone("sfx_skill", [(360, .5, 0), (720, .25, 0)], .30, sweep=160)
tone("sfx_victory", [(440, .35, 0), (554, .3, 0), (659, .25, 0)], .70)
tone("sfx_defeat", [(330, .45, 0), (247, .4, 0)], .52, sweep=-55)
