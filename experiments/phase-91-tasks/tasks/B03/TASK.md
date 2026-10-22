# 과제

`OrderEventPublisherTests`가 실패합니다. 주문 이벤트를 Kafka 트랜잭션 안에서 발행하려고 `OrderEventPublisher`의 메서드에 `@Transactional`을 붙여 두었는데, 이벤트를 보내는 시점에 트랜잭션이 없다는 오류가 납니다. 같은 구성의 애플리케이션이 이전 메이저 버전에서는 잘 동작했습니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과). 테스트는 브로커 없이 실행됩니다.
- 제약:
  - Spring Boot 버전은 4.0.1로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `OrderEventPublisher`가 `KafkaTemplate`과 `@Transactional`로 이벤트를 발행하는 방식은 유지되어야 합니다(`spring.kafka.producer.transaction-id-prefix` 설정 포함).
