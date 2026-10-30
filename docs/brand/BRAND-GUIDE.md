# greed — Logo kit and usage guide

## 1. The logo

**Idea:** a soft square cut once at the golden ratio. The cut marks the level where enough is: greed that knows when to stop.

**Versions**

| Version | Use it for | Files |
|---|---|---|
| Horizontal (primary) | Website header, documents, email | `svg/greed-horizontal-*.svg` |
| Stacked | T-shirts, posters, square layouts | `svg/greed-stacked-*.svg` |
| Symbol | Avatars, app icon, small spaces | `svg/greed-symbol-*.svg` |
| Symbol, small cut | 16 to 48 px only (favicon) | `svg/greed-symbol-small-*.svg` |
| Wordmark only | Where the symbol already appears nearby, wide merch prints | `svg/greed-wordmark-*.svg` |

Every version comes in `green`, `black` and `white`. The `svg-tight/` folder has the same files cropped to the artwork with no padding, for use inside the app UI.

The symbol and the wordmark are drawn from geometry, not typed in a font and not taken from a template. Never retype "GREED" in a font to stand in for the wordmark.

## 2. Clear space

Keep a clear zone around the logo equal to the height of the symbol's top slab (the part above the cut). The padded SVGs in `svg/` already include half of that; add the rest with spacing in your layout.

## 3. Minimum size

| Version | Screen | Print |
|---|---|---|
| Horizontal | 96 px wide | 25 mm wide |
| Stacked | 64 px wide | 18 mm wide |
| Symbol | 24 px (below that, use the small-cut symbol) | 6 mm |

## 4. Colour

| Name | HEX | RGB | CMYK (approx.) |
|---|---|---|---|
| greed green | #0F6B4F | 15, 107, 79 | 86, 0, 26, 58 |
| White | #FFFFFF | 255, 255, 255 | 0, 0, 0, 0 |
| Ink | #18181B | 24, 24, 27 | 0, 0, 0, 90 |

The CMYK values are a starting point. For printed merch, ask the printer for a proof against a physical swatch, and match to the closest Pantone they recommend.

**Approved pairs:** green on white · white on green · white on black or ink · black on white.
Avoid green on black or dark grey; the contrast is too low. Use white there.

**UI colour scale (Tailwind `brand`):** 50 #EDFBF4 · 100 #DBF4E8 · 200 #BCE6D3 · 300 #92D1B6 · 400 #61B191 · 500 #378E6F · 600 #0F6B4F · 700 #00553D · 800 #00422E · 900 #003222 · 950 #001C12

## 5. Social and app icons

- **Profile picture:** `social/greed-avatar-green-bg-1024.png` (white symbol on green). The mark sits inside the centre 60 %, so circle crops never clip it.
- **App icon:** `icons/app-icon-1024.png` is a full green square with the white cut. iOS and Android mask it to a rounded square, which turns it into the logo itself.
- **Web:** `icons/` holds `favicon.ico`, `favicon.svg`, the PWA icons, `site.webmanifest` and `head-snippet.html`.

## 6. T-shirts and merch

Print files are in `merch/`: the stacked logo and the wordmark, as SVG (send this to the printer) and as 4000 px transparent PNG. Use white on dark or green fabric, green on white or light fabric. Print one colour only.

## 7. Don'ts

Don't stretch, squash or rotate the logo. Don't move the cut or change its thickness. Don't recolour it outside the palette. Don't add shadows, outlines, gradients or effects. Don't rearrange the symbol and wordmark or change their sizes relative to each other. Don't place it on busy photos without a solid background behind it. Don't use the small-cut symbol above 48 px.

## 8. Trademark

The logo was drawn from scratch with no template, stock graphic or licensed font, so the artwork is yours. Before you register it, have a trademark professional run a clearance search on the name and the mark.
