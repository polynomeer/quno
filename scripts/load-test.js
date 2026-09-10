// 부하 테스트(production-readiness.md B-5). 비로그인 공개 읽기 경로(검색/태그 목록/질문 상세,
// ADR-0041/0042)만 다룬다 — 로그인이 필요한 쓰기 경로는 VU마다 계정을 발급/관리해야 해서 범위 밖.
// 실제 용량 산정에 의미 있는 결과를 얻으려면 스테이징 인프라(사람이 할 일, production-readiness.md
// A)에 대고 돌려야 한다 — 로컬 docker-compose 한 대짜리 Postgres/Redis로는 병목이 인프라
// 사이징이 아니라 이 개발 머신 자체가 되기 쉽다. 여기서는 스크립트 자체가 동작하는지만 확인한다.
//
// 사용법:
//   k6 run scripts/load-test.js
//   BASE_URL=https://staging.example.com VUS=50 DURATION=2m k6 run scripts/load-test.js

import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";
const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || "1m";

export const options = {
  scenarios: {
    public_read: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: VUS },
        { duration: DURATION, target: VUS },
        { duration: "15s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

const SEARCH_QUERIES = ["kotlin", "spring", "docker", "redis", "postgres"];

export default function () {
  const query = SEARCH_QUERIES[Math.floor(Math.random() * SEARCH_QUERIES.length)];

  const searchRes = http.get(`${BASE_URL}/api/v1/search?q=${query}`, { tags: { name: "search" } });
  check(searchRes, { "search: status 200": (r) => r.status === 200 });

  const tagsRes = http.get(`${BASE_URL}/api/v1/tags`, { tags: { name: "tags" } });
  check(tagsRes, { "tags: status 200": (r) => r.status === 200 });

  let questions = [];
  try {
    questions = JSON.parse(searchRes.body);
  } catch {
    // 빈 응답/파싱 실패는 위 check가 이미 실패로 표시한다
  }

  if (Array.isArray(questions) && questions.length > 0) {
    const id = questions[Math.floor(Math.random() * questions.length)].id;
    const detailRes = http.get(`${BASE_URL}/api/v1/questions/${id}`, { tags: { name: "question_detail" } });
    check(detailRes, { "question detail: status 200": (r) => r.status === 200 });
  }

  sleep(1);
}
