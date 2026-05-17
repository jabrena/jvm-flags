#!/usr/bin/env bash
#
# Regenerate src/test/resources/printflagsfinal/java-<N>-hotspot.md from HotSpot -XX:+PrintFlagsFinal.
#   JDK ids pinned in sdk-java-common.sh (sdk install java <id>)
#
# Usage:
#   ./scripts/regenerate-printflagsfinal.sh          # all versions
#   ./scripts/regenerate-printflagsfinal.sh 17     # one version

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${PROJECT_ROOT}/src/test/resources/printflagsfinal"

# shellcheck source=sdk-java-common.sh
source "${SCRIPT_DIR}/sdk-java-common.sh"

usage() {
  cat <<EOF
Usage: $(basename "$0") [8|11|17|21|25]

  No argument  Regenerate java-8-hotspot.md … java-25-hotspot.md for all supported releases.
  8|11|17|21|25 Regenerate printflagsfinal/java-<version>-hotspot.md only.

JDK sources (SDKMAN):
  8  -> Corretto (*-amzn)
  11, 17, 21, 25 -> Temurin (*-tem)
EOF
}

regenerate_one() {
  local major="$1"
  local outfile="${OUT_DIR}/java-${major}-hotspot.md"
  sdk_java_use_major "${major}"
  java -XX:+PrintFlagsFinal > "${outfile}" 2>&1
  echo "Wrote ${outfile} ($(wc -l < "${outfile}" | tr -d ' ') lines)"
}

main() {
  if [[ $# -gt 1 ]]; then
    usage >&2
    exit 1
  fi

  if [[ $# -eq 1 && ( "${1}" == "-h" || "${1}" == "--help" ) ]]; then
    usage
    exit 0
  fi

  export SDKMAN_OFFLINE_MODE="${SDKMAN_OFFLINE_MODE:-false}"
  sdk_java_source_sdkman
  mkdir -p "${OUT_DIR}"

  if [[ $# -eq 0 ]]; then
    local major
    for major in "${VALID_JAVA_VERSIONS[@]}"; do
      echo "=== Java ${major} ==="
      regenerate_one "${major}"
    done
    return
  fi

  if ! sdk_java_validate_version "${1}"; then
    usage >&2
    sdk_java_die "invalid version '${1}'; allowed: ${VALID_JAVA_VERSIONS[*]}"
  fi

  regenerate_one "${1}"
}

main "$@"
