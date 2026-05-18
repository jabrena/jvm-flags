# Shared SDKMAN Java helpers for project scripts.
# Source from other scripts: source "${SCRIPT_DIR}/sdk-java-common.sh"

VALID_JAVA_VERSIONS=(8 11 17 21 25)
VALID_HOTSPOT_VERSIONS=(8 11 17 21 25)
VALID_GRAALVM_VERSIONS=(17 21 25)

SDKMAN_INIT="${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
JAVA_CANDIDATES="${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java"

# HotSpot (Temurin / Corretto): sdk install java <id>
JAVA_SDK_ID_8="8.0.452-amzn"
JAVA_SDK_ID_11="11.0.27-tem"
JAVA_SDK_ID_17="17.0.19-tem"
JAVA_SDK_ID_21="21.0.11-tem"
JAVA_SDK_ID_25="25.0.3-tem"

# GraalVM Community Edition (graalce): sdk install java <id>
# Oracle GraalVM alternatives: 17.0.12-graal, 21.0.11-graal, 25.0.3-graal
GRAALVM_SDK_ID_17="${GRAALVM_SDK_ID_17:-17.0.9-graalce}"
GRAALVM_SDK_ID_21="${GRAALVM_SDK_ID_21:-21.0.2-graalce}"
GRAALVM_SDK_ID_25="${GRAALVM_SDK_ID_25:-25.0.2-graalce}"

sdk_java_die() {
  echo "error: $*" >&2
  exit 1
}

sdk_java_source_sdkman() {
  [[ -f "${SDKMAN_INIT}" ]] || sdk_java_die "SDKMAN not found at ${SDKMAN_INIT}"
  set +u
  # shellcheck source=/dev/null
  source "${SDKMAN_INIT}"
  set -u
}

sdk_java_id_for_major() {
  local major="$1"
  case "${major}" in
    8) echo "${JAVA_SDK_ID_8}" ;;
    11) echo "${JAVA_SDK_ID_11}" ;;
    17) echo "${JAVA_SDK_ID_17}" ;;
    21) echo "${JAVA_SDK_ID_21}" ;;
    25) echo "${JAVA_SDK_ID_25}" ;;
    *) sdk_java_die "unknown Java feature release '${major}'; allowed: ${VALID_JAVA_VERSIONS[*]}" ;;
  esac
}

sdk_graalvm_id_for_major() {
  local major="$1"
  case "${major}" in
    17) echo "${GRAALVM_SDK_ID_17}" ;;
    21) echo "${GRAALVM_SDK_ID_21}" ;;
    25) echo "${GRAALVM_SDK_ID_25}" ;;
    *) sdk_java_die "GraalVM not catalogued for Java ${major}; allowed: ${VALID_GRAALVM_VERSIONS[*]}" ;;
  esac
}

sdk_java_validate_hotspot_version() {
  local arg="$1"
  local v
  for v in "${VALID_HOTSPOT_VERSIONS[@]}"; do
    [[ "${arg}" == "${v}" ]] && return 0
  done
  return 1
}

sdk_java_validate_graalvm_version() {
  local arg="$1"
  local v
  for v in "${VALID_GRAALVM_VERSIONS[@]}"; do
    [[ "${arg}" == "${v}" ]] && return 0
  done
  return 1
}

sdk_java_install() {
  local java_id="$1"
  if [[ -d "${JAVA_CANDIDATES}/${java_id}" ]]; then
    return 0
  fi
  echo "Installing Java: ${java_id}"
  sdk install java "${java_id}" <<< 'n'
}

sdk_java_use() {
  local java_id="$1"
  sdk_java_install "${java_id}"
  echo "Default Java: ${java_id}"
  sdk default java "${java_id}" >/dev/null
  export JAVA_HOME="${JAVA_CANDIDATES}/${java_id}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
  java -version
  echo
}

sdk_java_use_major() {
  sdk_java_use "$(sdk_java_id_for_major "$1")"
}

sdk_graalvm_use_major() {
  sdk_java_use "$(sdk_graalvm_id_for_major "$1")"
}

sdk_java_validate_version() {
  sdk_java_validate_hotspot_version "$1" || sdk_java_validate_graalvm_version "$1"
}

sdk_java_install_hotspot_major() {
  sdk_java_install "$(sdk_java_id_for_major "$1")"
}

sdk_graalvm_install_major() {
  sdk_java_install "$(sdk_graalvm_id_for_major "$1")"
}

sdk_java_install_all_hotspot() {
  local major
  for major in "${VALID_HOTSPOT_VERSIONS[@]}"; do
    sdk_java_install_hotspot_major "${major}"
  done
}

sdk_graalvm_install_all() {
  local major
  for major in "${VALID_GRAALVM_VERSIONS[@]}"; do
    sdk_graalvm_install_major "${major}"
  done
}
