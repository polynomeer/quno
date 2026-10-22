# 과제

`MemberRepositoryTests`가 실패합니다. 개발 DB를 PostgreSQL 18.1로 올린 뒤부터 Flyway 마이그레이션을 쓰는 이 애플리케이션이 기동하지 못합니다. 접속 정보에는 문제가 없습니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다. `CHECK`는 Docker로 `postgres:18.1` 컨테이너를 띄우고 `./gradlew test`를 실행합니다(Docker 필요).
- 제약:
  - Spring Boot 버전은 4.0.2로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드와 `CHECK` 스크립트, 마이그레이션 SQL, 사용하는 PostgreSQL 이미지 버전(18.1)은 바꾸지 마세요.
  - Flyway를 끄거나 다른 마이그레이션 도구로 바꾸지 마세요.
