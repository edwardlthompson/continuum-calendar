#!/usr/bin/env python3
"""Generate Continuum cyan+purple smooth infinity mark (canonical brand icons)."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
CYAN = (0x00, 0xE5, 0xFF, 255)
PURPLE = (0xE0, 0x40, 0xFB, 255)
INK = (0x0B, 0x12, 0x20, 255)


def _geometry(size: int) -> dict[str, float]:
    stroke = max(3, int(size * 0.08))
    r = size * 0.218
    open_half = 50.0
    tip_gap = size * 0.006
    lobe_dx = r * math.cos(math.radians(open_half)) + tip_gap / 2
    cx = cy = size / 2.0
    return {
        "stroke": float(stroke),
        "r": r,
        "r_out": r + stroke / 2,
        "r_in": r - stroke / 2,
        "tip_gap": tip_gap,
        "lobe_dx": lobe_dx,
        "cx": cx,
        "cy": cy,
        "left_cx": cx - lobe_dx,
        "right_cx": cx + lobe_dx,
        "left_tip_x": cx - tip_gap / 2,
        "right_tip_x": cx + tip_gap / 2,
        "dot_r": size * 0.04,
    }


def _blit_clipped(
    im: Image.Image,
    size: int,
    g: dict[str, float],
    lcx: float,
    tip_x: float,
    keep_left_of_tip: bool,
    color: tuple[int, int, int, int],
) -> None:
    alpha = Image.new("L", (size, size), 0)
    ad = ImageDraw.Draw(alpha)
    cy, r_out, r_in = g["cy"], g["r_out"], g["r_in"]
    ad.ellipse((lcx - r_out, cy - r_out, lcx + r_out, cy + r_out), fill=255)
    ad.ellipse((lcx - r_in, cy - r_in, lcx + r_in, cy + r_in), fill=0)
    if keep_left_of_tip:
        ad.rectangle((math.ceil(tip_x), 0, size, size), fill=0)
    else:
        ad.rectangle((0, 0, math.floor(tip_x), size), fill=0)
    layer = Image.new("RGBA", (size, size), color)
    layer.putalpha(alpha)
    im.alpha_composite(layer)


def draw_infinity_mark(
    size: int,
    *,
    with_plate: bool = True,
    transparent_bg: bool = False,
) -> Image.Image:
    """Canonical Continuum mark: cyan+purple smooth ∞, vertical tips, yin-yang dots."""
    if transparent_bg:
        im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    elif with_plate:
        im = Image.new("RGBA", (size, size), INK)
    else:
        im = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    g = _geometry(size)
    _blit_clipped(im, size, g, g["left_cx"], g["left_tip_x"], True, CYAN)
    _blit_clipped(im, size, g, g["right_cx"], g["right_tip_x"], False, PURPLE)
    d = ImageDraw.Draw(im)
    dr = g["dot_r"]
    d.ellipse(
        (g["left_cx"] - dr, g["cy"] - dr, g["left_cx"] + dr, g["cy"] + dr),
        fill=PURPLE,
    )
    d.ellipse(
        (g["right_cx"] - dr, g["cy"] - dr, g["right_cx"] + dr, g["cy"] + dr),
        fill=CYAN,
    )
    return im


def draw_glass_infinity(size: int) -> Image.Image:
    """3D glass / vaporwave preview (marketing mock — not the flat launcher source)."""
    # Soft dark gradient plate
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base = Image.new("RGBA", (size, size), (8, 10, 28, 255))
    # Radial vignette
    vig = Image.new("L", (size, size), 0)
    vd = ImageDraw.Draw(vig)
    for i in range(24):
        t = i / 23
        pad = int(size * 0.02 * t)
        vd.ellipse((pad, pad, size - 1 - pad, size - 1 - pad), fill=int(40 + 180 * (1 - t)))
    tint = Image.new("RGBA", (size, size), (40, 10, 80, 255))
    tint.putalpha(vig.point(lambda v: int(v * 0.35)))
    base.alpha_composite(tint)
    im.alpha_composite(base)

    g = _geometry(size)
    # Soft colored glow behind mark
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    flat = draw_infinity_mark(size, transparent_bg=True)
    # Expand glow via blur of solid colors
    glow_src = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    _blit_clipped(glow_src, size, g, g["left_cx"], g["left_tip_x"], True, (0, 229, 255, 180))
    _blit_clipped(glow_src, size, g, g["right_cx"], g["right_tip_x"], False, (224, 64, 251, 180))
    glow_src = glow_src.filter(ImageFilter.GaussianBlur(radius=max(2, size // 48)))
    im.alpha_composite(glow_src)

    # Translucent glass body
    body = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    _blit_clipped(body, size, g, g["left_cx"], g["left_tip_x"], True, (0, 229, 255, 200))
    _blit_clipped(body, size, g, g["right_cx"], g["right_tip_x"], False, (224, 64, 251, 200))
    im.alpha_composite(body)

    # Specular rim (bright edge on upper-left of each lobe)
    spec = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(spec)
    for lcx, tip_x, keep_left in (
        (g["left_cx"], g["left_tip_x"], True),
        (g["right_cx"], g["right_tip_x"], False),
    ):
        # Thin bright arc on outer upper quadrant
        bbox = (
            lcx - g["r_out"],
            g["cy"] - g["r_out"],
            lcx + g["r_out"],
            g["cy"] + g["r_out"],
        )
        # Approximate highlight with thick arc via pieslice on mask then clip
        mask = Image.new("L", (size, size), 0)
        md = ImageDraw.Draw(mask)
        md.arc(bbox, start=200, end=320, fill=255, width=max(2, int(g["stroke"] * 0.35)))
        # Also clip to C half-plane
        if keep_left:
            md.rectangle((math.ceil(tip_x), 0, size, size), fill=0)
        else:
            md.rectangle((0, 0, math.floor(tip_x), size), fill=0)
        # Keep only within annulus
        ring = Image.new("L", (size, size), 0)
        rd = ImageDraw.Draw(ring)
        rd.ellipse(
            (lcx - g["r_out"], g["cy"] - g["r_out"], lcx + g["r_out"], g["cy"] + g["r_out"]),
            fill=255,
        )
        rd.ellipse(
            (lcx - g["r_in"], g["cy"] - g["r_in"], lcx + g["r_in"], g["cy"] + g["r_in"]),
            fill=0,
        )
        mask = ImageChops.multiply(mask, ring)
        hi = Image.new("RGBA", (size, size), (255, 255, 255, 0))
        hi.putalpha(mask.point(lambda v: int(v * 0.85)))
        spec.alpha_composite(hi)
    spec = spec.filter(ImageFilter.GaussianBlur(radius=max(1, size // 256)))
    im.alpha_composite(spec)

    # Inner glass sheen (soft white gradient blobs)
    sheen = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shd = ImageDraw.Draw(sheen)
    for lcx, col in ((g["left_cx"], (180, 255, 255, 70)), (g["right_cx"], (255, 180, 255, 70))):
        rr = g["r"] * 0.55
        shd.ellipse(
            (lcx - rr * 0.6, g["cy"] - g["r"] * 0.55, lcx + rr * 0.9, g["cy"] - g["r"] * 0.05),
            fill=col,
        )
    sheen = sheen.filter(ImageFilter.GaussianBlur(radius=max(2, size // 64)))
    # Mask sheen to mark alpha
    mark_a = flat.split()[-1]
    r_ch, g_ch, b_ch, a_ch = sheen.split()
    sheen = Image.merge("RGBA", (r_ch, g_ch, b_ch, ImageChops.multiply(a_ch, mark_a)))
    im.alpha_composite(sheen)

    # Yin-yang glass dots with highlight
    d = ImageDraw.Draw(im)
    dr = g["dot_r"]
    for lcx, fill, hi in (
        (g["left_cx"], PURPLE, (255, 200, 255, 160)),
        (g["right_cx"], CYAN, (200, 255, 255, 160)),
    ):
        d.ellipse((lcx - dr, g["cy"] - dr, lcx + dr, g["cy"] + dr), fill=fill)
        hr = dr * 0.35
        d.ellipse(
            (lcx - hr * 0.3, g["cy"] - dr * 0.55, lcx + hr * 1.1, g["cy"] - dr * 0.05),
            fill=hi,
        )

    # Rounded plate edge highlight
    edge = ImageDraw.Draw(im)
    inset = max(2, size // 64)
    edge.rounded_rectangle(
        (inset, inset, size - 1 - inset, size - 1 - inset),
        radius=size // 5,
        outline=(255, 255, 255, 40),
        width=max(1, size // 256),
    )
    return im


def write_svg(path: Path) -> None:
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" role="img" aria-label="Continuum Calendar">
  <rect width="512" height="512" rx="96" fill="#0B1220"/>
  <!-- Simplified vector silhouette of cyan+purple ∞ (raster is canonical) -->
  <g fill="none" stroke-width="42" stroke-linecap="butt">
    <path d="M248 118 A112 112 0 1 0 248 394" stroke="#00E5FF"/>
    <path d="M264 118 A112 112 0 1 1 264 394" stroke="#E040FB"/>
  </g>
  <circle cx="190" cy="256" r="22" fill="#E040FB"/>
  <circle cx="322" cy="256" r="22" fill="#00E5FF"/>
</svg>
"""
    path.write_text(svg, encoding="utf-8")


