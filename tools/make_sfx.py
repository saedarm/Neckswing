"""Synthesizes every Neckswing sound and the three music loops.
22050 Hz mono 16-bit WAV into assets/sfx and assets/music. Run: python tools/make_sfx.py
Placeholders: drop in a replacement with the same file name to change a sound."""
import os
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, lfilter

SR = 22050
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")
rng = np.random.default_rng(7)


def t(d): return np.arange(int(SR * d)) / SR
def env(n, a=0.005, r=None):
    e = np.ones(n)
    ai = max(1, int(a * SR)); e[:ai] = np.linspace(0, 1, ai)
    if r is None: r = n / SR
    ri = min(n, max(1, int(r * SR))); e[-ri:] *= np.linspace(1, 0, ri) ** 2
    return e
def noise(d): return rng.uniform(-1, 1, int(SR * d))
def lp(x, f): b, a = butter(2, min(0.99, f / (SR / 2)), "low"); return lfilter(b, a, x)
def hp(x, f): b, a = butter(2, min(0.99, f / (SR / 2)), "high"); return lfilter(b, a, x)
def bp(x, lo, hi): b, a = butter(2, [lo / (SR / 2), min(0.99, hi / (SR / 2))], "band"); return lfilter(b, a, x)
def sweep(f0, f1, d, shape=np.sin):
    tt = t(d); f = np.linspace(f0, f1, len(tt)); return shape(2 * np.pi * np.cumsum(f) / SR)
def sq(x): return np.sign(np.sin(x))
def norm(x, v=0.9):
    m = np.max(np.abs(x)) or 1; return x / m * v
def save(name, x, folder="sfx", v=0.9):
    os.makedirs(os.path.join(ROOT, folder), exist_ok=True)
    wavfile.write(os.path.join(ROOT, folder, name + ".wav"), SR, (norm(x, v) * 32767).astype(np.int16))


S = {}
# neck swing: stretchy rubber "thwip" + boing
S["thwip"] = sweep(1400, 300, 0.18) * env(int(SR * .18), r=.15) + 0.4 * hp(noise(.18), 3000) * env(int(SR * .18), r=.1)
S["release"] = hp(noise(.25), 800) * np.linspace(0.2, 1, int(SR * .25)) * env(int(SR * .25), r=.1)
S["jump"] = sweep(220, 520, 0.14) * env(int(SR * .14))
S["land"] = lp(noise(.25), 300) * env(int(SR * .25), r=.22) + 0.6 * sweep(90, 40, .25) * env(int(SR * .25))
S["spin"] = bp(noise(.45), 400, 1500) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t(.45))) * env(int(SR * .45), r=.2)
S["hit"] = lp(noise(.12), 1800) * env(int(SR * .12), r=.1) + 0.7 * sweep(180, 60, .12) * env(int(SR * .12))
S["hit_big"] = lp(noise(.3), 1200) * env(int(SR * .3), r=.28) + sweep(140, 35, .3) * env(int(SR * .3))
S["kick"] = sweep(260, 50, .2) * env(int(SR * .2)) + 0.5 * lp(noise(.2), 900) * env(int(SR * .2), r=.15)
S["slam"] = lp(noise(.9), 250) * env(int(SR * .9), r=.85) + sweep(70, 25, .9) * env(int(SR * .9))
# giraffe bellow: low hum with growl (giraffes really do hum at night)
bt = t(1.3)
growl = 1 + 0.4 * np.sin(2 * np.pi * 23 * bt)
S["bellow"] = (np.sin(2 * np.pi * 92 * bt) + 0.6 * np.sin(2 * np.pi * 184 * bt) + 0.3 * sq(2 * np.pi * 46 * bt)) * growl * env(len(bt), a=.08, r=.4)
S["tongue"] = sweep(700, 200, .3) * (0.6 + 0.4 * np.sin(2 * np.pi * 30 * t(.3))) * env(int(SR * .3)) + 0.3 * bp(noise(.3), 1500, 4000) * env(int(SR * .3), r=.2)
mt = t(.6)
S["munch"] = lp(noise(.6), 2500) * (np.sin(2 * np.pi * 7 * mt) > 0.3) * env(len(mt))
S["look"] = sum(np.sin(2 * np.pi * f * t(.5)) * env(int(SR * .5), r=.4) for f in (880, 1320)) / 2
S["gunshot"] = hp(noise(.22), 400) * env(int(SR * .22), a=.001, r=.2) + 0.5 * sweep(300, 60, .22) * env(int(SR * .22))
S["hurt"] = sweep(500, 250, .15, sq) * env(int(SR * .15)) * 0.5
S["down"] = sweep(400, 120, .35) * env(int(SR * .35)) + 0.3 * lp(noise(.35), 800) * env(int(SR * .35), r=.3)
S["car_door"] = lp(noise(.15), 600) * env(int(SR * .15), a=.001, r=.12) + 0.5 * sweep(160, 80, .15) * env(int(SR * .15))
S["crash"] = lp(noise(.7), 2500) * env(int(SR * .7), a=.001, r=.65) + 0.5 * bp(noise(.7), 2500, 6000) * env(int(SR * .7), r=.4)
ht = t(.6)
S["horn"] = (sq(2 * np.pi * 349 * ht) + sq(2 * np.pi * 440 * ht)) * 0.3 * env(len(ht), r=.05)
S["ring"] = sum(np.sin(2 * np.pi * f * t(.4)) * env(int(SR * .4), r=.35) for f in (1046, 1568, 2093)) / 3
def arp(notes, d=0.11, shape=np.sin):
    return np.concatenate([shape(2 * np.pi * f * t(d)) * env(int(SR * d), r=d * .8) for f in notes])
