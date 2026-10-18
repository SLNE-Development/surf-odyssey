"""Generates the Odyssey resource pack pixel art: NPC portraits and custom item textures.

Portraits are drawn on a 16x16 canvas and written upscaled to 64x64 with nearest-neighbour
scaling. Item textures are 16x16 and drawn from character grids. The script only uses the Python
standard library; PNGs are encoded with zlib and struct.

Usage: python tools/pack-art/generate.py [output-textures-directory]
"""

import struct
import sys
import zlib
from pathlib import Path

DEFAULT_OUTPUT = (
    Path(__file__).resolve().parents[2]
    / "docker/paper/plugins/CraftEngine/resources/odyssey/resourcepack/assets/odyssey/textures"
)

PORTRAIT_SCALE = 4
TRANSPARENT = (0, 0, 0, 0)

BACKGROUND = "#4b6a86"
SHOULDERS = "#365069"
EYE_WHITE = "#ffffff"
MOUTH = "#7a3b33"

NPCS = {
    "brina": {"skin": "#c99a74", "shade": "#a87a58", "hair": "#5a2f1c", "eye": "#2b5f8a",
              "extra": "hat", "hat": "#1f3b5a", "trim": "#d7b14a"},
    "olek": {"skin": "#d8ab86", "shade": "#b5876a", "hair": "#9a9a9a", "eye": "#3b3b3b",
             "extra": "beard", "hat": "#c2702f", "trim": "#7a4420"},
    "mira": {"skin": "#8d5e3f", "shade": "#6f4630", "hair": "#1c1410", "eye": "#3d7a3a",
             "extra": "glasses", "hat": None, "trim": "#c9a227"},
    "ysa": {"skin": "#b8c7d6", "shade": "#93a6b8", "hair": "#3fb6c9", "eye": "#ff55ff",
            "extra": "hood", "hat": "#2a2350", "trim": "#55ffff"},
    "koch": {"skin": "#e0b08a", "shade": "#bf8d68", "hair": "#4a2c1a", "eye": "#3b2a1a",
             "extra": "chef", "hat": "#f2f2f2", "trim": "#cfcfcf", "mustache": "#4a2c1a"},
}

PORTRAIT_MOODS = {
    "brina": ["neutral", "worried", "happy"],
    "olek": ["neutral", "happy"],
    "mira": ["neutral", "thinking"],
    "ysa": ["neutral", "mystic"],
    "koch": ["neutral", "happy"],
}


