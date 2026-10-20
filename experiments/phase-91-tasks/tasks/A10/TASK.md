# 과제

애플리케이션이 시작되지 않습니다. `OAuth2Config`에서 `OAuth2ClientProperties`를 주입받아 `ClientRegistrationRepository`를 직접 정의했는데, `application.properties`에는 `spring.security.oauth2.client.registration.github.*`가 분명히 설정되어 있습니다. `OAuth2ConfigTests`가 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전은 4.0.8로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `ClientRegistrationRepository`는 계속 직접 정의한 지연 생성 빈이어야 하고(`OAuth2Config`), 등록 정보는 `application.properties`의 설정에서 가져와야 합니다. 설정 값을 코드에 옮겨 적지 마세요.
