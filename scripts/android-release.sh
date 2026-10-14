#!/bin/bash
# Passwords are entered only in the user's terminal, never as command arguments.
set +x
set -euo pipefail
umask 077

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
if [[ ! -t 0 || ! -t 1 ]]; then
    echo 'Run this helper in an interactive terminal; do not enter passwords in chat.' >&2
    exit 1
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -x '/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool' ]]; then
        export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
    else
        echo 'Set JAVA_HOME to an installed JDK before running this helper.' >&2
        exit 1
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
export OH_MY_HAZE_KEYSTORE_PATH="${OH_MY_HAZE_KEYSTORE_PATH:-$HOME/.android/oh-my-haze/upload.jks}"
export OH_MY_HAZE_KEY_ALIAS="${OH_MY_HAZE_KEY_ALIAS:-oh-my-haze-upload}"

# Check the resolved destination before creating a key, including symlinks.
python3 - "$REPO_ROOT" "$OH_MY_HAZE_KEYSTORE_PATH" <<'PY'
from pathlib import Path
import sys
repo, key = Path(sys.argv[1]).resolve(), Path(sys.argv[2])
if not key.is_absolute() or key.resolve().is_relative_to(repo):
    raise SystemExit('Keystore path must be absolute and outside the repository.')
PY

case "${1:-}" in
    create-key)
        if [[ -e "$OH_MY_HAZE_KEYSTORE_PATH" ]]; then
            echo 'Keystore already exists; refusing to replace it. Use the build command.' >&2
            exit 1
        fi
        mkdir -p "$(dirname "$OH_MY_HAZE_KEYSTORE_PATH")"
        chmod 700 "$(dirname "$OH_MY_HAZE_KEYSTORE_PATH")"
        # keytool prompts with echo disabled, including password confirmation.
        # JKS supports a separate key password; Enter reuses the store password.
        keytool -genkeypair -storetype JKS \
            -keystore "$OH_MY_HAZE_KEYSTORE_PATH" -alias "$OH_MY_HAZE_KEY_ALIAS" \
            -keyalg RSA -keysize 3072 -validity 10000 \
            -dname 'CN=Oh My Haze Upload'
        chmod 600 "$OH_MY_HAZE_KEYSTORE_PATH"
        echo "Keystore created: $OH_MY_HAZE_KEYSTORE_PATH"
        echo 'Back up the keystore and save your passwords in a password manager.'
        ;;
    build)
        [[ -f "$OH_MY_HAZE_KEYSTORE_PATH" ]] || { echo 'Create the keystore first.' >&2; exit 1; }
        trap 'unset OH_MY_HAZE_STORE_PASSWORD OH_MY_HAZE_KEY_PASSWORD' EXIT
        unset OH_MY_HAZE_STORE_PASSWORD OH_MY_HAZE_KEY_PASSWORD
        read -r -s -p 'Keystore password: ' OH_MY_HAZE_STORE_PASSWORD </dev/tty
        printf '\n' >/dev/tty
        read -r -s -p 'Key password (Enter = same as keystore password): ' OH_MY_HAZE_KEY_PASSWORD </dev/tty
        printf '\n' >/dev/tty
        OH_MY_HAZE_KEY_PASSWORD="${OH_MY_HAZE_KEY_PASSWORD:-$OH_MY_HAZE_STORE_PASSWORD}"
        [[ -n "$OH_MY_HAZE_STORE_PASSWORD" ]] || { echo 'Empty password refused.' >&2; exit 1; }
        export OH_MY_HAZE_STORE_PASSWORD OH_MY_HAZE_KEY_PASSWORD
        cd "$REPO_ROOT"
        ./gradlew --no-daemon --no-configuration-cache --console=plain \
            testDebugUnitTest lintRelease assembleDebug bundleRelease
        BUNDLE="$REPO_ROOT/app/build/outputs/bundle/release/app-release.aab"
        python3 scripts/verify-android-bundle.py "$BUNDLE"
        echo "Signed bundle: $BUNDLE"
        ;;
    *)
        echo 'Usage: bash scripts/android-release.sh create-key|build' >&2
        exit 1
        ;;
esac
