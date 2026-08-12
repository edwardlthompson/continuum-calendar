#!/usr/bin/env python3
"""3D glass preview of the locked Continuum mark — marketing mock only."""

from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
import importlib.util

_spec = importlib.util.spec_from_file_location(
    "gim", ROOT / "scripts" / "generate-infinity-mark.py"
)
_gim = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
_spec.loader.exec_module(_gim)
CYAN = _gim.CYAN
PURPLE = _gim.PURPLE
_geometry = _gim._geometry


def c_alpha(size: int, g: dict, lcx: float, tip_x: float, keep_left: bool) -> Image.Image:
    alpha = Image.new("L", (size, size), 0)
    ad = ImageDraw.Draw(alpha)
    cy, r_out, r_in = g["cy"], g["r_out"], g["r_in"]
    ad.ellipse((lcx - r_out, cy - r_out, lcx + r_out, cy + r_out), fill=255)
    ad.ellipse((lcx - r_in, cy - r_in, lcx + r_in, cy + r_in), fill=0)
    if keep_left:
        ad.rectangle((math.ceil(tip_x), 0, size, size), fill=0)
    else:
        ad.rectangle((0, 0, math.floor(tip_x), size), fill=0)
    return alpha


def draw_glass(size: int = 1024) -> Image.Image:
    im = Image.new("RGBA", (size, size), (6, 8, 22, 255))
    # Ambient plate
    vig = Image.new("L", (size, size), 0)
    vd = ImageDraw.Draw(vig)
    for i in range(30):
        t = i / 29
        pad = int(size * 0.02 * t)
        vd.ellipse((pad, pad, size - 1 - pad, size - 1 - pad), fill=int(255 * (1 - t) ** 2))
    amb = Image.new("RGBA", (size, size), (50, 16, 80, 255))
    amb.putalpha(vig.point(lambda v: int(v * 0.4)))
    im.alpha_composite(amb)

    g = _geometry(size)
    lobes = (
        (g["left_cx"], g["left_tip_x"], True, (0, 229, 255), (160, 255, 255)),
        (g["right_cx"], g["right_tip_x"], False, (224, 64, 251), (255, 170, 255)),
    )

    # Soft drop shadow
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for lcx, tip_x, keep_left, _, _ in lobes:
        a = c_alpha(size, g, lcx, tip_x, keep_left)
        layer = Image.new("RGBA", (size, size), (0, 0, 0, 140))
        layer.putalpha(a)
        shadow.alpha_composite(layer)
    ox = int(size * 0.012)
    oy = int(size * 0.018)
    shifted = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shifted.paste(shadow, (ox, oy), shadow)
    shifted = shifted.filter(ImageFilter.GaussianBlur(radius=max(6, size // 36)))
    im.alpha_composite(shifted)

    # Outer neon glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for lcx, tip_x, keep_left, rgb, _ in lobes:
        a = c_alpha(size, g, lcx, tip_x, keep_left)
        layer = Image.new("RGBA", (size, size), (*rgb, 160))
        layer.putalpha(a)
        glow.alpha_composite(layer)
    glow = glow.filter(ImageFilter.GaussianBlur(radius=max(8, size // 28)))
    im.alpha_composite(glow)

    # Frosted translucent glass fill (lower alpha so depth shows)
    for lcx, tip_x, keep_left, rgb, _ in lobes:
        a = c_alpha(size, g, lcx, tip_x, keep_left)
        body = Image.new("RGBA", (size, size), (*rgb, 150))
        body.putalpha(ImageChops.multiply(a, Image.new("L", (size, size), 150)))
        im.alpha_composite(body)
        # Darker inner rim (thickness cue)
        rim = Image.new("L", (size, size), 0)
        rd = ImageDraw.Draw(rim)
        cy, r_in, r_out = g["cy"], g["r_in"], g["r_out"]
        mid = (r_in + r_out) / 2
        rd.ellipse((lcx - mid - 1, cy - mid - 1, lcx + mid + 1, cy + mid + 1), outline=255, width=max(2, int(g["stroke"] * 0.2)))
        rim = ImageChops.multiply(rim, a)
        dark = Image.new("RGBA", (size, size), (10, 10, 30, 0))
        dark.putalpha(rim.point(lambda v: int(v * 0.55)))
        im.alpha_composite(dark)

    # Specular ridge along top of each C (glass reflection)
    for lcx, tip_x, keep_left, _, hi_rgb in lobes:
        a = c_alpha(size, g, lcx, tip_x, keep_left)
        mask = Image.new("L", (size, size), 0)
        md = ImageDraw.Draw(mask)
        bbox = (lcx - g["r_out"], g["cy"] - g["r_out"], lcx + g["r_out"], g["cy"] + g["r_out"])
        md.arc(bbox, start=210, end=330, fill=255, width=max(3, int(g["stroke"] * 0.42)))
        mask = ImageChops.multiply(mask, a)
        mask = mask.filter(ImageFilter.GaussianBlur(radius=max(1, size // 200)))
        spec = Image.new("RGBA", (size, size), (255, 255, 255, 0))
        spec.putalpha(mask.point(lambda v: int(v * 0.9)))
        im.alpha_composite(spec)
        # Colored secondary sheen
        col = Image.new("RGBA", (size, size), (*hi_rgb, 0))
        col.putalpha(mask.point(lambda v: int(v * 0.35)))
        im.alpha_composite(col)

    # Broad soft sheen patches (internal refraction feel)
    sheen = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sheen)
    for lcx, tip_x, keep_left, _, hi_rgb in lobes:
        sd.ellipse(
            (
                lcx - g["r"] * 0.35,
                g["cy"] - g["r"] * 0.7,
                lcx + g["r"] * 0.55,
                g["cy"] - g["r"] * 0.05,
            ),
            fill=(*hi_rgb[:3], 90),
        )
    sheen = sheen.filter(ImageFilter.GaussianBlur(radius=max(4, size // 48)))
    mark_a = Image.new("L", (size, size), 0)
    for lcx, tip_x, keep_left, _, _ in lobes:
        mark_a = ImageChops.lighter(mark_a, c_alpha(size, g, lcx, tip_x, keep_left))
    r_ch, g_ch, b_ch, a_ch = sheen.split()
    sheen = Image.merge("RGBA", (r_ch, g_ch, b_ch, ImageChops.multiply(a_ch, mark_a)))
    im.alpha_composite(sheen)

    # Glass yin-yang dots (translucent orbs + catchlight)
    d = ImageDraw.Draw(im)
    dr = g["dot_r"]
    for lcx, fill, hi in (
        (g["left_cx"], PURPLE, (255, 220, 255, 200)),
        (g["right_cx"], CYAN, (220, 255, 255, 200)),
    ):
        # soft glow under dot
        glow_d = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow_d)
        gd.ellipse((lcx - dr * 1.4, g["cy"] - dr * 1.4, lcx + dr * 1.4, g["cy"] + dr * 1.4), fill=(*fill[:3], 80))
        glow_d = glow_d.filter(ImageFilter.GaussianBlur(radius=max(2, int(dr))))
        im.alpha_composite(glow_d)
        d.ellipse((lcx - dr, g["cy"] - dr, lcx + dr, g["cy"] + dr), fill=(*fill[:3], 210))
        hr = dr * 0.4
        d.ellipse(
            (lcx - hr * 0.2, g["cy"] - dr * 0.65, lcx + hr * 1.2, g["cy"] - dr * 0.1),
            fill=hi,
        )

    # Plate glass rim
    edge = ImageDraw.Draw(im)
    inset = max(3, size // 48)
    edge.rounded_rectangle(
        (inset, inset, size - 1 - inset, size - 1 - inset),
        radius=size // 5,
        outline=(255, 255, 255, 55),
        width=max(2, size // 180),
    )
    return im


def main() -> None:
    out = ROOT / ".cursor" / "logo-preview-glass.png"
    draw_glass(1024).save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
