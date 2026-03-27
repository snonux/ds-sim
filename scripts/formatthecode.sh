#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "${SCRIPT_DIR}/.." && pwd)

usage() {
    cat <<'EOF'
Usage: ./scripts/formatthecode.sh [--] [JAVA_FILE ...]

Formats Java sources with Artistic Style using the project's historical style.

Without arguments, formats all Java files under:
  - src/main/java
  - src/test/java

With arguments, formats only the provided files.
Use `--` before filenames that begin with `-`.

Options:
  -h, --help        Show this help text
  --check-deps      Verify required formatter dependencies are installed
EOF
}

check_deps() {
    if ! command -v astyle >/dev/null 2>&1; then
        echo "error: required formatter 'astyle' was not found in PATH" >&2
        echo "Install Artistic Style to use ./scripts/formatthecode.sh" >&2
        return 1
    fi
}

collect_default_targets() {
    find "${REPO_ROOT}/src/main/java" "${REPO_ROOT}/src/test/java" \
        -type f -name '*.java' -print0
}

sanitize_path_for_astyle() {
    local path="$1"

    if [[ "${path}" == -* ]]; then
        printf './%s\n' "${path}"
        return
    fi

    printf '%s\n' "${path}"
}

main() {
    local -a file_args=()
    local -a astyle_args=()

    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                usage
                return 0
                ;;
            --check-deps)
                check_deps
                return 0
                ;;
            --)
                shift
                file_args=("$@")
                break
                ;;
            -*)
                echo "error: unknown option: $1" >&2
                echo "Use -- before filenames that begin with '-'." >&2
                return 1
                ;;
            *)
                file_args+=("$1")
                ;;
        esac

        shift
    done

    check_deps

    if [[ ${#file_args[@]} -gt 0 ]]; then
        local target
        for target in "${file_args[@]}"; do
            if [[ ! -f "${target}" ]]; then
                echo "error: file not found: ${target}" >&2
                return 1
            fi
        done

        for target in "${file_args[@]}"; do
            astyle_args+=("$(sanitize_path_for_astyle "${target}")")
        done

        astyle --style=java --mode=java -n "${astyle_args[@]}"
        return 0
    fi

    mapfile -d '' targets < <(collect_default_targets)

    if [[ ${#targets[@]} -eq 0 ]]; then
        echo "No Java files found to format." >&2
        return 0
    fi

    astyle --style=java --mode=java -n "${targets[@]}"
}

main "$@"
