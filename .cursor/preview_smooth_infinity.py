#!/usr/bin/env python3
"""Preview only — smooth Continuum infinity mark (no bristles). Not applied to app assets."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
CYAN = (0x00, 0xE5, 0xFF, 255)  # slightly brighter cyan for retrowave punch
PURPLE = (0xE0, 0x40, 0xFB, 255)  # vaporwave / retrowave magenta-purple
INK = (0x0B, 0x12, 0x20, 255)


def draw_smooth_infinity(size: int) -> Image.Image:
    im = Image.new("RGBA", (size, size), INK)
    cx = cy = size / 2.0

    stroke = max(3, int(size * 0.08))
    r = size * 0.218
    r_out = r + stroke / 2
    r_in = r - stroke / 2

    # Locked horizontal spacing (dots / C positions)
    open_half = 50.0
    tip_gap = size * 0.006
    lobe_dx = r * math.cos(math.radians(open_half)) + tip_gap / 2
    left_cx = cx - lobe_dx
    right_cx = cx + lobe_dx
    left_tip_x = cx - tip_gap / 2
    right_tip_x = cx + tip_gap / 2

    def blit_clipped(lcx: float, tip_x: float, keep_left_of_tip: bool, color: tuple[int, int, int, int]) -> None:
        """Annulus with vertical clip → flat tip faces aimed at ∞ center."""
        # Build alpha: ring ∩ half-plane
        alpha = Image.new("L", (size, size), 0)
        ad = ImageDraw.Draw(alpha)
        ad.ellipse((lcx - r_out, cy - r_out, lcx + r_out, cy + r_out), fill=255)
        ad.ellipse((lcx - r_in, cy - r_in, lcx + r_in, cy + r_in), fill=0)
        if keep_left_of_tip:
            ad.rectangle((math.ceil(tip_x), 0, size, size), fill=0)
        else:
            ad.rectangle((0, 0, math.floor(tip_x), size), fill=0)

        layer = Image.new("RGBA", (size, size), color)
        layer.putalpha(alpha)
        im.alpha_composite(layer)

    blit_clipped(left_cx, left_tip_x, keep_left_of_tip=True, color=CYAN)
    blit_clipped(right_cx, right_tip_x, keep_left_of_tip=False, color=PURPLE)

    d = ImageDraw.Draw(im)
    # Yin-yang: purple dot in cyan C, cyan dot in purple C
    dot_r = size * 0.04
    d.ellipse((left_cx - dot_r, cy - dot_r, left_cx + dot_r, cy + dot_r), fill=PURPLE)
    d.ellipse((right_cx - dot_r, cy - dot_r, right_cx + dot_r, cy + dot_r), fill=CYAN)

    return im


def main() -> None:
    out = ROOT / ".cursor" / "logo-preview-smooth.png"
    draw_smooth_infinity(1024).save(out)
    draw_smooth_infinity(192).save(ROOT / ".cursor" / "logo-preview-smooth-192.png")
    im = Image.open(out).convert("RGBA")
    im.crop((420, 300, 604, 724)).resize((368, 848), Image.Resampling.NEAREST).save(
        ROOT / ".cursor" / "logo-preview-smooth-tips.png"
    )
    print("wrote", out)


if __name__ == "__main__":
    main()