S["mission_start"] = arp([392, 523, 659, 784], .12, sq) * 0.5
S["mission_done"] = arp([523, 659, 784, 1046, 784, 1046], .13, sq) * 0.5
S["fail"] = arp([392, 349, 311, 262], .2, sq) * 0.5
S["pickup"] = arp([660, 990], .09)
S["deliver"] = arp([523, 784, 1046], .1)
S["died"] = sweep(400, 60, 1.0, sq) * env(int(SR * 1.0)) * 0.5
# giraffe grunt for quips when Windows speech isn't available: snort + low hum
gt = t(.45)
S["grunt"] = 0.6 * lp(noise(.45), 700) * env(len(gt), r=.3) + (np.sin(2 * np.pi * 110 * gt) + 0.5 * np.sin(2 * np.pi * 165 * gt)) * env(len(gt), a=.05, r=.3)
S["no_anchor"] = sweep(300, 200, .2, sq) * env(int(SR * .2)) * 0.3
S["shield_break"] = bp(noise(.5), 2000, 7000) * env(int(SR * .5), a=.001, r=.45) + sweep(1200, 200, .5) * env(int(SR * .5)) * 0.4
S["not_ready"] = np.sin(2 * np.pi * 180 * t(.1)) * env(int(SR * .1))
st = t(1.2)
S["crime"] = np.sin(2 * np.pi * np.cumsum(700 + 250 * np.sin(2 * np.pi * 2.5 * st)) / SR) * env(len(st), r=.2) * 0.6
S["step"] = lp(noise(.07), 500) * env(int(SR * .07), a=.001)
S["splat"] = lp(noise(.25), 1000) * env(int(SR * .25), a=.001, r=.22) + 0.5 * sweep(250, 80, .25) * env(int(SR * .25))
S["click"] = np.sin(2 * np.pi * 1200 * t(.04)) * env(int(SR * .04))
# loops
et = t(1.0)
S["engine"] = lp(sq(2 * np.pi * 55 * et) + 0.5 * sq(2 * np.pi * 110.5 * et) + 0.3 * noise(1.0), 900)
S["wind"] = bp(noise(2.0), 300, 1800)
for k, v in S.items(): save(k, v, v=0.8 if k not in ("step", "click") else 0.6)


# ---------------------------------------------------------------- music
def note(f, d, kind="sq", vol=1.0):
    tt = t(d)
    if kind == "sq": w = sq(2 * np.pi * f * tt) * 0.5
    elif kind == "tri": w = 2 / np.pi * np.arcsin(np.sin(2 * np.pi * f * tt))
    else: w = np.sin(2 * np.pi * f * tt)
    return w * env(len(tt), a=.005, r=d * .6) * vol
def kick(d):
    tt = t(d); return np.sin(2 * np.pi * np.cumsum(np.linspace(140, 40, len(tt))) / SR) * env(len(tt), a=.001, r=d * .9)
def snare(d): return hp(noise(d), 1500) * env(int(SR * d), a=.001, r=d * .8) * 0.6
def hat(d): return hp(noise(d), 7000) * env(int(SR * d), a=.001, r=d * .5) * 0.3
def mtof(m): return 440 * 2 ** ((m - 69) / 12)

def track(bpm, bars, bass, lead, drums, lead_kind="sq", swing=0.0):
    beat = 60 / bpm; step = beat / 2
    n = int(SR * step * 8 * bars)
    out = np.zeros(n + SR)
    for i in range(bars * 8):
        pos = int(i * step * SR)
        b = bass[i % len(bass)]
        if b: seg = note(mtof(b), step * 0.95, "tri", 0.6); out[pos:pos + len(seg)] += seg
        l = lead[i % len(lead)]
        if l: seg = note(mtof(l), step * 0.9, lead_kind, 0.35); out[pos:pos + len(seg)] += seg
        d = drums[i % len(drums)]
        if "k" in d: seg = kick(.25); out[pos:pos + len(seg)] += seg * 0.8
        if "s" in d: seg = snare(.18); out[pos:pos + len(seg)] += seg
        if "h" in d: seg = hat(.05); out[pos:pos + len(seg)] += seg
    return out[:n]

