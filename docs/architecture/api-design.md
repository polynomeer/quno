# Quno API 설계 (MVP)

> 도메인 모델은 [domain-model.md](domain-model.md), 시스템 아키텍처는 [system-architecture.md](system-architecture.md) 참고.

## 주요 REST API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/v1/questions` | 질문과 Qv1 생성 |
| GET | `/api/v1/questions/{id}` | 질문 최신본/버전 요약 조회 |
| GET | `/api/v1/questions/{id}/versions/{version}` | 특정 질문 버전 조회 |
| POST | `/api/v1/questions/{id}/versions` | 새 질문 리비전 생성 |
| POST | `/api/v1/questions/{id}/answers` | 답변 등록 |
| GET | `/api/v1/questions/{id}/answers` | 답변 목록 |
| POST | `/api/v1/answers/{id}/accept` | 답변 채택 및 질문 RESOLVED 전환 |
| POST | `/api/v1/questions/{id}/watch` | 와드 등록 |
| DELETE | `/api/v1/questions/{id}/watch` | 와드 해제 |
| GET | `/api/v1/me/watches` | 내 와드 목록 |
| GET | `/api/v1/me/notifications` | 내 알림 목록 |
| POST | `/api/v1/me/notifications/mark-read` | 알림 일괄 읽음 |
| GET | `/api/v1/tags` | 태그 검색 |
| POST | `/api/v1/tags/{id}/follow` | 태그 팔로우 |
| DELETE | `/api/v1/tags/{id}/follow` | 태그 언팔로우 |
| GET | `/api/v1/search?q=...` | 질문/태그/에러 검색 |
| GET | `/api/v1/recommendations/questions?source=tags` | 태그 기반 추천 |
| GET | `/api/v1/dashboard` | 대시보드 집계 |

## 인증 (확정 필요 — Phase 1 착수 시 결정)

원본 기획 문서들에는 인증 방식이 구체적으로 명시되어 있지 않다. 계정(P0) 구현 시 아래 중 하나를 확정하고 이 문서에 기록한다.

- Spring Security + 세션 기반 인증
- Spring Security + JWT (Access/Refresh Token 분리)

공통 원칙:
- 요청에서 `authorId`/`userId`를 클라이언트가 직접 지정하지 않는다. 인증 Principal(SecurityContext)에서 사용자 식별자를 얻는다.
- 관리자/모더레이터 API가 추가되면 Role과 세부 권한을 분리한다.

## 페이지네이션

목록형 API(`GET /api/v1/questions`, `/api/v1/search`, `/api/v1/me/notifications` 등)는 페이지 번호 기반보다 **cursor pagination**을 권장한다. 예: `created_at` 또는 `last_activity_at` + `id`를 커서로 사용하면 데이터가 계속 추가되는 상황에서도 중복/누락을 줄일 수 있다.

## 입력 검증 공통 원칙

- Markdown 본문은 렌더링 시 XSS Sanitization을 적용한다.
- 질문/답변 작성은 Redis 기반 레이트 리밋 적용을 검토한다 (스팸 방지).
- 첨부파일(MVP 이후)은 Object Storage에 저장하고 API/DB에는 metadata만 보관한다.
