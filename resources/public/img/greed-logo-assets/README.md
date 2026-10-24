# Greed logo assets

Brand green: #0F6B4F. All files have a transparent background. All colour variants of one logo share the same artwork and the same canvas, so you can swap green, black and white without the layout shifting.

## Canvas sizes

| Logo | Aspect | SVG viewBox | PNG sizes (px) |
|---|---|---|---|
| Wordmark | 3:1 | 0 0 1200 400 | 300x100, 600x200, 1200x400, 2400x800 |
| Wordmark + tagline | 30:13 | 0 0 1200 520 | 300x130, 600x260, 1200x520, 2400x1040 |
| (G) mark | 1:1 | 0 0 512 512 | 64, 128, 256, 512, 1024 (square) |

PNGs are rendered from the SVGs, so every size is sharp. Pick the PNG at 2x the size you display it (for example, show the 600x200 file at 300x100 CSS px).

## Variants

- `green`: primary, light backgrounds
- `white`: dark or green backgrounds
- `black`: one-colour use
- `tagline-green-on-dark`: low contrast on dark; prefer `tagline-white`

Do not use the tagline version below about 160 px wide. Use the plain wordmark or the (G) mark instead.

## Folders

- `svg/`: vector files on the standard canvases above, with built-in padding
- `svg-tight/`: the same logos cropped to the artwork with no padding. Use these inside the app UI (navbar, buttons) so the logo lines up with text; spacing comes from your CSS. Tight wordmark aspect is about 3.54:1, tight mark about 1.2:1
- `png/<logo-variant>/`: fixed-size PNGs, named `<logo-variant>-<width>x<height>.png`
- `icons/`: favicon and app icons, white G on green
- `icons-black/`: the same set, white G on black. Use one icon folder, not both.

## Recommended display sizes

- Site header: wordmark at 32 to 40 px tall (96 to 120 px wide)
- Login or splash screen: wordmark or tagline version at 200 to 320 px wide
- Avatar or small UI badge: (G) mark at 24 to 64 px

## HTML head

```html
<link rel="icon" href="/favicon.ico" sizes="any">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="manifest" href="/manifest.webmanifest">
<meta name="theme-color" content="#0F6B4F">
```

## manifest.webmanifest icons

```json
"icons": [
  { "src": "/icon-192.png", "sizes": "192x192", "type": "image/png" },
  { "src": "/icon-512.png", "sizes": "512x512", "type": "image/png" },
  { "src": "/icon-512-maskable.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
]
```
