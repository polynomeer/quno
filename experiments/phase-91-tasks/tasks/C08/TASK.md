# 과제

주문 상세 화면용으로 주문과 품목을 함께 돌려주는 `OrderService.findDetail`을 만들었습니다. `OrderServiceTests`를 돌리면 품목에 접근하는 순간 예외가 나며 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전은 4.0.8로 고정되어 있습니다. 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `spring.jpa.open-in-view=false` 설정(조직 정책)과 `Order.items`의 지연 로딩(`LAZY`) 설정은 그대로 두세요.
  - `findDetail`의 반환 타입(`Order`)은 바꾸지 마세요.
