#!/bin/bash
# Codex TDD Guard Hook — PreToolUse[apply_patch]
# Codex가 apply_patch로 Android/Kotlin(또는 node) 구현 코드를 작성하려 할 때,
# 대응 테스트가 먼저 존재하는지 확인하고 없으면 차단(deny)한다.
# Claude Code의 scripts/hooks/tdd-guard.sh를 Codex 훅 규격으로 포팅한 것.
#
# 입력(stdin): Codex PreToolUse 페이로드 JSON
#   { hook_event_name, tool_name, tool_input, cwd, ... }
# 출력(stdout): 통과 시 없음(exit 0) / 차단 시 permissionDecision=deny JSON

INPUT=$(cat)

# jq가 없으면 가드를 비활성화한다(빌드 차단보다 통과 우선).
command -v jq >/dev/null 2>&1 || exit 0

PROJ_CWD=$(printf '%s' "$INPUT" | jq -r '.cwd // empty')
[ -z "$PROJ_CWD" ] && PROJ_CWD="$PWD"

# 전역(~/.codex/config.toml)에 등록되는 훅이지만, 이 스크립트가 속한 repo에서
# Codex가 동작할 때만 enforce 한다. 다른 프로젝트에서는 no-op.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SELF_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null)"
PROJ_ROOT="$(git -C "$PROJ_CWD" rev-parse --show-toplevel 2>/dev/null || echo "$PROJ_CWD")"
if [ -n "$SELF_ROOT" ] && [ "$SELF_ROOT" != "$PROJ_ROOT" ]; then
  exit 0
fi
ROOT="$PROJ_ROOT"

# 대상 파일 경로 추출:
#   1) apply_patch 패치 마커(*** Add/Update File: <path>)
#   2) 일반 키(file_path/path) fallback
FILES=$(
  {
    printf '%s' "$INPUT" | jq -r '.tool_input | .. | strings' 2>/dev/null \
      | grep -oE '^\*\*\* (Add|Update) File: .+' \
      | sed -E 's/^\*\*\* (Add|Update) File: //'
    printf '%s' "$INPUT" | jq -r '.tool_input.file_path // .tool_input.path // empty' 2>/dev/null
  } | sed '/^[[:space:]]*$/d' | sort -u
)
[ -z "$FILES" ] && exit 0

# 프로젝트 유형 감지
has_gradle=false
if [ -f "$ROOT/settings.gradle.kts" ] || [ -f "$ROOT/build.gradle.kts" ] || [ -f "$ROOT/gradle/libs.versions.toml" ]; then
  has_gradle=true
fi
has_node=false
if [ -f "$ROOT/package.json" ]; then
  has_node=true
fi
if [ "$has_gradle" = false ] && [ "$has_node" = false ]; then
  exit 0
fi

deny() {
  local base_name="$1"
  local example="$2"
  cat << EOF
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "TDD GUARD: '${base_name}'에 대한 테스트 파일이 존재하지 않습니다. 구현 코드를 작성하기 전에 테스트를 먼저 작성하세요. (예: ${example})"
  }
}
EOF
  exit 0
}

while IFS= read -r FILE_PATH; do
  [ -z "$FILE_PATH" ] && continue

  ABS_PATH="$FILE_PATH"
  case "$ABS_PATH" in
    /*) ;;
    *) ABS_PATH="$ROOT/$FILE_PATH" ;;
  esac

  # 테스트 파일 자체, 문서, 설정, 리소스는 테스트 선행 대상이 아니다.
  case "$FILE_PATH" in
    */src/test/*|*/src/androidTest/*|*Test.kt|*Spec.kt|*test*|*spec*|*.test.*|*.spec.*|*__tests__*)
      continue ;;
    *.json|*.xml|*.md|*.yml|*.yaml|*.properties|*.toml|*.gradle|*.gradle.kts|*.env*|*.config.*)
      continue ;;
    */res/*|*/assets/*|*/buildSrc/*)
      continue ;;
  esac

  if [ "$has_gradle" = true ]; then
    case "$FILE_PATH" in
      *.kt)
        source_root=""
        if [[ "$ABS_PATH" == *"/src/main/java/"* ]]; then
          source_root="/src/main/java/"
        elif [[ "$ABS_PATH" == *"/src/main/kotlin/"* ]]; then
          source_root="/src/main/kotlin/"
        else
          continue
        fi

        module_root="${ABS_PATH%%$source_root*}"
        rel_path="${ABS_PATH#*$source_root}"
        dir_rel=$(dirname "$rel_path")
        base_name=$(basename "$rel_path" .kt)

        candidates=(
          "$module_root/src/test/java/$dir_rel/${base_name}Test.kt"
          "$module_root/src/test/java/$dir_rel/${base_name}Spec.kt"
          "$module_root/src/test/kotlin/$dir_rel/${base_name}Test.kt"
          "$module_root/src/test/kotlin/$dir_rel/${base_name}Spec.kt"
          "$module_root/src/androidTest/java/$dir_rel/${base_name}Test.kt"
          "$module_root/src/androidTest/kotlin/$dir_rel/${base_name}Test.kt"
        )

        found=false
        for candidate in "${candidates[@]}"; do
          if [ -f "$candidate" ]; then
            found=true
            break
          fi
        done

        if [ "$found" = false ]; then
          deny "$base_name" "app/src/test/java/.../${base_name}Test.kt"
        fi
        ;;
    esac
  fi

  if [ "$has_node" = true ]; then
    case "$FILE_PATH" in
      *.ts|*.tsx|*.js|*.jsx)
        dir=$(dirname "$FILE_PATH")
        base_name=$(basename "$FILE_PATH" | sed -E 's/\.(ts|tsx|js|jsx)$//')
        test_found=false

        for ext in ts tsx js jsx; do
          if [ -f "${dir}/${base_name}.test.${ext}" ] || [ -f "${dir}/${base_name}.spec.${ext}" ]; then
            test_found=true
            break
          fi
        done

        if [ "$test_found" = false ]; then
          parent=$(dirname "$dir")
          for ext in ts tsx js jsx; do
            if [ -f "${parent}/__tests__/${base_name}.test.${ext}" ] || [ -f "${dir}/__tests__/${base_name}.test.${ext}" ]; then
              test_found=true
              break
            fi
          done
        fi

        if [ "$test_found" = false ]; then
          for ext in ts tsx js jsx; do
            if [ -f "${ROOT}/src/__tests__/${base_name}.test.${ext}" ]; then
              test_found=true
              break
            fi
          done
        fi

        if [ "$test_found" = false ]; then
          deny "$base_name" "${base_name}.test.ts"
        fi
        ;;
    esac
  fi
done <<< "$FILES"

exit 0
