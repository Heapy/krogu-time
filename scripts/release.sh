#!/usr/bin/env bash
# Publishes krogu-time to Maven Central through the toolchain's built-in Central
# Portal support. The module declares `publishingMode: manual`, so this only
# stages a deployment: nothing becomes public until it is released by hand at
# https://central.sonatype.com/publishing/deployments.
#
# The signing key is exported from the local GPG keyring at run time rather than
# stored anywhere: the toolchain wants the ASCII-armored private key in
# KOTLIN_TOOLCHAIN_SIGNING_KEY.
#
# Environment:
#   GPG_KEY_ID       key id or fingerprint to sign with (required)
#   GPG_PASSPHRASE   passphrase for that key (required, non-interactive export)
#   CENTRAL_TOKEN    "<username>:<password>" Central Portal user token (required)
#
# Credentials belong in the git-ignored ./publish.sh wrapper, which exports them
# and calls this script.
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

: "${GPG_KEY_ID:?set GPG_KEY_ID to the key you sign releases with}"
: "${GPG_PASSPHRASE:?set GPG_PASSPHRASE so the key can be exported without a prompt}"
: "${CENTRAL_TOKEN:?set CENTRAL_TOKEN to '<username>:<password>' from the Central Portal}"

# `version:` appears more than once in module.yaml (the JDK version too), so
# anchor on the publishing block's four-space indent.
version="$(sed -n 's/^    version: *//p' module.yaml | head -1)"
[ -n "$version" ] || { echo "cannot read version from module.yaml" >&2; exit 1; }

# The module name is the directory name, which is `grogu-time` here while the
# artifact is `krogu-time`. Derive it so a rename of either one cannot desync.
module="$(basename "$repo_root")"

# The token is one "user:pass" string in the Portal UI, but the toolchain takes
# the two halves separately.
KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_USERNAME="${CENTRAL_TOKEN%%:*}"
KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_PASSWORD="${CENTRAL_TOKEN#*:}"
KOTLIN_TOOLCHAIN_SIGNING_KEY="$(gpg --batch --pinentry-mode loopback \
  --passphrase "$GPG_PASSPHRASE" --armor --export-secret-keys "$GPG_KEY_ID")"
KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE="$GPG_PASSPHRASE"
export KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_USERNAME KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_PASSWORD
export KOTLIN_TOOLCHAIN_SIGNING_KEY KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE

echo "==> publishing io.heapy:krogu-time:$version to Maven Central"
# The task is invoked directly instead of through `kotlin publish mavenCentral`.
# On toolchain 0.12.0-dev-4300 that command first checks the requested id against
# the module's publishable repositories, and the built-in Central publication is
# not in that list, so it always fails with "not marked as publishable". Adding
# the id under `repositories` to satisfy the check collides with the task the
# toolchain already registers ("Task ':<module>:publishToMavenCentral' already
# exists"). Running the task itself skips the check and does the same work.
# Re-test `kotlin publish mavenCentral` after a toolchain upgrade.
KOTLIN_CLI_NO_WELCOME_BANNER=1 ./kotlin task ":$module:publishToMavenCentral"

# The toolchain names the bundle "<module>-central-bundle.zip" and buries it in
# the task output directory, so the file on disk does not say which release it
# is. Copy it out under a versioned name.
bundle="build/tasks/_${module}_prepareMavenCentralBundle/${module}-central-bundle.zip"
if [ -f "$bundle" ]; then
  cp "$bundle" "build/${module}-${version}.zip"
  echo
  echo "bundle: build/${module}-${version}.zip"
fi
