"""
Render a holographic human "turntable" for the CrewPulse body view (draft, variant 4).

The body and organs are built from simple 3D primitives (capsules, ellipsoids) and ray-marched
with signed distance functions, so no external 3D model or license is needed. Every layer is
rendered see-through (hologram style) from N angles around the vertical axis.

Output (android/app/src/main/assets/body_turntable/):
  sheet_0.png  R = body shell, G = radiation halo, B = brain (behavioral)
  sheet_1.png  R = eyes (vision), G = heart (cardiovascular), B = abdomen (metabolic)
  sheet_2.png  R = lymph nodes (immune), G = skeleton (musculoskeletal), B = unused
  meta.json    frame grid and which channel holds which system
Each sheet is a grid of frames (COLS x ROWS); the app tints each channel with the status color.

Usage: python render_turntable.py [out_dir]   (needs numpy, pillow)
"""
import json
import os
import sys
from multiprocessing import Pool

import numpy as np
from PIL import Image

FRAMES = 24          # 15 degrees per frame; the app cross-fades between neighbours
COLS, ROWS = 6, 4
W, H = 256, 416      # frame size in the sheet
SS = 2               # supersampling factor for anti-aliasing
Y0, Y1 = -0.04, 1.90 # visible height range (body is ~1.78 tall)

# ---------- SDF primitives (p: (N, 3)) ----------

def capsule(p, a, b, r):
    a, b = np.asarray(a, float), np.asarray(b, float)
    pa, ba = p - a, b - a
    h = np.clip((pa @ ba) / (ba @ ba), 0.0, 1.0)
    return np.linalg.norm(pa - h[:, None] * ba, axis=1) - r


def ellipsoid(p, c, r):
    q = (p - np.asarray(c, float)) / np.asarray(r, float)
    k0 = np.linalg.norm(q, axis=1)
    k1 = np.linalg.norm(q / np.asarray(r, float), axis=1)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-9)


def sphere(p, c, r):
    return np.linalg.norm(p - np.asarray(c, float), axis=1) - r


def smin(a, b, k):
    h = np.maximum(k - np.abs(a - b), 0.0) / k
    return np.minimum(a, b) - h * h * k * 0.25


# ---------- Model ----------

def body(p):
    d = ellipsoid(p, (0, 1.635, 0.0), (0.085, 0.115, 0.1))                       # head
    d = smin(d, capsule(p, (0, 1.44, -0.01), (0, 1.55, 0.0), 0.045), 0.04)        # neck
    d = smin(d, ellipsoid(p, (0, 1.27, 0), (0.165, 0.17, 0.1)), 0.06)             # chest
    d = smin(d, ellipsoid(p, (0, 1.05, 0.005), (0.14, 0.15, 0.09)), 0.06)         # abdomen
    d = smin(d, ellipsoid(p, (0, 0.9, 0), (0.155, 0.1, 0.1)), 0.05)              # pelvis
    d = smin(d, capsule(p, (-0.16, 1.39, -0.01), (0.16, 1.39, -0.01), 0.055), 0.05)  # shoulders
    for s in (-1, 1):
        d = smin(d, capsule(p, (s * 0.2, 1.38, -0.01), (s * 0.245, 1.1, -0.01), 0.043), 0.03)   # upper arm
        d = smin(d, capsule(p, (s * 0.245, 1.1, -0.01), (s * 0.27, 0.84, 0.02), 0.035), 0.02)   # forearm
        d = smin(d, ellipsoid(p, (s * 0.275, 0.77, 0.025), (0.026, 0.065, 0.018)), 0.02)         # hand
        d = smin(d, capsule(p, (s * 0.085, 0.87, 0), (s * 0.095, 0.47, 0.0), 0.068), 0.04)      # thigh
        d = smin(d, capsule(p, (s * 0.095, 0.47, 0.0), (s * 0.1, 0.08, -0.01), 0.048), 0.03)    # shin
        d = smin(d, ellipsoid(p, (s * 0.1, 0.03, 0.04), (0.04, 0.03, 0.095)), 0.03)             # foot
    return d


