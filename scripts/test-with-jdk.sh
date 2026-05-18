#!/usr/bin/env bash
#
# Run ./mvnw test with pinned SDKMAN JDKs (see sdk-java-common.sh: JAVA_SDK_ID_*).
#
# Usage:
#   ./scripts/test-with-jdk.sh              # current SDKMAN Java, then test
#   ./scripts/test-with-jdk.sh 17           # switch to Temurin 17 (HotSpot), then test
#   ./scripts/test-with-jdk.sh all          # HotSpot on 8, 11, 17, 21, and 25 in sequence
#   ./scripts/test-with-jdk.sh hotspot      # same as all (all HotSpot versions)
#   ./scripts/test-with-jdk.sh graalvm      # GraalVM CE on 17, 21, and 25
#   ./scripts/test-with-jdk.sh hotspot 17   # one HotSpot release
#   ./scripts/test-with-jdk.sh graalvm 21   # one GraalVM release

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MAVEN_ROOT="${REPO_ROOT}/json-graph-generator"

# shellcheck source=sdk-java-common.sh
source "${SCRIPT_DIR}/sdk-java-common.sh"

VM_KIND=""
VERSION=""

usage() {
  cat <<EOF
Usage: $(basename "$0") [hotspot|graalvm|all] [8|11|17|21|25]

  No argument  Use the current SDKMAN Java (sdk current java), then run ./mvnw test.
  8|11|17|21|25 Default Java (HotSpot / Temurin), then run ./mvnw test.
  all          HotSpot on 8, then 11, 17, 21, and 25 in order (same as hotspot).

VM kinds:
  hotspot   Java 8, 11, 17, 21, 25 (Corretto / Temurin via SDKMAN)
  graalvm   Java 17, 21, 25 (GraalVM CE via SDKMAN)
  all       HotSpot on all catalogued feature releases (default when only "all" is given)

Examples:
  $(basename "$0")
  $(basename "$0") 17
  $(basename "$0") all
  $(basename "$0") hotspot
  $(basename "$0") graalvm
  $(basename "$0") all hotspot
  $(basename "$0") all graalvm
  $(basename "$0") hotspot 17
  $(basename "$0") graalvm 21
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        usage
        exit 0
        ;;
      hotspot|graalvm|all)
        VM_KIND="$1"
        shift
        ;;
      *)
        if [[ -z "${VERSION}" ]]; then
          VERSION="$1"
          shift
        else
          usage >&2
          exit 1
        fi
        ;;
    esac
  done

  if [[ "${VM_KIND}" == "all" && "${VERSION}" == "hotspot" ]]; then
    VM_KIND="hotspot"
    VERSION=""
  elif [[ "${VM_KIND}" == "all" && "${VERSION}" == "graalvm" ]]; then
    VM_KIND="graalvm"
    VERSION=""
  elif [[ -z "${VM_KIND}" && -n "${VERSION}" ]]; then
    VM_KIND="hotspot"
  elif [[ "${VM_KIND}" == "all" ]]; then
    VM_KIND="hotspot"
  fi

  if [[ -n "${VERSION}" ]]; then
    case "${VM_KIND}" in
      hotspot)
        sdk_java_validate_hotspot_version "${VERSION}" || {
          usage >&2
          sdk_java_die "invalid HotSpot version '${VERSION}'; allowed: ${VALID_HOTSPOT_VERSIONS[*]}"
        }
        ;;
      graalvm)
        sdk_java_validate_graalvm_version "${VERSION}" || {
          usage >&2
          sdk_java_die "invalid GraalVM version '${VERSION}'; allowed: ${VALID_GRAALVM_VERSIONS[*]}"
        }
        ;;
      *)
        usage >&2
        sdk_java_die "version '${VERSION}' requires hotspot or graalvm (e.g. $(basename "$0") hotspot ${VERSION})"
        ;;
    esac
  fi
}

run_tests() {
  cd "${MAVEN_ROOT}"
  ./mvnw clean test
}

run_tests_hotspot() {
  local major="$1"
  echo "=== HotSpot Java ${major} ==="
  sdk_java_use_major "${major}"
  run_tests
}

run_tests_graalvm() {
  local major="$1"
  echo "=== GraalVM Java ${major} ==="
  sdk_graalvm_use_major "${major}"
  run_tests
}

run_all_hotspot() {
  local major
  for major in "${VALID_HOTSPOT_VERSIONS[@]}"; do
    run_tests_hotspot "${major}"
  done
}

run_all_graalvm() {
  local major
  for major in "${VALID_GRAALVM_VERSIONS[@]}"; do
    run_tests_graalvm "${major}"
  done
}

main() {
  export SDKMAN_OFFLINE_MODE="${SDKMAN_OFFLINE_MODE:-false}"

  if [[ $# -gt 2 ]]; then
    usage >&2
    exit 1
  fi

  parse_args "$@"

  sdk_java_source_sdkman

  if [[ -z "${VM_KIND}" && -z "${VERSION}" ]]; then
    echo "Current SDKMAN Java:"
    sdk current java
    java -version
    echo
    run_tests
    return
  fi

  case "${VM_KIND}" in
    hotspot)
      if [[ -n "${VERSION}" ]]; then
        run_tests_hotspot "${VERSION}"
      else
        run_all_hotspot
      fi
      ;;
    graalvm)
      if [[ -n "${VERSION}" ]]; then
        run_tests_graalvm "${VERSION}"
      else
        run_all_graalvm
      fi
      ;;
    *)
      usage >&2
      sdk_java_die "invalid arguments; use: hotspot|graalvm|all [version]"
      ;;
  esac
}

main "$@"
