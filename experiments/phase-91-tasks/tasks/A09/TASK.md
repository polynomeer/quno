# 과제

배치 환경에서 파트너 API 연결 대기를 사실상 제한하지 않으려고 `partner-api.connect-timeout=30d`로 설정했더니 애플리케이션이 시작되지 않습니다. 같은 설정을 `7d`로 바꾸면 잘 뜹니다. `PartnerApiConfigTests`가 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전(따라서 Spring Framework 버전)은 4.0.8 기준으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - 설정값 `30d`는 그대로 두고, 그 값이 "매우 긴 연결 타임아웃"이라는 의미를 유지해야 합니다(`0`이나 짧은 값으로 바꾸지 마세요). `ReactorClientHttpRequestFactory`는 계속 사용합니다.
