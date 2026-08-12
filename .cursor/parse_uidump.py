from pathlib import Path
import re
import sys

xml = Path(".cursor/uidump.xml").read_text(encoding="utf-8", errors="replace")
# Normalize attribute order: extract nodes with text + bounds
nodes = []
for m in re.finditer(r"<node\b([^>]*)/>", xml):
    attrs = m.group(1)
    text_m = re.search(r'\btext="([^"]*)"', attrs)
    bounds_m = re.search(r'\bbounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attrs)
    desc_m = re.search(r'\bcontent-desc="([^"]*)"', attrs)
    if not bounds_m:
        continue
    text = text_m.group(1) if text_m else ""
    desc = desc_m.group(1) if desc_m else ""
    x1, y1, x2, y2 = map(int, bounds_m.groups())
    if text or desc:
        nodes.append((text, desc, x1, y1, x2, y2))

print("ALL TEXT NODES:")
for text, desc, x1, y1, x2, y2 in nodes:
    label = text or f"[{desc}]"
    if label.strip():
        print(f"  ({x1},{y1})-({x2},{y2}) {label}")

print("\nCLICK CANDIDATES (error/detail):")
for text, desc, x1, y1, x2, y2 in nodes:
    blob = f"{text} {desc}".lower()
    if "error detail" in blob or blob.strip() == "error details" or "see error" in blob:
        cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
        print(f"TAP {cx} {cy} :: {text or desc}")
