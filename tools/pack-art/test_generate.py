"""Tests for the pack art generator: PNG encoding, portrait layout and item art."""

import struct
import tempfile
import unittest
import zlib
from pathlib import Path

import generate


def read_png(path):
    """Decodes an 8-bit RGBA PNG written by the generator into (width, height, rows of RGBA tuples)."""
    data = Path(path).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "missing PNG signature"
    pos = 8
    width = height = None
    idat = b""
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        crc = struct.unpack(">I", data[pos + 8 + length:pos + 12 + length])[0]
        assert crc == zlib.crc32(kind + body) & 0xFFFFFFFF, "bad chunk CRC"
        if kind == b"IHDR":
            width, height, depth, color_type = struct.unpack(">IIBB", body[:10])
            assert (depth, color_type) == (8, 6), "expected 8-bit RGBA"
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4
    rows = []
    for y in range(height):
        start = y * (stride + 1)
        assert raw[start] == 0, "expected filter type 0"
        line = raw[start + 1:start + 1 + stride]
        rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(width)])
    return width, height, rows


def rgba(hex_color):
    """Converts a #rrggbb string into an opaque RGBA tuple."""
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


class GenerateTest(unittest.TestCase):
    """Checks that the generator writes valid PNGs with the expected sizes, colours and file set."""

    def test_png_round_trip(self):
        pixels = [[(255, 0, 0, 255), (0, 0, 0, 0)], [(1, 2, 3, 4), (9, 8, 7, 255)]]
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "a.png"
            generate.write_png(path, pixels)
            self.assertEqual((2, 2, pixels), read_png(path))

    def test_portrait_uses_artifact_layout(self):
        canvas = generate.draw_portrait("brina", "neutral")
        self.assertEqual(16, len(canvas))
        self.assertEqual(rgba("#4b6a86"), canvas[0][0])
        self.assertEqual(rgba("#365069"), canvas[12][0])
        self.assertEqual(rgba("#2b5f8a"), canvas[7][6])
        self.assertEqual(rgba("#ffffff"), canvas[7][5])
        self.assertEqual(rgba("#7a3b33"), canvas[11][6])
        self.assertEqual(rgba("#1f3b5a"), canvas[1][1])
        self.assertEqual(rgba("#d7b14a"), canvas[3][1])

    def test_every_mood_differs_from_neutral(self):
        for npc, moods in generate.PORTRAIT_MOODS.items():
            neutral = generate.draw_portrait(npc, "neutral")
            for mood in moods:
                if mood != "neutral":
                    self.assertNotEqual(neutral, generate.draw_portrait(npc, mood), f"{npc}_{mood}")

    def test_item_art_is_sixteen_square_with_known_palette(self):
        for name, (palette, rows) in generate.ITEM_ART.items():
            self.assertEqual(16, len(rows), name)
            for row in rows:
                self.assertEqual(16, len(row), f"{name}: {row!r}")
                for char in row:
                    self.assertTrue(char == "." or char in palette, f"{name}: unknown {char!r}")

    def test_generate_writes_expected_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            generate.generate(Path(tmp))
            textures = Path(tmp)
            portraits = sorted(p.name for p in (textures / "font" / "portrait").iterdir())
            expected = sorted(f"{n}_{m}.png" for n, ms in generate.PORTRAIT_MOODS.items() for m in ms)
            self.assertEqual(expected, portraits)
            self.assertEqual(11, len(expected))
            for name in portraits:
                width, height, _ = read_png(textures / "font" / "portrait" / name)
                self.assertEqual((64, 64), (width, height), name)
            items = sorted(p.stem for p in (textures / "item").iterdir())
            self.assertEqual(sorted([
                "odyssey_book", "sushi", "smoked_fish", "fish_soup", "copper_sheet", "lantern_oil",
                "bait_worm", "drill_bit", "fish_platter", "harbor_rod", "harbor_rod_cast", "pit_pickaxe",
            ]), items)
            for name in items:
                width, height, _ = read_png(textures / "item" / f"{name}.png")
                self.assertEqual((16, 16), (width, height), name)


if __name__ == "__main__":
    unittest.main()