def rgba(color):
    """Converts a ``#rrggbb`` string into an opaque RGBA tuple."""
    return tuple(int(color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def png_chunk(kind, body):
    """Encodes one PNG chunk with its length prefix and CRC."""
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)


def write_png(path, pixels):
    """Writes rows of RGBA tuples as an 8-bit RGBA PNG, creating parent directories as needed."""
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    data = (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + png_chunk(b"IDAT", zlib.compress(raw, 9))
        + png_chunk(b"IEND", b"")
    )
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def upscale(pixels, factor):
    """Scales rows of pixels by an integer factor with nearest-neighbour sampling."""
    return [[pixel for pixel in row for _ in range(factor)] for row in pixels for _ in range(factor)]


class Canvas:
    """A fixed-size RGBA pixel grid that is filled with axis-aligned rectangles."""

    def __init__(self, size=16):
        """Creates a fully transparent square canvas of the given edge length."""
        self.size = size
        self.pixels = [[TRANSPARENT] * size for _ in range(size)]

    def px(self, x, y, w, h, color):
        """Fills the rectangle at ``(x, y)`` with width ``w`` and height ``h``, clipped to the canvas."""
        value = rgba(color)
        for row in range(max(0, y), min(self.size, y + h)):
            for column in range(max(0, x), min(self.size, x + w)):
                self.pixels[row][column] = value


def draw_face(canvas, npc):
    """Draws the background, face, hair, eyes, nose and mouth shared by every portrait."""
    px = canvas.px
    px(0, 0, 16, 16, BACKGROUND)
    px(0, 12, 16, 4, SHOULDERS)
    px(3, 3, 10, 11, npc["skin"])
    px(3, 11, 10, 3, npc["shade"])
    px(2, 1, 12, 4, npc["hair"])
    px(2, 5, 2, 5, npc["hair"])
    px(12, 5, 2, 5, npc["hair"])
    px(5, 7, 2, 2, EYE_WHITE)
    px(9, 7, 2, 2, EYE_WHITE)
    px(6, 7, 1, 2, npc["eye"])
    px(10, 7, 1, 2, npc["eye"])
    px(7, 9, 2, 1, npc["shade"])
    px(6, 11, 4, 1, MOUTH)


def draw_extra(canvas, npc):
    """Draws the NPC's headwear or accessory: hat, beard with cap, glasses, hood or chef's hat."""
    px = canvas.px
    extra, hat, trim = npc["extra"], npc["hat"], npc["trim"]
    if extra == "hat":
        px(1, 1, 14, 2, hat)
        px(3, 0, 10, 1, hat)
        px(1, 3, 14, 1, trim)
    elif extra == "beard":
        px(4, 10, 8, 4, npc["hair"])
        px(6, 11, 4, 1, MOUTH)
        px(2, 0, 12, 3, hat)
        px(2, 3, 12, 1, trim)
    elif extra == "glasses":
        px(4, 6, 4, 1, trim)
        px(8, 6, 4, 1, trim)
        px(4, 6, 1, 3, trim)
        px(11, 6, 1, 3, trim)
    elif extra == "hood":
        px(1, 0, 14, 3, hat)
        px(1, 3, 2, 11, hat)
        px(13, 3, 2, 11, hat)
        px(7, 1, 2, 1, trim)
    elif extra == "chef":
        px(3, 0, 10, 1, hat)
        px(2, 1, 12, 2, hat)
        px(2, 3, 12, 1, trim)
        px(6, 10, 4, 1, npc["mustache"])


def mouth_base(npc):
    """Returns the colour under the mouth row, which is the beard for bearded NPCs and the shade otherwise."""
    return npc["hair"] if npc["extra"] == "beard" else npc["shade"]


def draw_mood(canvas, npc, mood):
    """Overlays the eyebrows, mouth and eyes that express the given mood; ``neutral`` adds nothing."""
    px = canvas.px
    brow = npc["hair"]
    if mood == "worried":
        px(5, 6, 1, 1, brow)
        px(6, 5, 1, 1, brow)
        px(9, 5, 1, 1, brow)
        px(10, 6, 1, 1, brow)
        px(6, 11, 4, 1, mouth_base(npc))
        px(7, 11, 2, 1, MOUTH)
        px(6, 12, 1, 1, MOUTH)
        px(9, 12, 1, 1, MOUTH)
    elif mood == "happy":
        px(5, 11, 6, 1, MOUTH)
        px(6, 11, 4, 1, EYE_WHITE)
        px(6, 12, 4, 1, MOUTH)
        px(4, 10, 1, 1, MOUTH)
        px(11, 10, 1, 1, MOUTH)
    elif mood == "thinking":
        if npc["extra"] != "glasses":
            px(5, 6, 2, 1, brow)
        px(9, 5, 2, 1, brow)
        px(5, 7, 2, 2, EYE_WHITE)
        px(9, 7, 2, 2, EYE_WHITE)
        px(6, 7, 1, 1, npc["eye"])
        px(10, 7, 1, 1, npc["eye"])
        px(6, 11, 4, 1, mouth_base(npc))
        px(7, 11, 3, 1, MOUTH)
    elif mood == "mystic":
        px(5, 7, 2, 2, npc["eye"])
        px(9, 7, 2, 2, npc["eye"])
        px(6, 7, 1, 1, EYE_WHITE)
        px(10, 7, 1, 1, EYE_WHITE)
        px(4, 7, 1, 1, npc["trim"])
        px(11, 7, 1, 1, npc["trim"])
        px(6, 11, 4, 1, mouth_base(npc))
        px(7, 11, 2, 1, MOUTH)


def draw_portrait(name, mood):
    """Draws the 16x16 portrait of the named NPC in the given mood and returns its pixel rows."""
    npc = NPCS[name]
    canvas = Canvas()
    draw_face(canvas, npc)
    draw_extra(canvas, npc)
    draw_mood(canvas, npc, mood)
    return canvas.pixels


OUTLINE = "#2a2a30"

ITEM_ART = {
    "odyssey_book": (
        {"K": "#1f2a3a", "B": "#2f5a87", "b": "#24476b", "G": "#d7b14a", "g": "#a8862f",
         "P": "#f0e6c8", "p": "#cdbf98"},
        [
            "................",
            "..KKKKKKKKKKKK..",
            "..KbBBBBBBBBBBK.",
            "..KbBGGGGGGGGBK.",
            "..KbBGBBBBBBGBK.",
            "..KbBGBBGGBBGBK.",
            "..KbBGBGggGBGBK.",
            "..KbBGBGggGBGBK.",
            "..KbBGBBGGBBGBK.",
            "..KbBGBBBBBBGBK.",
            "..KbBGGGGGGGGBK.",
            "..KbBBBBBBBBBBK.",
            "..KbKKKKKKKKKKK.",
            "..KbPPPPPPPPPPK.",
            "..KKpppppppppK..",
            "................",
        ],
    ),
    "sushi": (
        {"K": "#3a2a24", "W": "#f4f1e8", "w": "#d6d0c0", "S": "#f08a5a", "s": "#ffc1a0",
         "d": "#c8603a", "N": "#1d2b22"},
        [
            "................",
            "................",
            "................",
            "................",
            "....KKKKKKKKK...",
            "...KSSsSSNsSSK..",
            "..KSsSSsNNSsSdK.",
            "..KdSSsSNNsSddK.",
            "..KKddddNNdddKK.",
            "..KWWWWWNNWWWWK.",
            "..KWWwWWNNWwWWK.",
            "..KwWWWWNNWWWwK.",
            "...KwwwwNNwwwK..",
            "....KKKKKKKKK...",
            "................",
            "................",
        ],
    ),
    "smoked_fish": (
        {"K": "#2e1d12", "F": "#b0642c", "f": "#8a4a1e", "h": "#d98c45", "e": "#f2e6c8",
         "T": "#7a3f18"},
        [
            "................",
            "................",
            "................",
            "................",
            ".....KKKKKK.....",
            "...KKhhhhhhKK.KK",
            "..KhFFFFFFFFhKTK",
            ".KeKFfFFfFFfFKTK",
            ".KFFFFfFFfFFFFTK",
            "..KfFFfFFfFFfKTK",
            "...KKffffffKK.KK",
            ".....KKKKKK.....",
            "................",
            "................",
            "................",
            "................",
        ],
    ),
    "fish_soup": (
        {"K": "#3b2414", "B": "#8a5a32", "b": "#6b4325", "h": "#a9743f", "S": "#e39a4b",
         "s": "#f2c17a", "F": "#f4e3c8", "G": "#4f8a3a", "v": "#dfe8ee"},
        [
            "................",
            "................",
            ".....v...v......",
            "......v...v.....",
            ".....v...v......",
            "................",
            "..KKKKKKKKKKKK..",
            ".KSsSSFSSGSsSSK.",
            ".KSSGSSsSSFSSSK.",
            ".KhBBBBBBBBBBbK.",
            "..KhBBBBBBBBbK..",
            "...KbBBBBBBbK...",
            "....KKbbbbKK....",
            ".....KKKKKK.....",
            "................",
            "................",
        ],
    ),
    "copper_sheet": (
        {"K": "#5a2c1a", "C": "#c8693f", "c": "#a4522f", "h": "#e8946a", "r": "#7a3a22"},
        [
            "................",
            "................",
            "................",
            "................",
            "......KKKKKKKKK.",
            ".....KhhhhhhhhK.",
            "....KhrCCCCrCK..",
            "...KhCCCCCCcK...",
            "..KhCCCCCCcK....",
            ".KhrCCCCrcK.....",
            ".KccccccccK.....",
            ".KKKKKKKKKK.....",
            "................",
            "................",
            "................",
            "................",
        ],
    ),
    "lantern_oil": (
        {"K": "#2b2b33", "g": "#bfe3ef", "G": "#ffffff", "O": "#e6a521", "o": "#b97a12",
         "c": "#9c6b3c", "L": "#f0e2bd", "l": "#8a5a32"},
        [
            "................",
            "......KKKK......",
            "......KccK......",
            "......KccK......",
            ".....KKKKKK.....",
            "......KgGK......",
            ".....KggGgK.....",
            "....KKggggKK....",
            "...KGgggggggK...",
            "...KGOOOOOOoK...",
            "...KGLLLLLLoK...",
            "...KOLlllLLoK...",
            "...KOLLLLLLoK...",
            "...KOOOOOOooK...",
            "....KKKKKKKK....",
            "................",
        ],
    ),
    "bait_worm": (
        {"K": "#4a1f24", "W": "#e07a8a", "w": "#b85566", "h": "#f5a8b4", "d": "#5a3d24"},
        [
            "................",
            "................",
            "..........KK....",
            ".........KhWK...",
            "........KhWwK...",
            ".......KhWwK....",
            "......KhWwK.....",
            ".....KhWwK......",
            "....KhWwK.......",
            "....KWwK........",
            "....KWwWKK......",
            ".....KwWWhKK....",
            "......KKwWWhK...",
            ".d......KKwWWK..",
            "..d.......KKK...",
            "................",
        ],
    ),
    "drill_bit": (
        {"K": OUTLINE, "I": "#b9c1c9", "i": "#7d8690", "h": "#e6ecf0", "C": "#c8693f",
         "c": "#8f4527"},
        [
            "................",
            ".....KKKKKK.....",
            ".....KCCCcK.....",
            ".....KCcCcK.....",
            "....KKKKKKKK....",
            "....KhIIIiiK....",
            "....KIhiIIiK....",
            ".....KIhiiK.....",
            ".....KhIIiK.....",
            ".....KIhiiK.....",
            "......KhiK......",
            "......KIiK......",
            "......KhiK......",
            ".......KK.......",
            "................",
            "................",
        ],
    ),
    "fish_platter": (
        {"K": "#3a3a44", "P": "#eef2f4", "p": "#c9d3da", "R": "#4b6a86", "S": "#f08a5a",
         "W": "#f4f1e8", "N": "#1d2b22", "F": "#b0642c", "f": "#8a4a1e", "G": "#4f8a3a",
         "L": "#f1d54a"},
        [
            "................",
            "................",
            "................",
            "................",
            "................",
            "..KKKKKKKKKKKK..",
            ".KRRRRRRRRRRRRK.",
            "KRPSSNSSPfFFPfRK",
            "KRPWWNWWPFfFfFRK",
            "KRPWWNWWPPFFFLRK",
            "KRpPPPPPPPGPPpRK",
            ".KRRRRRRRRRRRRK.",
            "..KKKKKKKKKKKK..",
            "................",
            "................",
            "................",
        ],
    ),
    "harbor_rod": (
        {"W": "#8a5a32", "w": "#5e3b1e", "G": "#d7b14a", "B": "#2f5a87", "b": "#1f3b5a",
         "s": "#e8e8e8", "H": "#9a9a9a"},
        [
            "................",
            ".............Ws.",
            "............Wws.",
            "...........Ww.s.",
            "..........Ww..s.",
            ".........Ww...s.",
            "........GG....s.",
            ".......Ww.....s.",
            "......Ww......s.",
            ".....Ww.......s.",
            "....BB........s.",
            "...Bb.........s.",
            "..Bb........H.H.",
            ".Bb..........H..",
            "Bb..............",
            "................",
        ],
    ),
    "harbor_rod_cast": (
        {"W": "#8a5a32", "w": "#5e3b1e", "G": "#d7b14a", "B": "#2f5a87", "b": "#1f3b5a"},
        [
            "................",
            ".............W..",
            "............Ww..",
            "...........Ww...",
            "..........Ww....",
            ".........Ww.....",
            "........GG......",
            ".......Ww.......",
            "......Ww........",
            ".....Ww.........",
            "....BB..........",
            "...Bb...........",
            "..Bb............",
            ".Bb.............",
            "Bb..............",
            "................",
        ],
    ),
    "pit_pickaxe": (
        {"K": "#1f1f24", "I": "#9aa3ad", "i": "#6b737d", "h": "#d5dbe0", "W": "#7a4f2a",
         "w": "#4f3219", "C": "#c8693f"},
        [
            "................",
            "....KKKKKK......",
            "...KhhIIIIKK....",
            "....KKKiIIIIK...",
            "........KCiIIK..",
            ".......KWCKiIIK.",
            "......KWwK.KiIK.",
            ".....KWwK...KiIK",
            "....KWwK....KiIK",
            "...KWwK......KiK",
            "..KWwK.......KiK",
            ".KWwK.........K.",
            "KWwK............",
            "KwK.............",
            ".K..............",
            "................",
        ],
    ),
}


def render_item(palette, rows):
    """Turns a character grid into RGBA pixel rows; ``.`` is transparent, other characters map through the palette."""
    return [[TRANSPARENT if char == "." else rgba(palette[char]) for char in row] for row in rows]


def generate(output):
    """Writes every portrait to ``font/portrait`` and every item texture to ``item`` below the output directory."""
    output = Path(output)
    for name, moods in PORTRAIT_MOODS.items():
        for mood in moods:
            pixels = upscale(draw_portrait(name, mood), PORTRAIT_SCALE)
            write_png(output / "font" / "portrait" / f"{name}_{mood}.png", pixels)
    for name, (palette, rows) in ITEM_ART.items():
        write_png(output / "item" / f"{name}.png", render_item(palette, rows))


def main():
    """Generates the art into the directory given on the command line or the pack's texture directory."""
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUTPUT
    generate(output)
    print(f"Wrote pack art to {output}")


if __name__ == "__main__":
    main()
