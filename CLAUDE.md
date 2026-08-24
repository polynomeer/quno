# Quno

Quno는 개발자를 위한 Q&A 플랫폼으로, 질문을 "죽은 게시물"이 아니라 리비전·클러스터링·재활성화가 가능한 **Living Question Card**로 다루는 것을 핵심 철학으로 한다. 상세 기획은 [docs/](docs/) 디렉터리를 참고한다.

- `docs/Quno 서비스 통합 기획서 — Living Question Knowledge Platform.md`: 통합 기획서 (제품 철학, 문제 정의, 핵심 개념)
- `docs/*.docx`: 도메인 설계, 백엔드/시스템 설계 등 세부 기획 문서
- `docs/quno-event-storming.png`: 이벤트 스토밍 다이어그램

현재는 기획 단계이며 코드베이스는 아직 없다. 코드가 추가되면 이 파일에 빌드/테스트/실행 방법과 아키텍처 개요를 갱신한다.

## 커밋 규칙

이 저장소는 [Conventional Commits](https://www.conventionalcommits.org/) 규칙을 따른다. 상세 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고한다. 요약:

- 형식: `<type>(<scope>): <subject>` — 예: `docs(planning): Quno 통합 기획서 추가`
- 하나의 커밋은 하나의 논리적 작업 단위만 포함한다 (기능 하나, 문서 하나, 설정 변경 하나 등을 섞지 않는다).
- 제목은 명령형·현재형으로, 마침표 없이 작성한다.

**작업 진행 방식**: 작업을 완료할 때마다 (기능 단위, 문서 단위, 설정 변경 단위 등) 바로 커밋한다. 여러 작업을 모아서 한 번에 커밋하지 않는다.
