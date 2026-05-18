#!/usr/bin/env bash
#
# Run ./mvnw test with pinned SDKMAN JDKs (see sdk-java-common.sh: JAVA_SDK_ID_*).
#
# Usage:
#   ./scripts/test-with-jdk.sh          # current SDKMAN Java, then test
#   ./scripts/test-with-jdk.sh 17     # switch to latest Temurin 17, then test
#   ./scripts/test-with-jdk.sh all    # test on 8, 11, 17, 21, and 25 in sequence

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MAVEN_ROOT="${REPO_ROOT}/json-graph-generator"

# shellcheck source=sdk-java-common.sh
source "${SCRIPT_DIR}/sdk-java-common.sh"

usage() {
  cat <<EOF
Usage: $(basename "$0") [8|11|17|21|25|all]

  No argument  Use the current SDKMAN Java (sdk current java), then run ./mvnw test.
  8|11|17|21|25 sdk install + sdk default the pinned JDK, then run ./mvnw test.
  all          Switch JDK and test on 8, then 11, 17, 21, and 25 in order.

Examples:
  $(basename "$0")
  $(basename "$0") 17
  $(basename "$0") all
EOF
}

run_tests_for_version() {
  local major="$1"
  echo "=== Java ${major} ==="
  sdk_java_use_major "${major}"
  run_tests
}

run_tests() {
  cd "${MAVEN_ROOT}"
  ./mvnw clean test
}

main() {
  export SDKMAN_OFFLINE_MODE="${SDKMAN_OFFLINE_MODE:-false}"
  if [[ $# -gt 1 ]]; then
    usage >&2
    exit 1
  fi

  if [[ $# -eq 1 && ( "${1}" == "-h" || "${1}" == "--help" ) ]]; then
    usage
    exit 0
  fi

  sdk_java_source_sdkman

  if [[ $# -eq 0 ]]; then
    echo "Current SDKMAN Java:"
    sdk current java
    java -version
    echo
    run_tests
    return
  fi

  if [[ "${1}" == "all" ]]; then
    local major
    for major in "${VALID_JAVA_VERSIONS[@]}"; do
      run_tests_for_version "${major}"
    done
    return
  fi

  if ! sdk_java_validate_version "${1}"; then
    usage >&2
    sdk_java_die "invalid version '${1}'; allowed: all ${VALID_JAVA_VERSIONS[*]}"
  fi

  run_tests_for_version "${1}"
}

main "$@"