def brain(p):
    return ellipsoid(p, (0, 1.66, -0.005), (0.068, 0.062, 0.08))


def eyes(p):
    return np.minimum(sphere(p, (-0.033, 1.625, 0.085), 0.016), sphere(p, (0.033, 1.625, 0.085), 0.016))


def heart(p):
    return smin(ellipsoid(p, (0.025, 1.27, 0.03), (0.048, 0.058, 0.042)),
                ellipsoid(p, (0.045, 1.24, 0.035), (0.035, 0.04, 0.035)), 0.02)


def abdomen(p):
    liver = ellipsoid(p, (-0.045, 1.13, 0.015), (0.085, 0.045, 0.06))
    stomach = ellipsoid(p, (0.06, 1.1, 0.03), (0.05, 0.04, 0.04))
    gut = ellipsoid(p, (0.0, 1.0, 0.02), (0.085, 0.06, 0.05))
    return np.minimum(np.minimum(liver, stomach), gut)


def lymph(p):
    d = np.full(len(p), 1e9)
    for c in [(-0.04, 1.47, 0.02), (0.04, 1.47, 0.02), (-0.13, 1.33, 0.0), (0.13, 1.33, 0.0),
              (-0.07, 0.86, 0.045), (0.07, 0.86, 0.045), (0.0, 1.22, -0.02)]:
        d = np.minimum(d, sphere(p, c, 0.017))
    return d


def skeleton(p):
    d = capsule(p, (0, 1.52, -0.045), (0, 0.93, -0.06), 0.017)                      # spine
    d = np.minimum(d, ellipsoid(p, (0, 0.92, -0.01), (0.12, 0.05, 0.06)) )            # pelvis
    for s in (-1, 1):
        d = np.minimum(d, capsule(p, (s * 0.02, 1.36, 0.06), (s * 0.15, 1.39, 0.0), 0.012))  # clavicle
        d = np.minimum(d, capsule(p, (s * 0.2, 1.37, -0.01), (s * 0.243, 1.11, -0.01), 0.014))  # humerus
        d = np.minimum(d, capsule(p, (s * 0.085, 0.86, 0), (s * 0.095, 0.49, 0.0), 0.02))       # femur
        d = np.minimum(d, capsule(p, (s * 0.095, 0.46, 0.0), (s * 0.1, 0.1, -0.01), 0.016))     # tibia
    for i in range(6):                                                                     # ribs
        y = 1.36 - i * 0.045
        for s in (-1, 1):
            d = np.minimum(d, capsule(p, (s * 0.02, y, -0.06), (s * 0.13, y - 0.03, 0.02), 0.007))
    return d

# ---------- Rendering ----------

def march(sdf, ro, rd, steps=90, tmax=4.0, track_min=False):
    n = len(ro)
    t = np.zeros(n)
    hit = np.zeros(n, bool)
    alive = np.ones(n, bool)
    mind = np.full(n, 1e9)
    for _ in range(steps):
        idx = np.nonzero(alive)[0]
        if len(idx) == 0:
            break
        d = sdf(ro[idx] + rd[idx] * t[idx, None])
        if track_min:
            mind[idx] = np.minimum(mind[idx], d)
        h = d < 1e-4
        hit[idx[h]] = True
        t[idx] += np.maximum(d, 1e-4)
        alive[idx] = ~h & (t[idx] < tmax)
    return t, hit, mind


def normals(sdf, p):
    e = 1e-3
    n = np.stack([sdf(p + [e, 0, 0]) - sdf(p - [e, 0, 0]),
                  sdf(p + [0, e, 0]) - sdf(p - [0, e, 0]),
                  sdf(p + [0, 0, e]) - sdf(p - [0, 0, e])], axis=1)
    return n / np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-9)


def shade(sdf, ro, rd, base, rim_w, extra=None):
    t, hit, _ = march(sdf, ro, rd)
    out = np.zeros(len(ro))
    if hit.any():
        p = ro[hit] + rd[hit] * t[hit, None]
        n = normals(sdf, p)
        rim = 1.0 - np.abs(np.sum(n * rd[hit], axis=1))
        v = base + rim_w * rim ** 2.0
        if extra is not None:
            v = v + extra(p)
        out[hit] = np.clip(v, 0, 1)
    return out


