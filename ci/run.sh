#!/usr/bin/env bash
#
# GBL-013 US-4 (wave 1) — CI contract for conventions-gradle.
#
# The pipeline YAML is an adapter: it checks out, selects the runtime image
# (`cccp-ci-base`, pinned by digest) and delegates to this script. The exit code
# is the contract (see workspace-bom/ci/README.adoc).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/conventions-plugin"

job="${1:-}"

case "$job" in
  fast)
    ./gradlew compileKotlin compileTestKotlin --no-daemon --console=plain
    ;;
  test)
    ./gradlew check --no-daemon --console=plain
    ;;
  *)
    printf 'unsupported job for conventions-gradle: %s\n' "${job:-<none>}" >&2
    printf 'supported: fast | test\n' >&2
    exit 2
    ;;
esac
