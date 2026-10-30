# greed logo kit

Start with `BRAND-GUIDE.md`. `kit-overview.png` shows the logo in use.

## Folders

- `svg/`: master files, padded with clear space
- `svg-tight/`: cropped to the artwork, for app UI
- `png/<version-colour>/`: fixed-size PNGs, named `<name>-<width>x<height>.png`
- `icons/`: favicon, app and PWA icons, manifest, `<head>` snippet
- `social/`: profile pictures (400 and 1024 px)
- `merch/`: t-shirt print files (SVG and 4000 px PNG)

## Dropping it into the app

`svg-tight/greed-wordmark-green-tight.svg` and `greed-wordmark-white-tight.svg` now contain the new horizontal logo, so your existing `:src` paths keep working. The shape changed, so update `:width`/`:height` to `648`/`192` and use the heights below. The symbol is taller than the old text, so the heights go up slightly to keep "GREED" readable.

| Old text size | New class |
|---|---|
| `text-lg` | `h-6` |
| `text-2xl` | `h-8` |
| `text-2xl sm:text-3xl` | `h-8 sm:h-9` |
| `text-3xl` | `h-9` |