def render_frame(i):
    ang = 2 * np.pi * i / FRAMES
    w, h = W * SS, H * SS
    half_w = (Y1 - Y0) * W / H / 2
    xs = np.linspace(-half_w, half_w, w)
    ys = np.linspace(Y1, Y0, h)
    gx, gy = np.meshgrid(xs, ys)
    # Orthographic camera looking along -z, orbiting the body by `ang`.
    c, s = np.cos(ang), np.sin(ang)
    ro = np.stack([gx.ravel() * c + 2.0 * s, gy.ravel(), -gx.ravel() * s + 2.0 * c], axis=1)
    rd = np.tile([-s, 0.0, -c], (len(ro), 1))

    def body_lines(p):
        # Meridians and latitude lines on the shell: they turn with the body and show the rotation.
        lon = np.degrees(np.arctan2(p[:, 2], p[:, 0])) % 20.0
        lat = (p[:, 1] * 50.0) % 1.0
        return 0.22 * (np.minimum(lon, 20 - lon) < 0.9) + 0.12 * (lat < 0.08)

    t, hit, mind = march(body, ro, rd, track_min=True)
    shell = np.zeros(len(ro))
    if hit.any():
        p = ro[hit] + rd[hit] * t[hit, None]
        n = normals(body, p)
        rim = 1.0 - np.abs(np.sum(n * rd[hit], axis=1))
        shell[hit] = np.clip(0.10 + 0.85 * rim ** 2.2 + body_lines(p), 0, 1)
    halo = np.where(hit, 0.0, np.exp(-np.maximum(mind, 0) / 0.035) * 0.9)

    layers = [
        shell, halo,
        shade(brain, ro, rd, 0.35, 0.6),
        shade(eyes, ro, rd, 0.6, 0.4),
        shade(heart, ro, rd, 0.45, 0.55),
        shade(abdomen, ro, rd, 0.35, 0.55),
        shade(lymph, ro, rd, 0.6, 0.4),
        shade(skeleton, ro, rd, 0.35, 0.6),
    ]
    imgs = [l.reshape(h, w).reshape(H, SS, W, SS).mean(axis=(1, 3)) for l in layers]
    return i, [(im * 255).astype(np.uint8) for im in imgs]


LAYERS = [  # (system id, sheet, channel)
    ("body", 0, 0), ("radiation", 0, 1), ("behavioral", 0, 2),
    ("vision", 1, 0), ("cardiovascular", 1, 1), ("metabolic", 1, 2),
    ("immune", 2, 0), ("musculoskeletal", 2, 1),
]


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, "../../android/app/src/main/assets/body_turntable")
    os.makedirs(out, exist_ok=True)
    sheets = [np.zeros((ROWS * H, COLS * W, 3), np.uint8) for _ in range(3)]
    with Pool() as pool:
        for i, imgs in pool.imap_unordered(render_frame, range(FRAMES)):
            r, c = divmod(i, COLS)
            for (_, sheet, ch), im in zip(LAYERS, imgs):
                sheets[sheet][r * H:(r + 1) * H, c * W:(c + 1) * W, ch] = im
            print(f"frame {i + 1}/{FRAMES}", flush=True)
    for k, sh in enumerate(sheets):
        Image.fromarray(sh, "RGB").save(os.path.join(out, f"sheet_{k}.png"), optimize=True)
    meta = {
        "frames": FRAMES, "cols": COLS, "rows": ROWS, "frameWidth": W, "frameHeight": H,
        "layers": [{"id": i, "sheet": s, "channel": "rgb"[c]} for i, s, c in LAYERS],
    }
    with open(os.path.join(out, "meta.json"), "w") as f:
        json.dump(meta, f, indent=2)
    print("written to", os.path.abspath(out))


if __name__ == "__main__":
    main()
