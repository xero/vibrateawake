#!/usr/bin/env bash
#
# Generate Play Store phone screenshots + the feature graphic for Vibrate Awake.
#
# Prereqs: an Android emulator running with the app installed, `adb` in the SDK,
# and ImageMagick (`magick`) for the feature graphic.
#
# Usage:
#   store/gen-screenshots.sh [device-serial]
#
# It sizes the emulator to 1080x2160 (2:1, Play's max phone aspect) at 360 dpi so
# the whole four-knob form fits in one frame, sets a clean 24h 13:37 status bar with
# wifi off, captures each screen state, restores the device, then rebuilds the
# feature graphic from the light/dark main shots.
set -euo pipefail

ADB="${ADB:-$HOME/.local/share/android/sdk/platform-tools/adb}"
PKG="style.xero.vibrateawake"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/screenshots"
FEAT="$HERE/feature-graphic.png"

DEV="${1:-$("$ADB" devices | awk '/\temulator|\tdevice$/{print $1; exit}')}"
[ -z "${DEV:-}" ] && { echo "No device found. Start an emulator first." >&2; exit 1; }

adb_() { "$ADB" -s "$DEV" "$@"; }
demo() { adb_ shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null 2>&1; }

# Clean status bar: 13:37, full battery, no wifi/mobile/sim icons. Re-asserted before
# every capture because SystemUI re-adds connectivity icons after settings changes settle.
statusbar() {
  demo enter
  demo clock -e hhmm 1337
  demo battery -e level 100 -e plugged false
  demo network -e airplane hide -e wifi hide -e mobile hide
  demo notifications -e visible false
}

# --- device setup ----------------------------------------------------------
setup() {
  adb_ shell settings put system time_12_24 24 >/dev/null
  adb_ shell svc power stayon true >/dev/null 2>&1 || true
  adb_ shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1 || true
  # Airplane mode turns off wifi + cellular for real (the app needs neither) and stops
  # the emulator's dual-SIM icons from re-appearing over time; the icon is hidden below.
  adb_ shell cmd connectivity airplane-mode enable >/dev/null 2>&1 || true
  adb_ shell settings put global sysui_demo_allowed 1 >/dev/null
  demo enter
  statusbar
  adb_ shell wm size 1080x2160 >/dev/null
  adb_ shell wm density 360 >/dev/null
  adb_ shell dumpsys deviceidle whitelist +$PKG >/dev/null 2>&1 || true
  adb_ shell pm clear "$PKG" >/dev/null
  adb_ shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null
}

restore() {
  demo exit
  adb_ shell wm size reset >/dev/null 2>&1 || true
  adb_ shell wm density reset >/dev/null 2>&1 || true
  adb_ shell cmd uimode night auto >/dev/null 2>&1 || true
  adb_ shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true
  adb_ shell svc wifi enable >/dev/null 2>&1 || true
}
trap restore EXIT

# --- ui helpers ------------------------------------------------------------
dump() { adb_ shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb_ shell cat /sdcard/ui.xml 2>/dev/null | tr '<' '\n<'; }

# center of the first node matching a grep pattern -> "x y"
center_of() {
  dump | grep "$1" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 \
    | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\1 \2 \3 \4/' \
    | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'
}
tap_node() { local xy; xy="$(center_of "$1")"; [ -n "$xy" ] && adb_ shell input tap $xy || echo "  (tap target not found: $1)" >&2; }

# tap the Nth SeekBar at a horizontal fraction (0..1) to set its value
tap_slider() {
  local n="$1" frac="$2" b
  b="$(dump | grep 'class="android.widget.SeekBar"' | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | sed -n "${n}p" \
      | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\1 \2 \3 \4/')"
  [ -z "$b" ] && { echo "  (slider $n not found)" >&2; return; }
  set -- $b
  adb_ shell input tap "$(awk -v a=$1 -v c=$3 -v f=$frac 'BEGIN{print int(a+(c-a)*f)}')" "$(awk -v a=$2 -v d=$4 'BEGIN{print int((a+d)/2)}')"
}

