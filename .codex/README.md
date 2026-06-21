# `.codex/` — Codex 프로젝트 설정

이 디렉터리는 [Codex](https://developers.openai.com/codex)의 **프로젝트-로컬** 설정 레이어다.
`/Users/jje/cafeminsu`가 Codex에서 trusted project이면 이 레이어가 로드된다.

## `hooks.json` — TDD 가드 (PreToolUse)

`apply_patch`(= 파일 편집)로 `src/main`의 Kotlin 구현 파일을 작성하려 할 때, 대응 테스트가
먼저 있는지 검사하고 없으면 `permissionDecision: "deny"`로 차단한다.
구현은 [`scripts/hooks/tdd-guard-codex.sh`](../scripts/hooks/tdd-guard-codex.sh)
(Claude Code의 `scripts/hooks/tdd-guard.sh`를 Codex 훅 규격으로 포팅).

### ⚠️ 동작 범위 (중요)

| 실행 모드 | 훅 발화 |
| --- | --- |
| **대화형 Codex (TUI)** — `codex` | ✅ 발화 (이 repo에서) |
| **헤드리스** — `codex exec` (하네스 `scripts/execute.py`) | ❌ **발화 안 됨** |

Codex 0.140.0 기준 `PreToolUse` 훅은 **대화형 TUI 경로에서만** 동작하고 `codex exec`에서는
`apply_patch`·`Bash` 모두 발화되지 않음을 실측 확인했다(trusted project + `--dangerously-bypass-hook-trust`
+ 모든 matcher 조합에서도 동일). 따라서 **하네스(`codex exec`) 빌드의 TDD 강제는 이 훅이 아니라
주입 가드레일(`CLAUDE.md`/`AGENTS.md`의 "테스트 우선" 규칙)에 의존**한다.

### 사용 (대화형)

대화형 Codex에서 이 repo를 처음 열면 새 훅은 review 대상이다. CLI에서 `/hooks`로 검토 후 trust 하면
이후 자동 적용된다. (`scripts/hooks/tdd-guard-codex.sh`는 자신이 속한 repo에서만 enforce하도록 self-scope됨.)

## `config.toml` — 프로젝트 로컬 Codex 설정

이 repo가 trusted project일 때 로드되는 Codex 설정이다.

### Figma MCP

Figma 원격 MCP 서버를 프로젝트 범위로 등록한다.

- 서버 ID: `figma`
- URL: `https://mcp.figma.com/mcp`
- 인증: Codex의 MCP OAuth 흐름으로 진행

처음 사용 전 대화형 Codex에서 `/mcp`로 `figma` 서버를 확인하고, 필요하면 `codex mcp login figma`로
Figma OAuth 인증을 완료한다.
