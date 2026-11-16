#!/usr/bin/env python3
"""Compose Continuum GitHub Social preview at 1280x640 without stretching the mark."""

from __future__ import annotations

import importlib.util
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
W, H = 1280, 640
INK = (11, 18, 32, 255)
CYAN = (0, 229, 255, 255)
PURPLE = (224, 64, 251, 255)


def _load_infinity():
    spec = importlib.util.spec_from_file_location(
        "generate_infinity_mark",
        ROOT / "scripts" / "generate-infinity-mark.py",
    )
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod.draw_infinity_mark


def compose() -> Image.Image:
    draw_infinity_mark = _load_infinity()
    canvas = Image.new("RGBA", (W, H), INK)

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy = 90 + 185, H // 2
    for radius, alpha in ((220, 28), (160, 40), (100, 55)):
        gd.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=(0, 180, 220, alpha))
    for radius, alpha in ((200, 22), (140, 34)):
        gd.ellipse(
            (cx - radius + 40, cy - radius, cx + radius + 40, cy + radius),
            fill=(200, 40, 230, alpha),
        )
    glow = glow.filter(ImageFilter.GaussianBlur(28))
    canvas = Image.alpha_composite(canvas, glow)

    # True circular geometry — square resize only; plate matched to canvas ink (no box).
    mark_size = 370
    mark = draw_infinity_mark(mark_size, with_plate=True)
    px = mark.load()
    for y in range(mark_size):
        for x in range(mark_size):
            r, g, b, a = px[x, y]
            if a > 200 and r < 30 and g < 40 and b < 50 and abs(r - g) < 15:
                px[x, y] = INK

    left = 90
    top = (H - mark_size) // 2
    canvas.paste(mark, (left, top), mark)

    reflect = mark.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    reflect = reflect.resize((mark_size, int(mark_size * 0.28)), Image.Resampling.LANCZOS)
    ra = reflect.split()[-1].point(lambda v: int(v * 0.22))
    reflect.putalpha(ra)
    reflect = reflect.filter(ImageFilter.GaussianBlur(3))
    canvas.paste(reflect, (left, top + mark_size - 6), reflect)

    draw = ImageDraw.Draw(canvas)
    text_x = left + mark_size + 48
    try:
        font_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 56)
        font_sub = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 26)
        font_meta = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 20)
    except OSError:
        font_title = font_sub = font_meta = ImageFont.load_default()

    draw.text((text_x, H // 2 - 74), "Continuum Calendar", fill=(244, 247, 251, 255), font=font_title)
    draw.text(
        (text_x, H // 2 + 2),
        "Your week, your devices, your rules",
        fill=(168, 179, 199, 255),
        font=font_sub,
    )
    draw.rectangle((text_x, H // 2 + 46, text_x + 240, H // 2 + 50), fill=CYAN)
    draw.text((text_x, H // 2 + 66), "MIT · FOSS · cyan + purple", fill=PURPLE, font=font_meta)
    return canvas.convert("RGB")


def main() -> None:
    rgb = compose()
    out_png = ROOT / "docs" / "brand" / "github-social-neon.png"
    out_jpg = ROOT / "docs" / "brand" / "github-social-neon-upload.jpg"
    out_png.parent.mkdir(parents=True, exist_ok=True)
    rgb.save(out_png, format="PNG", optimize=True)
    rgb.save(out_jpg, format="JPEG", quality=92, optimize=True)
    downloads = Path.home() / "Downloads" / "continuum-github-social-preview.jpg"
    downloads.write_bytes(out_jpg.read_bytes())
    print(f"wrote {out_png} {rgb.size} {out_png.stat().st_size}B")
    print(f"wrote {out_jpg} {out_jpg.stat().st_size}B")
    print(f"copied {downloads}")


if __name__ == "__main__":
    main()
