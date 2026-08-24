#!/usr/bin/env bash
# Publishes krogu-time to Maven Central.
#
# The toolchain builds and signs the bundle; the upload is done here with curl.
# That is what lets the deployment carry a real name: the toolchain's own
# `publishToMavenCentral` posts the file under its fixed
# "<module>-central-bundle.zip", and the Portal shows that as the deployment
# name. Uploading directly puts "<module>-<version>" there instead.
#
# The deployment is USER_MANAGED: Central validates it and then waits. Nothing
# becomes public until it is released by hand at
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
name="$module-$version"

KOTLIN_TOOLCHAIN_SIGNING_KEY="$(gpg --batch --pinentry-mode loopback \
  --passphrase "$GPG_PASSPHRASE" --armor --export-secret-keys "$GPG_KEY_ID")"
KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE="$GPG_PASSPHRASE"
export KOTLIN_TOOLCHAIN_SIGNING_KEY KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE

echo "==> building and signing the bundle for io.heapy:krogu-time:$version"
KOTLIN_CLI_NO_WELCOME_BANNER=1 ./kotlin task ":$module:prepareMavenCentralBundle"

built="build/tasks/_${module}_prepareMavenCentralBundle/${module}-central-bundle.zip"
[ -f "$built" ] || { echo "the bundle task produced no zip at $built" >&2; exit 1; }
bundle="build/$name.zip"
cp "$built" "$bundle"
echo "bundle: $bundle ($(unzip -l "$bundle" | tail -1 | awk '{print $2}') files)"

auth="$(printf '%s' "$CENTRAL_TOKEN" | base64 | tr -d '\n')"

echo "==> uploading as '$name'"
deployment_id="$(curl -s --fail-with-body \
  --header "Authorization: Bearer $auth" \
  --form "bundle=@$bundle" \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED&name=$name")"
echo "deployment id: $deployment_id"

echo "==> waiting for validation"
# Central validates asynchronously. VALIDATED is the terminal state for a
# USER_MANAGED deployment: it stops there and waits for the release button.
for _ in $(seq 1 60); do
  status="$(curl -s --header "Authorization: Bearer $auth" -X POST \
    "https://central.sonatype.com/api/v1/publisher/status?id=$deployment_id")"
  state="$(printf '%s' "$status" | sed -n 's/.*"deploymentState":"\([A-Z_]*\)".*/\1/p')"
  echo "  $state"
  case "$state" in
    VALIDATED|PUBLISHED) break ;;
    FAILED)
      echo "$status" >&2
      exit 1
      ;;
  esac
  sleep 5
done

echo
echo "Deployment '$name' is $state and waiting."
echo "Release it at https://central.sonatype.com/publishing/deployments"
