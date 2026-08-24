# 커밋 규칙 (Commit Convention)

이 프로젝트는 언어/프레임워크에 무관하게 적용 가능한 범용 규칙인 [Conventional Commits](https://www.conventionalcommits.org/ko/v1.0.0/)를 따른다.

## 형식

```
<type>(<scope>): <subject>

<body (선택)>

<footer (선택)>
```

- **type**: 변경의 종류 (아래 표 참고)
- **scope**: 변경 범위 (예: `docs`, `api`, `auth`, `question-card` 등). 범위가 불명확하면 생략 가능.
- **subject**: 무엇을 했는지 한 줄 요약. 명령형·현재형("추가", "수정" 등 동사로 끝나는 간결한 문장), 마침표 없음, 50자 내외 권장.
- **body**: 왜 이 변경이 필요한지, 무엇이 바뀌었는지 설명 (자명하지 않은 경우에만 작성).
- **footer**: Breaking Change, 이슈 참조 등 (`BREAKING CHANGE: ...`, `Closes #12` 등).

## Type 목록

| type | 의미 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 추가/수정 (기획서, README, 주석 등 코드 동작에 영향 없는 변경) |
| `style` | 코드 포맷팅, 세미콜론 등 동작에 영향 없는 스타일 변경 |
| `refactor` | 기능 변경 없는 코드 구조 개선 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가/수정 |
| `build` | 빌드 시스템, 패키지 매니저, 의존성 관련 변경 |
| `ci` | CI 설정 변경 |
| `chore` | 위 항목에 속하지 않는 잡무성 변경 (환경 설정, 도구 설정 등) |
| `revert` | 이전 커밋 되돌리기 |

## 원칙

1. **하나의 커밋 = 하나의 작업 단위.** 여러 목적(기능 추가 + 문서 수정 + 설정 변경)을 한 커밋에 섞지 않는다.
2. **작업이 끝나는 즉시 커밋한다.** 여러 작업을 모아서 나중에 한 번에 커밋하지 않는다.
3. **커밋 메시지만 보고도 무엇이 왜 바뀌었는지 알 수 있게 작성한다.**
4. Breaking change가 있으면 footer에 `BREAKING CHANGE:`로 명시한다.

## 예시

```
docs(planning): Quno 통합 기획서 추가
chore(env): Claude Code 환경 및 커밋 규칙 설정
feat(question-card): 질문 리비전 이력 조회 API 추가
fix(auth): 토큰 만료 시 무한 재시도되는 버그 수정
```
