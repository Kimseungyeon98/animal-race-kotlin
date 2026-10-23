#!/usr/bin/env bash
# Claude Code PreToolUse 훅: git commit 전에 tools/verify 테스트(공정성·전개·랜덤 등수·미니게임)를 돌리고,
# 실패하면 커밋을 막는다 (exit 2 → Claude에게 실패 내용이 전달됨).
# 입력: stdin JSON {"tool_input":{"command":"..."}}
cmd="$(jq -r '.tool_input.command // ""')"
case "$cmd" in
  *"git commit"*) ;;
  *) exit 0 ;;
esac
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root/tools/verify" || exit 0
# 캐시가 있으면 오프라인으로 빠르게, 없으면(새 세션) 내려받으며
if ! out="$(./gradlew test -q --offline 2>&1)"; then
  if echo "$out" | grep -q "No cached version\|Could not resolve\|offline mode"; then
    out="$(./gradlew test -q 2>&1)" || { echo "커밋 전 테스트 실패 — 커밋을 막았어요." >&2; echo "$out" | grep -v JAVA_TOOL | tail -30 >&2; exit 2; }
  else
    echo "커밋 전 테스트 실패 — 커밋을 막았어요." >&2; echo "$out" | grep -v JAVA_TOOL | tail -30 >&2; exit 2
  fi
fi
exit 0
