#!/usr/bin/env bash
#
# Regenerate PrintFlagsFinal snapshots under json-graph-generator/src/test/resources/printflagsfinal/.
#
#   java-<N>-hotspot.md   HotSpot (Corretto 8, Temurin 11/17/21/25) via SDKMAN
#   java-<N>-graalvm.md   GraalVM CE (17, 21, 25) via SDKMAN
#
# Usage:
#   ./scripts/regenerate-printflagsfinal.sh                    # all HotSpot + GraalVM
#   ./scripts/regenerate-printflagsfinal.sh hotspot            # all HotSpot versions
#   ./scripts/regenerate-printflagsfinal.sh graalvm            # all GraalVM versions
#   ./scripts/regenerate-printflagsfinal.sh hotspot 17          # one HotSpot release
#   ./scripts/regenerate-printflagsfinal.sh graalvm 21          # one GraalVM release
#   ./scripts/regenerate-printflagsfinal.sh --install            # install SDKs, then all
#   ./scripts/regenerate-printflagsfinal.sh --install graalvm 25

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MAVEN_ROOT="${REPO_ROOT}/json-graph-generator"
OUT_DIR="${MAVEN_ROOT}/src/test/resources/printflagsfinal"

# shellcheck source=sdk-java-common.sh
source "${SCRIPT_DIR}/sdk-java-common.sh"

INSTALL_SDKS=false
VM_KIND="all"
VERSION=""

usage() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS] [hotspot|graalvm|all] [8|11|17|21|25]

Regenerate -XX:+PrintFlagsFinal snapshots as markdown under:
  ${OUT_DIR}/

VM kinds:
  hotspot   Java 8, 11, 17, 21, 25 (Corretto / Temurin via SDKMAN)
  graalvm   Java 17, 21, 25 (GraalVM CE via SDKMAN; override ids with GRAALVM_SDK_ID_*)
  all       Both kinds (default when omitted)

Options:
  --install   Run \`sdk install java <id>\` for each required JDK before capturing flags
  -h, --help  Show this help

Examples:
  $(basename "$0")
  $(basename "$0") --install hotspot
  $(basename "$0") graalvm 21
  $(basename "$0") hotspot 17

SDKMAN identifiers (see scripts/sdk-java-common.sh):
  HotSpot 8  -> \${JAVA_SDK_ID_8}
  HotSpot 11,17,21,25 -> Temurin (*-tem)
  GraalVM 17,21,25 -> GraalVM CE (*-graalce); Oracle *-graal via env overrides

List GraalVM builds: sdk list java | grep -i graal
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        usage
        exit 0
        ;;
      --install)
        INSTALL_SDKS=true
        shift
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
      all)
        sdk_java_validate_version "${VERSION}" || {
          usage >&2
          sdk_java_die "invalid version '${VERSION}'; HotSpot: ${VALID_HOTSPOT_VERSIONS[*]}; GraalVM: ${VALID_GRAALVM_VERSIONS[*]}"
        }
        ;;
    esac
  fi
}

capture_print_flags_final() {
  local outfile="$1"
  # HotSpot and GraalVM often exit non-zero after printing flags; keep output if valid.
  java -XX:+PrintFlagsFinal > "${outfile}" 2>&1 || true
  if [[ ! -s "${outfile}" ]] || ! grep -q '\[Global flags\]' "${outfile}"; then
    sdk_java_die "PrintFlagsFinal produced no usable output in ${outfile}"
  fi
}

regenerate_hotspot_one() {
  local major="$1"
  local outfile="${OUT_DIR}/java-${major}-hotspot.md"
  local java_id
  java_id="$(sdk_java_id_for_major "${major}")"
  echo "=== HotSpot Java ${major} (${java_id}) ==="
  if [[ "${INSTALL_SDKS}" == true ]]; then
    sdk_java_install "${java_id}"
  fi
  sdk_java_use "${java_id}"
  capture_print_flags_final "${outfile}"
  echo "Wrote ${outfile} ($(wc -l < "${outfile}" | tr -d ' ') lines)"
}

regenerate_graalvm_one() {
  local major="$1"
  local outfile="${OUT_DIR}/java-${major}-graalvm.md"
  local java_id
  java_id="$(sdk_graalvm_id_for_major "${major}")"
  echo "=== GraalVM Java ${major} (${java_id}) ==="
  if [[ "${INSTALL_SDKS}" == true ]]; then
    sdk_java_install "${java_id}"
  fi
  sdk_java_use "${java_id}"
  capture_print_flags_final "${outfile}"
  echo "Wrote ${outfile} ($(wc -l < "${outfile}" | tr -d ' ') lines)"
}

run_hotspot() {
  local major
  if [[ -n "${VERSION}" ]]; then
    regenerate_hotspot_one "${VERSION}"
    return
  fi
  for major in "${VALID_HOTSPOT_VERSIONS[@]}"; do
    regenerate_hotspot_one "${major}"
  done
}

run_graalvm() {
  local major
  if [[ -n "${VERSION}" ]]; then
    regenerate_graalvm_one "${VERSION}"
    return
  fi
  for major in "${VALID_GRAALVM_VERSIONS[@]}"; do
    regenerate_graalvm_one "${major}"
  done
}

run_all_for_version() {
  local major="$1"
  if sdk_java_validate_hotspot_version "${major}"; then
    regenerate_hotspot_one "${major}"
  fi
  if sdk_java_validate_graalvm_version "${major}"; then
    regenerate_graalvm_one "${major}"
  fi
}

main() {
  parse_args "$@"

  export SDKMAN_OFFLINE_MODE="${SDKMAN_OFFLINE_MODE:-false}"
  sdk_java_source_sdkman
  mkdir -p "${OUT_DIR}"

  if [[ "${INSTALL_SDKS}" == true && -z "${VERSION}" && "${VM_KIND}" == "all" ]]; then
    echo "Installing HotSpot and GraalVM SDKMAN candidates..."
    sdk_java_install_all_hotspot
    sdk_graalvm_install_all
    echo
  fi

  case "${VM_KIND}" in
    hotspot)
      run_hotspot
      ;;
    graalvm)
      run_graalvm
      ;;
    all)
      if [[ -n "${VERSION}" ]]; then
        run_all_for_version "${VERSION}"
      else
        run_hotspot
        echo
        run_graalvm
      fi
      ;;
  esac
}

main "$@"