# menu: relaxed savanna shuffle
menu = track(92, 8,
             [45, 0, 52, 0, 50, 0, 52, 0, 43, 0, 50, 0, 48, 0, 50, 0],
             [69, 0, 72, 74, 76, 0, 74, 0, 72, 0, 69, 0, 67, 0, 69, 0, 69, 0, 72, 74, 76, 79, 76, 0, 74, 72, 69, 0, 72, 0, 0, 0],
             ["k", "h", "h", "h", "s", "h", "h", "h"], "tri")
# city: bouncy free-roam groove
city = track(118, 8,
             [41, 41, 53, 41, 43, 43, 55, 43, 45, 45, 57, 45, 43, 43, 55, 43],
             [65, 0, 69, 0, 72, 0, 69, 72, 74, 0, 72, 0, 69, 0, 67, 0, 69, 0, 72, 0, 76, 0, 74, 72, 74, 0, 0, 72, 69, 0, 0, 0],
             ["kh", "h", "sh", "h", "kh", "kh", "sh", "h"], "sq")
# action: fast minor chase
action = track(148, 8,
               [40, 40, 52, 40, 40, 52, 40, 43, 41, 41, 53, 41, 38, 38, 50, 43],
               [76, 0, 76, 79, 0, 76, 74, 0, 72, 0, 72, 74, 0, 72, 71, 0, 76, 0, 79, 81, 0, 79, 76, 0, 74, 76, 74, 72, 71, 0, 71, 0],
               ["kh", "h", "sh", "kh", "kh", "h", "sh", "h"], "sq")
save("menu", lp(menu, 4000), "music", 0.7)
save("city", lp(city, 4500), "music", 0.7)
save("action", lp(action, 5000), "music", 0.75)
print("wrote", len(S), "sounds and 3 music loops")


# ---------------------------------------------------------------- added: skills, snipers, cars, peds, races
X = {}
X["zip"] = sweep(300, 1800, .35) * env(int(SR * .35), r=.1) + 0.4 * hp(noise(.35), 2000) * np.linspace(0.2, 1, int(SR * .35))
X["alert"] = arp([660, 880], .08, sq) * 0.5
X["laser"] = np.sin(2 * np.pi * 2200 * t(.12)) * env(int(SR * .12)) * 0.5
X["sniper"] = hp(noise(.5), 300) * env(int(SR * .5), a=.001, r=.48) + sweep(500, 40, .5) * env(int(SR * .5)) * 0.8
ft = t(1.0)
X["fire"] = lp(noise(1.0), 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 6 * ft)) * env(len(ft), a=.1, r=.4)
X["explode"] = lp(noise(1.4), 500) * env(int(SR * 1.4), a=.001, r=1.35) + sweep(80, 20, 1.4) * env(int(SR * 1.4)) + 0.3 * bp(noise(1.4), 2000, 6000) * env(int(SR * 1.4), r=.6)
st2 = t(.7)
X["scream"] = np.sin(2 * np.pi * np.cumsum(900 + 300 * np.sin(2 * np.pi * 7 * st2) - 400 * st2) / SR) * env(len(st2), a=.03, r=.3) * 0.5
X["checkpoint"] = arp([880, 1320], .07)
X["race_start"] = np.concatenate([note(440, .25, "sq"), np.zeros(int(SR * .25)), note(440, .25, "sq"), np.zeros(int(SR * .25)), note(880, .5, "sq")])
X["medal"] = arp([523, 659, 784, 1046, 1318], .1, sq) * 0.5
X["leaf"] = sum(np.sin(2 * np.pi * f * t(.5)) * env(int(SR * .5), r=.45) for f in (1318, 1760, 2637)) / 3
X["charge"] = sweep(120, 260, .5, sq) * env(int(SR * .5)) * 0.4 + 0.5 * lp(noise(.5), 600) * env(int(SR * .5))
X["ping"] = np.sin(2 * np.pi * 3200 * t(.15)) * np.exp(-t(.15) * 30)
X["buy"] = arp([784, 988, 1175, 1568], .08)
X["ped_hit"] = lp(noise(.15), 1500) * env(int(SR * .15), a=.001) + 0.5 * sweep(600, 300, .15) * env(int(SR * .15))
for k, v in X.items(): save(k, v)
print("wrote", len(X), "more sounds")
