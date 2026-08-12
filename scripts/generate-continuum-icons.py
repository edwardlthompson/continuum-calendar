#!/usr/bin/env python3
"""Rasterize Continuum brand mark into Android + Tauri icon slots (UTF-8)."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TEAL = (0x0F, 0x6E, 0x8C, 255)
BRIGHT = (0x4E, 0xB6, 0xD4, 255)
INK = (0x0B, 0x12, 0x20, 255)
WHITE = (255, 255, 255, 255)


def _arc_points(cx: float, cy: float, r: float, start_deg: float, sweep_deg: float, n: int) -> list[tuple[float, float]]:
    pts: list[tuple[float, float]] = []
    for i in range(n + 1):
        a = math.radians(start_deg + sweep_deg * i / n)
        # math: 0° = east, CCW — match brand SVG rotate(-115) continuum
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def draw_mark(
    size: int,
    *,
    plate: bool = True,
    mono: bool = False,
    fg_only: bool = False,
) -> Image.Image:
    """Draw Continuum C-arc mark. fg_only: transparent bg for adaptive foreground."""
    bg = (0, 0, 0, 0) if (fg_only or not plate) else INK
    im = Image.new("RGBA", (size, size), bg)
    d = ImageDraw.Draw(im)
    cx = cy = size / 2
    r = size * (0.28 if fg_only else 0.30)
    stroke = max(2, int(size * (0.085 if fg_only else 0.094)))
    teal = WHITE if mono else TEAL
    bright = (255, 255, 255, 230) if mono else BRIGHT

    # Outer continuum arc (~308°), gap near upper-right
    start = math.degrees(math.atan2(-1, 0)) - 25  # near top, slight rotate
    start = -115
    sweep = 308
    pts = _arc_points(cx, cy, r, start, sweep, max(48, size // 2))
    d.line(pts, fill=teal, width=stroke, joint="curve")

    r2 = size * 0.172
    stroke2 = max(1, int(size * 0.031))
    ring = _arc_points(cx, cy, r2, 0, 360, max(36, size // 3))
    d.line(ring + [ring[0]], fill=bright, width=stroke2, joint="curve")

    tw = max(2, int(size * 0.062))
    th = max(4, int(size * 0.14))
    top = cy - r
    d.rounded_rectangle(
        [cx - tw / 2, top - th * 0.05, cx + tw / 2, top + th * 0.65],
        radius=max(1, tw / 2),
        fill=bright,
    )
    return im


def solid(size: int, color: tuple[int, int, int, int]) -> Image.Image:
    return Image.new("RGBA", (size, size), color)


def save(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, format="PNG")
    print(f"wrote {path.relative_to(ROOT)}")


def main() -> None:
    dens = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432,
    }
    mipmap = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    res = ROOT / "apps/mobile/app/src/main/res"
    for name, size in dens.items():
        fg = draw_mark(size, fg_only=True)
        mono = draw_mark(size, fg_only=True, mono=True)
        bg = solid(size, INK)
        save(fg, res / f"drawable-{name}" / "ic_launcher_foreground.png")
        save(mono, res / f"drawable-{name}" / "ic_launcher_monochrome.png")
        save(bg, res / f"drawable-{name}" / "ic_launcher_background.png")

    for name, size in mipmap.items():
        # Legacy mipmap launcher: rounded plate + mark
        plate = draw_mark(size * 4, plate=True).resize((size, size), Image.Resampling.LANCZOS)
        save(plate, res / f"mipmap-{name}" / "ic_launcher.png")

    # Master + Tauri sources
    master = draw_mark(1024, plate=True)
    tauri = ROOT / "apps/desktop/src-tauri/icons"
    save(master, tauri / "icon.png")
    save(master.resize((32, 32), Image.Resampling.LANCZOS), tauri / "32x32.png")
    save(master.resize((128, 128), Image.Resampling.LANCZOS), tauri / "128x128.png")
    save(master.resize((256, 256), Image.Resampling.LANCZOS), tauri / "128x128@2x.png")

    # Multi-size ICO
    ico_sizes = [16, 24, 32, 48, 64, 128, 256]
    icos = [master.resize((s, s), Image.Resampling.LANCZOS) for s in ico_sizes]
    ico_path = tauri / "icon.ico"
    icos[0].save(ico_path, format="ICO", sizes=[(s, s) for s in ico_sizes])
    print(f"wrote {ico_path.relative_to(ROOT)}")

    # Store / Square logos
    for name, size in [
        ("Square30x30Logo.png", 30),
        ("Square44x44Logo.png", 44),
        ("Square71x71Logo.png", 71),
        ("Square89x89Logo.png", 89),
        ("Square107x107Logo.png", 107),
        ("Square142x142Logo.png", 142),
        ("Square150x150Logo.png", 150),
        ("Square284x284Logo.png", 284),
        ("Square310x310Logo.png", 310),
        ("StoreLogo.png", 50),
    ]:
        save(master.resize((size, size), Image.Resampling.LANCZOS), tauri / name)

    # Public SVG companion raster + graphics
    save(master.resize((256, 256), Image.Resampling.LANCZOS), ROOT / "apps/desktop/public/continuum-mark.png")
    graphics = ROOT / "apps/mobile/graphics"
    if graphics.is_dir():
        webp = graphics / "icon.webp"
        master.resize((512, 512), Image.Resampling.LANCZOS).save(webp, format="WEBP", quality=90)
        print(f"wrote {webp.relative_to(ROOT)}")

    # Hi-res for `tauri icon` if CLI available later
    save(master, ROOT / "apps/desktop/src-tauri/app-icon.png")


if __name__ == "__main__":
    main()
