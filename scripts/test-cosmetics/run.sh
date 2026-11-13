#!/bin/sh
# Runs the game-independent cosmetics tests (claims answers, per-account outfits).
# Needs a JDK 21+ and the Gson jar: GSON_JAR=/path/to/gson.jar sh scripts/test-cosmetics/run.sh
# (Gradle keeps one under ~/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/.)
set -eu
cd "$(dirname "$0")/../.."
GSON_JAR="${GSON_JAR:-$(find "$HOME/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson" -name 'gson-2*.jar' ! -name '*sources*' | head -n 1)}"
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";"; GSON_JAR="$(cygpath -w "$GSON_JAR")";; esac
OUT="build/test-cosmetics"
rm -rf "$OUT" && mkdir -p "$OUT"
javac -d "$OUT" -cp "$GSON_JAR" \
	src/main/java/com/orbitclient/cosmetics/ClaimsResponse.java \
	src/main/java/com/orbitclient/cosmetics/AccountOutfits.java \
	scripts/test-cosmetics/CosmeticsLogicTest.java
java -cp "$OUT$SEP$GSON_JAR" CosmeticsLogicTest
