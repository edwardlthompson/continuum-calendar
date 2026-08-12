"""Regenerate Continuum Android launcher icons from the desktop logo."""
from pathlib import Path

from PIL import Image

root = Path(__file__).resolve().parents[1]
src_path = root / "apps/desktop/src-tauri/icons/icon.png"
src = Image.open(src_path).convert("RGBA")
w, h = src.size
pixels = src.load()
for y in range(h):
    for x in range(w):
        r, g, b, a = pixels[x, y]
        if a == 0:
            continue
        if r < 28 and g < 28 and b < 28:
            pixels[x, y] = (0, 0, 0, 0)

densities = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}
legacy = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

res = root / "apps/mobile/app/src/main/res"


def fit_logo(logo: Image.Image, canvas_size: int, scale: float = 0.72) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    target = max(1, int(canvas_size * scale))
    lw, lh = logo.size
    ratio = min(target / lw, target / lh)
    nw, nh = max(1, int(lw * ratio)), max(1, int(lh * ratio))
    resized = logo.resize((nw, nh), Image.Resampling.LANCZOS)
    ox = (canvas_size - nw) // 2
    oy = (canvas_size - nh) // 2
    canvas.paste(resized, (ox, oy), resized)
    return canvas


def to_mono(logo: Image.Image) -> Image.Image:
    out = Image.new("RGBA", logo.size, (0, 0, 0, 0))
    sp = logo.load()
    op = out.load()
    for y in range(logo.height):
        for x in range(logo.width):
            r, g, b, a = sp[x, y]
            if a < 16:
                continue
            if r + g + b > 40:
                op[x, y] = (255, 255, 255, a)
    return out


mono_src = to_mono(src)

for name, size in densities.items():
    ddir = res / f"drawable-{name}"
    ddir.mkdir(parents=True, exist_ok=True)
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    fg = fit_logo(src, size, 0.72)
    mono = fit_logo(mono_src, size, 0.72)
    bg.save(ddir / "ic_launcher_background.png")
    fg.save(ddir / "ic_launcher_foreground.png")
    mono.save(ddir / "ic_launcher_monochrome.png")
    print("wrote", ddir)

for name, size in legacy.items():
    mdir = res / f"mipmap-{name}"
    mdir.mkdir(parents=True, exist_ok=True)
    full = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    logo = fit_logo(src, size, 0.86)
    full.paste(logo, (0, 0), logo)
    full.save(mdir / "ic_launcher.png")
    print("wrote", mdir / "ic_launcher.png")

print("done")
