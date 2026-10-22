# 과제

`./CHECK`가 실패합니다. 테스트는 H2로 돌아가지만, 배포 파이프라인이 빌드 단계에서 실행하는 `processAot`(AOT 처리)가 실패합니다. 개발 중 `bootRun`으로는 문제없이 뜹니다. 데이터소스 접속 정보(`spring.datasource.*`)는 운영 환경에서 환경 변수로 주입하므로 빌드 단계에는 없고, 빌드 서버에는 DB가 없습니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test processAot` 통과).
- 제약:
  - Spring Boot 버전은 4.0.8로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드와 테스트 리소스는 바꾸지 마세요.
  - 빌드 단계에서 DB에 접속하게 만들거나, 빌드 시점에 접속 정보를 `application.properties`에 넣지 마세요(운영 DB는 PostgreSQL이고 접속 정보는 배포 때 주입합니다).
  - Spring Data JDBC 리포지토리(`NoteRepository`)는 그대로 유지되어야 합니다.