def main() -> None:
    preview = draw_infinity_mark(1024, with_plate=True)
    preview_path = ROOT / ".cursor" / "logo-preview-smooth.png"
    preview_path.parent.mkdir(parents=True, exist_ok=True)
    preview.save(preview_path)
    preview.save(ROOT / ".cursor" / "logo-preview.png")

    glass = draw_glass_infinity(1024)
    glass_path = ROOT / ".cursor" / "logo-preview-glass.png"
    glass.save(glass_path)
    print("wrote", preview_path)
    print("wrote", glass_path)

    tauri = ROOT / "apps" / "desktop" / "src-tauri" / "icons"
    sizes = {
        "32x32.png": 32,
        "128x128.png": 128,
        "128x128@2x.png": 256,
        "icon.png": 1024,
        "Square30x30Logo.png": 30,
        "Square44x44Logo.png": 44,
        "Square71x71Logo.png": 71,
        "Square89x89Logo.png": 89,
        "Square107x107Logo.png": 107,
        "Square142x142Logo.png": 142,
        "Square150x150Logo.png": 150,
        "Square284x284Logo.png": 284,
        "Square310x310Logo.png": 310,
        "StoreLogo.png": 50,
    }
    for name, sz in sizes.items():
        draw_infinity_mark(sz, with_plate=True).save(tauri / name)

    ico_imgs = [draw_infinity_mark(s, with_plate=True) for s in (16, 32, 48, 64, 128, 256)]
    ico_imgs[-1].save(tauri / "icon.ico", format="ICO", sizes=[(i.width, i.height) for i in ico_imgs])

    public = ROOT / "apps" / "desktop" / "public"
    master = draw_infinity_mark(1024, with_plate=True)
    master.save(public / "continuum-mark.png")
    draw_infinity_mark(64, with_plate=True).save(public / "favicon.png")
    write_svg(public / "continuum-mark.svg")
    write_svg(public / "favicon.svg")
    for stale in (
        "fossify-calendar-icon.png",
        "fossify-calendar-icon.svg",
        "fossify-calendar-foreground.svg",
    ):
        stale_path = public / stale
        if stale_path.exists():
            if stale_path.suffix.lower() == ".png":
                master.resize((512, 512), Image.Resampling.LANCZOS).save(stale_path)
            else:
                write_svg(stale_path)

    gfx = ROOT / "apps" / "mobile" / "graphics"
    gfx.mkdir(parents=True, exist_ok=True)
    master.save(gfx / "icon.webp", format="WEBP", quality=90)
    write_svg(gfx / "icon.svg")
    feat = Image.new("RGBA", (1024, 500), INK)
    mark = draw_infinity_mark(360, with_plate=True)
    feat.paste(mark, ((1024 - mark.width) // 2, (500 - mark.height) // 2), mark)
    feat.save(gfx / "featureGraphic.png")

    playstore = draw_infinity_mark(512, with_plate=True)
    playstore.save(ROOT / "apps" / "mobile" / "app" / "src" / "main" / "ic_launcher-playstore.png")

    res = ROOT / "apps" / "mobile" / "app" / "src" / "main" / "res"
    for dens, sz in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        draw_infinity_mark(sz, with_plate=True).save(res / f"mipmap-{dens}" / "ic_launcher.png")

    for dens, sz in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
        folder = res / f"drawable-{dens}"
        folder.mkdir(parents=True, exist_ok=True)
        fg = Image.new("RGBA", (sz, sz), (0, 0, 0, 0))
        mark = draw_infinity_mark(int(sz * 0.72), transparent_bg=True)
        fg.paste(mark, ((sz - mark.width) // 2, (sz - mark.height) // 2), mark)
        fg.save(folder / "ic_launcher_foreground.png")
        Image.new("RGBA", (sz, sz), INK).save(folder / "ic_launcher_background.png")
        mono = fg.split()[-1].point(lambda a: 255 if a > 40 else 0)
        Image.merge("RGBA", (mono, mono, mono, mono)).save(folder / "ic_launcher_monochrome.png")

    print("done — flat mark locked; glass preview at .cursor/logo-preview-glass.png")


if __name__ == "__main__":
    main()
