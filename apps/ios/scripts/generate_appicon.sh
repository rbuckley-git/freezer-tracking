#!/bin/zsh
set -euo pipefail
ASSET_DIR='apps/ios/FreezerTracker/Resources/Assets.xcassets/AppIcon.appiconset'
SVG='apps/ios/FreezerTracker/Resources/AppIcon.svg'
mkdir -p "$ASSET_DIR"
cat > "$ASSET_DIR/Contents.json" <<'JSON'
{
  "images" : [
    { "idiom" : "iphone", "size" : "20x20", "scale" : "2x", "filename" : "icon-20@2x.png" },
    { "idiom" : "iphone", "size" : "20x20", "scale" : "3x", "filename" : "icon-20@3x.png" },
    { "idiom" : "iphone", "size" : "29x29", "scale" : "2x", "filename" : "icon-29@2x.png" },
    { "idiom" : "iphone", "size" : "29x29", "scale" : "3x", "filename" : "icon-29@3x.png" },
    { "idiom" : "iphone", "size" : "40x40", "scale" : "2x", "filename" : "icon-40@2x.png" },
    { "idiom" : "iphone", "size" : "40x40", "scale" : "3x", "filename" : "icon-40@3x.png" },
    { "idiom" : "iphone", "size" : "60x60", "scale" : "2x", "filename" : "icon-60@2x.png" },
    { "idiom" : "iphone", "size" : "60x60", "scale" : "3x", "filename" : "icon-60@3x.png" },

    { "idiom" : "ipad", "size" : "20x20", "scale" : "1x", "filename" : "icon-20@1x.png" },
    { "idiom" : "ipad", "size" : "20x20", "scale" : "2x", "filename" : "icon-20@2x.png" },
    { "idiom" : "ipad", "size" : "29x29", "scale" : "1x", "filename" : "icon-29@1x.png" },
    { "idiom" : "ipad", "size" : "29x29", "scale" : "2x", "filename" : "icon-29@2x.png" },
    { "idiom" : "ipad", "size" : "40x40", "scale" : "1x", "filename" : "icon-40@1x.png" },
    { "idiom" : "ipad", "size" : "40x40", "scale" : "2x", "filename" : "icon-40@2x.png" },
    { "idiom" : "ipad", "size" : "76x76", "scale" : "1x", "filename" : "icon-76@1x.png" },
    { "idiom" : "ipad", "size" : "76x76", "scale" : "2x", "filename" : "icon-76@2x.png" },
    { "idiom" : "ipad", "size" : "83.5x83.5", "scale" : "2x", "filename" : "icon-83.5@2x.png" },

    { "idiom" : "ios-marketing", "size" : "1024x1024", "scale" : "1x", "filename" : "icon-1024.png" }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
JSON

cat > /tmp/icon-sizes.txt <<'EOF'
icon-20@1x.png 20 20
icon-20@2x.png 40 40
icon-20@3x.png 60 60
icon-29@1x.png 29 29
icon-29@2x.png 58 58
icon-29@3x.png 87 87
icon-40@1x.png 40 40
icon-40@2x.png 80 80
icon-40@3x.png 120 120
icon-60@2x.png 120 120
icon-60@3x.png 180 180
icon-76@1x.png 76 76
icon-76@2x.png 152 152
icon-83.5@2x.png 167 167
icon-1024.png 1024 1024
EOF

while read -r file w h; do
  rsvg-convert -w "$w" -h "$h" "$SVG" -o "$ASSET_DIR/$file"
done < /tmp/icon-sizes.txt
