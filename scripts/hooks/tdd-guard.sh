#!/bin/bash
# TDD Guard Hook — PreToolUse[Edit|Write]
# Android/Kotlin 구현 코드를 작성하려 할 때 대응 테스트가 먼저 있는지 확인한다.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.path // empty')

if [ -z "$FILE_PATH" ]; then
  exit 0
fi

ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
ABS_PATH="$FILE_PATH"
case "$ABS_PATH" in
  /*) ;;
  *) ABS_PATH="$ROOT/$FILE_PATH" ;;
esac

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

# 테스트 파일 자체, 문서, 설정, 리소스는 테스트 선행 대상이 아니다.
case "$FILE_PATH" in
  */src/test/*|*/src/androidTest/*|*Test.kt|*Spec.kt|*test*|*spec*|*.test.*|*.spec.*|*__tests__*)
    exit 0
    ;;
  *.json|*.xml|*.md|*.yml|*.yaml|*.properties|*.toml|*.gradle|*.gradle.kts|*.env*|*.config.*)
    exit 0
    ;;
  */res/*|*/assets/*|*/buildSrc/*)
    exit 0
    ;;
esac

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
}

if [ "$has_gradle" = true ]; then
  case "$FILE_PATH" in
    *.kt)
      source_root=""
      module_root=""
      rel_path=""

      if [[ "$ABS_PATH" == *"/src/main/java/"* ]]; then
        source_root="/src/main/java/"
      elif [[ "$ABS_PATH" == *"/src/main/kotlin/"* ]]; then
        source_root="/src/main/kotlin/"
      else
        exit 0
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

      for candidate in "${candidates[@]}"; do
        if [ -f "$candidate" ]; then
          exit 0
        fi
      done

      deny "$base_name" "app/src/test/java/.../${base_name}Test.kt"
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

exit 0
