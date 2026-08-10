#!/bin/bash
# Cross-version share code test. Exports a code on every version, then reads every
# code on every version. Uses the integrated server inside runClient, so no EULA is needed.
cd "/Users/bjpelicano/Desktop/NEW MODS/Notch Currency - Test 2" || exit 1
# Somewhere without spaces on purpose: JAVA_TOOL_OPTIONS splits -D values on whitespace, and this
# repo lives under a path that has some.
SCRATCH="${NOTCH_SHARETEST_DIR:-/tmp/notchcurrency-sharetest}"
SHARE="$SCRATCH/sharecodes"
LOGS="$SCRATCH/sharelogs"
VERSIONS="1.20.1 1.21.1 1.21.11 26.1.2 26.2"

run() {  # run <version> <mode>
  # Separate statements: every right hand side in one local is expanded before any of them is
  # assigned, so a log built from mode in the same line comes out blank.
  local v="$1"
  local mode="$2"
  local log="$LOGS/$mode-$v.log"
  JAVA_TOOL_OPTIONS="-Dnotchcurrency.shareTest=$mode -Dnotchcurrency.shareDir=$SHARE -Dnotchcurrency.mcver=$v" \
    ./gradlew ":${v}:runClient" --console=plain --args="--quickPlaySingleplayer \"New World\"" > "$log" 2>&1 &
  local pid=$!
  for _ in $(seq 1 100); do
    grep -q 'NotchCurrency-ShareTest' "$log" 2>/dev/null && break
    grep -qE 'BUILD FAILED|Game crashed' "$log" 2>/dev/null && break
    sleep 3
  done
  sleep 4
  pkill -f 'runClient|KnotClient' 2>/dev/null
  wait $pid 2>/dev/null
  local line
  line=$(sed -E 's/\x1b\[[0-9;]*m//g' "$log" | grep 'NotchCurrency-ShareTest' | head -2 | sed 's/.*ShareTest) //')
  echo "  $mode $v: ${line:-NO HARNESS OUTPUT}"
}

case "$1" in
  export)
    rm -rf "$SHARE"; mkdir -p "$SHARE" "$LOGS"
    echo "EXPORT"
    for v in $VERSIONS; do run "$v" export; done
    echo "--- files ---"; ls -1 "$SHARE"
    ;;
  import)
    mkdir -p "$LOGS"
    echo "IMPORT"
    for v in $VERSIONS; do run "$v" import; done
    ;;
esac
