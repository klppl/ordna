#!/usr/bin/env python3
"""Wrap a screenshot in a minimal phone device frame using Pillow."""

from pathlib import Path
from PIL import Image, ImageDraw

SNAPSHOTS = Path(__file__).resolve().parents[1] / "snapshots" / "images"

# Phone frame settings
BEZEL = 16          # bezel thickness around the screen
CHIN = 10           # extra bottom bezel
TOP_BAR = 32        # status bar / notch area
CORNER_R = 40       # outer corner radius
FRAME_COLOR = (30, 30, 30)       # dark grey bezel
SCREEN_RADIUS = 28  # inner corner radius


def round_corners(img: Image.Image, radius: int) -> Image.Image:
    """Apply rounded corners with transparency."""
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), img.size], radius=radius, fill=255)
    img = img.convert("RGBA")
    img.putalpha(mask)
    return img


def frame_phone(src: Path, dst: Path) -> None:
    screen = Image.open(src).convert("RGBA")
    sw, sh = screen.size

    # Round the screen corners
    screen = round_corners(screen, SCREEN_RADIUS)

    # Frame dimensions
    fw = sw + 2 * BEZEL
    fh = sh + TOP_BAR + BEZEL + CHIN

    # Create frame (transparent background)
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    draw = ImageDraw.Draw(frame)

    # Draw the outer phone body
    draw.rounded_rectangle(
        [(0, 0), (fw - 1, fh - 1)],
        radius=CORNER_R,
        fill=FRAME_COLOR,
    )

    # Paste screen into the frame
    frame.paste(screen, (BEZEL, TOP_BAR), mask=screen)

    # Draw subtle top speaker/camera dot
    cx = fw // 2
    draw.ellipse(
        [(cx - 4, TOP_BAR // 2 - 4), (cx + 4, TOP_BAR // 2 + 4)],
        fill=(50, 50, 50),
    )

    frame.save(dst, "PNG")
    print(f"Saved: {dst}")


if __name__ == "__main__":
    today = SNAPSHOTS / "io.github.klppl.ordna.screenshots_ScreenshotTest_todayScreen.png"
    out = SNAPSHOTS / "today_screen_framed.png"
    frame_phone(today, out)
