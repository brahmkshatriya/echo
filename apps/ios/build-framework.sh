#!/bin/sh
set -eu

cd "$(dirname "$0")/../.."

# An IDE-supplied framework still needs its transitive Compose resources in the app bundle.
if [ "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-NO}" = "YES" ]; then
    ./gradlew :apps:ios:syncComposeResourcesForIos
else
    ./gradlew :apps:ios:embedAndSignAppleFrameworkForXcode
fi

font="$BUILT_PRODUCTS_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/compose-resources/composeResources/echo.client.generated.resources/font/GoogleSansFlex.ttf"
if [ ! -s "$font" ]; then
    printf 'error: Compose resources were not bundled: missing %s\n' "$font" >&2
    exit 1
fi
