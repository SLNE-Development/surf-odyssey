# Pack art

`generate.py` draws the pixel art of the `odyssey` CraftEngine pack and writes it below
`docker/paper/plugins/CraftEngine/resources/odyssey/resourcepack/assets/odyssey/textures/`:

- `font/portrait/<npc>_<mood>.png`: NPC portraits, drawn on a 16x16 grid and upscaled to 64x64
  with nearest-neighbour scaling. Moods change eyebrows, eyes and mouth.
- `item/<item>.png`: 16x16 item textures. `harbor_rod_cast.png` is the cast state of the rod.

The script needs Python 3 and only the standard library. Run it from the repository root and commit
the regenerated PNGs together with the script change:

```
python tools/pack-art/generate.py
```

An optional argument writes into another textures directory instead.

Tests:

```
cd tools/pack-art
python -m unittest test_generate
```

The CraftEngine definitions that use these textures live in
`docker/paper/plugins/CraftEngine/resources/odyssey/configuration/images/portraits.yml` and
`docker/paper/plugins/CraftEngine/resources/odyssey/configuration/items/odyssey_items.yml`.
