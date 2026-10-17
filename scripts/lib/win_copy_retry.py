"""Retry shutil.copy2 on Windows file-lock errors (AV / mapped sections)."""
from __future__ import annotations

import shutil
import time
from pathlib import Path


def copy2_retry(src: Path, dest: Path, attempts: int = 8) -> None:
    last: OSError | None = None
    for i in range(attempts):
        try:
            shutil.copy2(src, dest)
            return
        except OSError as exc:
            last = exc
            # 32 = sharing violation, 1224 = user-mapped section open
            if getattr(exc, "winerror", None) not in (32, 1224) and exc.errno not in (13, 11):
                raise
            time.sleep(0.05 * (i + 1))
    assert last is not None
    raise last