fresh() { adb_ shell am force-stop "$PKG"; adb_ shell am start -n "$PKG/.MainActivity" >/dev/null; sleep 2.5; }
cap()   { statusbar; sleep 1.8; adb_ exec-out screencap -p > "$OUT/$1"; echo "  $1"; }

# Snooze every notification that isn't ours (e.g. the emulator's "Serial console
# enabled" and the "Set a screen lock" nudge) so shot 05's shade shows only our alert.
# A for-loop (not while-read) avoids adb shell eating the loop's stdin.
clear_other_notifs() {
  local k
  for k in $(adb_ shell cmd notification list 2>/dev/null | tr -d '\r' | grep -v "$PKG" || true); do
    adb_ shell "cmd notification snooze --for 3600000 '$k'" >/dev/null 2>&1 </dev/null || true
  done
}

# --- capture ---------------------------------------------------------------
echo "Device $DEV -> $OUT"
setup
echo "Capturing:"

adb_ shell cmd uimode night yes >/dev/null; fresh
cap 01-main-dark.png

# customized options: mid interval, Continuous Wave, Maximum, longer duration
tap_slider 1 0.5; sleep 0.4
tap_node 'text="Continuous Wave"'; sleep 0.4
tap_node 'text="Maximum"'; sleep 0.4
tap_slider 2 0.72; sleep 0.4
cap 02-options.png

fresh; tap_node 'text="START"'; sleep 2
cap 03-running.png
tap_node 'text="STOP"'; sleep 1

fresh; tap_node 'content-desc="Privacy policy"'; sleep 1
cap 04-privacy.png

fresh; tap_node 'text="START"'; sleep 2
clear_other_notifs; sleep 1
adb_ shell cmd statusbar expand-notifications >/dev/null 2>&1; sleep 2
cap 05-notification.png
adb_ shell cmd statusbar collapse >/dev/null 2>&1; sleep 1
tap_node 'text="STOP"' >/dev/null 2>&1 || true; sleep 1

adb_ shell cmd uimode night no >/dev/null; fresh
cap 06-main-light.png

# --- feature graphic (1024x500, light on white | dark on black) ------------
# PNG32:/PNG24: force RGBA/RGB so the all-gray frame canvas can't collapse the
# colored screenshots to grayscale (ImageMagick auto-reduces gray-only content).
if command -v magick >/dev/null 2>&1; then
  echo "Feature graphic:"
  T="$(mktemp -d)"
  H=440; W=$(( 1080 * H / 2160 )); R=20; PAD=11
  BW=$(( W + PAD*2 )); BH=$(( H + PAD*2 ))
  LX=$(( 256 - BW/2 )); RX=$(( 768 - BW/2 )); PY=$(( (500 - BH)/2 ))
  magick -size ${W}x${H} xc:none -fill white -draw "roundrectangle 0,0,$((W-1)),$((H-1)),$R,$R" "$T/mask.png"
  magick -size ${BW}x${BH} xc:none -fill "#1c1c1c" -stroke "#555555" -strokewidth 2 \
    -draw "roundrectangle 1,1,$((BW-2)),$((BH-2)),$((R+12)),$((R+12))" PNG32:"$T/bezel.png"
  for t in "light:06-main-light.png" "dark:01-main-dark.png"; do
    name="${t%%:*}"; src="${t##*:}"
    magick "$OUT/$src" -resize x${H} "$T/$name-s.png"
    magick "$T/$name-s.png" "$T/mask.png" -compose CopyOpacity -composite PNG32:"$T/$name-r.png"
    magick "$T/bezel.png" "$T/$name-r.png" -gravity center -compose over -composite PNG32:"$T/$name-phone.png"
  done
  magick -size 1024x500 xc:white \( -size 512x500 xc:black \) -gravity East -compose over -composite PNG24:"$T/base.png"
  magick "$T/base.png" "$T/light-phone.png" -geometry +${LX}+${PY} -compose over -composite \
    "$T/dark-phone.png" -geometry +${RX}+${PY} -compose over -composite PNG24:"$FEAT"
  rm -rf "$T"
  echo "  feature-graphic.png ($(magick identify -format '%wx%h %[type]' "$FEAT"))"
else
  echo "magick not found; skipped feature graphic." >&2
fi

echo "Done."
