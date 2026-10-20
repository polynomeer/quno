# 과제

`ItemServiceTests`가 실패합니다. `saveAll`이 항목을 모두 저장하는데도 호출한 쪽은 일부만 받습니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전(따라서 Spring Framework 버전)은 4.0.8 기준으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `saveAll`이 호출되면 `items` 캐시가 비워지는 동작은 유지되어야 합니다.
