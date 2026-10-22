# 과제

회원 등급 할인을 적용해 견적을 내는 `PriceQuoteService`를 만들었는데, `PriceQuoteServiceTests`를 돌리면 견적을 내는 순간 예외가 나며 테스트가 실패합니다. 컨텍스트는 정상적으로 뜹니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전과 Kotlin 버전(2.2.21)은 고정되어 있습니다. 올리거나 내리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - 할인율 계산은 `DiscountPolicy`가 맡는 구조를 유지하세요.
