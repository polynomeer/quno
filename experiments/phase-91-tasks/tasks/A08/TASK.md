# 과제

`routes.properties`에 적어 둔 채널 매핑 중 이름이 `#`이나 `!`로 시작하는 것들을 `ChannelRouter`가 찾지 못합니다(`null`). 파일에는 분명히 적혀 있고 철자도 맞습니다. `ChannelRouterTests`가 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전은 4.0.8로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `routes.properties`의 키 이름(`#general`, `#alerts`, `!oncall`, `dm`)과 `ChannelRouter`의 조회 방식(`Environment`로 채널 이름 그대로 조회)은 유지되어야 합니다.
