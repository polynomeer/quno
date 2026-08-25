#!/usr/bin/env bash
# MVP 핵심 가설(Living Question — 질문은 죽은 게시물이 아니라 리비전·와드·태그로
# 계속 살아있다) 을 실제 API 호출로 보여주는 데모 스크립트. PLAN.md Phase 4.3.
#
# 사전 조건: docker compose up -d 로 인프라가 떠 있고,
#   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun 으로 서버가 http://localhost:8081 에서 실행 중이어야 한다.
#
# 사용법: ./scripts/demo.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
RUN_ID=$(date +%s)

step() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$1"; }
json() { jq .; }

step "0. 서버 상태 확인"
curl -sf "$BASE_URL/actuator/health" | json

step "1. 두 사용자 가입 — Alice(질문자), Bob(답변자/Ward)"
signup() {
  curl -s -X POST "$BASE_URL/api/v1/auth/signup" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"nickname\":\"$2\",\"password\":\"password123\"}" >/dev/null
}
login() {
  curl -s -X POST "$BASE_URL/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"password123\"}" | jq -r .accessToken
}

ALICE_EMAIL="alice-demo-$RUN_ID@example.com"
BOB_EMAIL="bob-demo-$RUN_ID@example.com"
signup "$ALICE_EMAIL" "alice$RUN_ID"
signup "$BOB_EMAIL" "bob$RUN_ID"
ALICE_TOKEN=$(login "$ALICE_EMAIL")
BOB_TOKEN=$(login "$BOB_EMAIL")
echo "Alice, Bob 로그인 완료"

step "2. Alice가 질문(Qv1) 작성 — 태그 kotlin, spring-boot"
QUESTION=$(curl -s -X POST "$BASE_URL/api/v1/questions" \
  -H "Authorization: Bearer $ALICE_TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Spring Boot 4에서 Kotlin coroutine이 멈춰요","body":"suspend 함수 호출 후 응답이 안 옵니다.","tags":["kotlin","spring-boot"]}')
echo "$QUESTION" | json
QUESTION_ID=$(echo "$QUESTION" | jq -r .id)

step "3. Bob이 이 질문을 Ward(watch) — '이 질문이 어떻게 발전하는지 지켜본다'"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE_URL/api/v1/questions/$QUESTION_ID/watch" \
  -H "Authorization: Bearer $BOB_TOKEN"

step "4. Alice가 질문을 리비전(Qv2) — 로그를 추가해 질문을 더 명확하게 만든다"
curl -s -X POST "$BASE_URL/api/v1/questions/$QUESTION_ID/versions" \
  -H "Authorization: Bearer $ALICE_TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Spring Boot 4에서 Kotlin coroutine이 멈춰요","body":"suspend 함수 호출 후 응답이 안 옵니다.","logs":"DEBUG: dispatcher thread blocked at Dispatchers.IO"}' | json

step "5. 두 버전 사이의 diff 확인 — 질문이 '리비전 가능한 카드'라는 증거"
curl -s "$BASE_URL/api/v1/questions/$QUESTION_ID/versions/2/diff" \
  -H "Authorization: Bearer $ALICE_TOKEN" | json

step "6. Bob에게 QUESTION_REVISION 알림이 왔는지 확인 (Ward 알림)"
echo "(outbox 스케줄러가 2초 주기로 비동기 처리하므로 잠시 대기)"
sleep 3
curl -s "$BASE_URL/api/v1/me/notifications" -H "Authorization: Bearer $BOB_TOKEN" | jq '[.[] | select(.type == "QUESTION_REVISION")]'

step "7. Bob이 답변 작성"
ANSWER=$(curl -s -X POST "$BASE_URL/api/v1/questions/$QUESTION_ID/answers" \
  -H "Authorization: Bearer $BOB_TOKEN" -H 'Content-Type: application/json' \
  -d '{"body":"Dispatchers.IO 대신 Dispatchers.Default를 써보세요."}')
echo "$ANSWER" | json
ANSWER_ID=$(echo "$ANSWER" | jq -r .id)

step "8. Alice가 답변을 채택 — 질문 상태가 RESOLVED로 전환"
curl -s -X POST "$BASE_URL/api/v1/answers/$ANSWER_ID/accept" -H "Authorization: Bearer $ALICE_TOKEN" | json

step "9. Bob에게 ANSWER_ACCEPTED 알림이 왔는지 확인"
sleep 3
curl -s "$BASE_URL/api/v1/me/notifications" -H "Authorization: Bearer $BOB_TOKEN" | jq '[.[] | select(.type == "ANSWER_ACCEPTED")]'

step "10. 검색으로 이 질문이 잡히는지 확인"
curl -s "$BASE_URL/api/v1/search?q=coroutine&limit=5" -H "Authorization: Bearer $ALICE_TOKEN" | json

step "11. Bob이 kotlin 태그를 팔로우하면 태그 기반 추천에 뜨는지 확인"
TAGS=$(curl -s "$BASE_URL/api/v1/tags?q=kotlin" -H "Authorization: Bearer $BOB_TOKEN")
KOTLIN_TAG_ID=$(echo "$TAGS" | jq -r '.[0].id')
curl -s -o /dev/null -w "태그 팔로우 HTTP %{http_code}\n" -X POST "$BASE_URL/api/v1/tags/$KOTLIN_TAG_ID/follow" -H "Authorization: Bearer $BOB_TOKEN"
curl -s "$BASE_URL/api/v1/recommendations/questions?source=tags" -H "Authorization: Bearer $BOB_TOKEN" | json

step "12. Alice의 공개 프로필 — 작성 질문/답변이 누적되는지 확인"
ALICE_ID=$(curl -s "$BASE_URL/api/v1/me" -H "Authorization: Bearer $ALICE_TOKEN" | jq -r .id)
curl -s "$BASE_URL/api/v1/users/$ALICE_ID/profile" -H "Authorization: Bearer $ALICE_TOKEN" | json

step "13. 라이트 대시보드 — 인기 질문/트렌딩 태그에 이 질문이 반영되는지 확인"
curl -s "$BASE_URL/api/v1/dashboard" -H "Authorization: Bearer $ALICE_TOKEN" | json

step "14. MVP 성공 지표 스냅샷 (Phase 4.1 계측)"
curl -s "$BASE_URL/api/v1/metrics" -H "Authorization: Bearer $ALICE_TOKEN" | json

printf '\n\033[1;32m데모 완료 — 질문 #%s 가 생성→리비전→답변→채택→Ward 알림까지 살아있는 흐름을 모두 통과했습니다.\033[0m\n' "$QUESTION_ID"
