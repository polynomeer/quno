# 과제

`CallRepositoryTests`가 실패합니다. 통화 기록 중 통화 시간이 짝수 초인 것만 조회하는 `CallRepository.findEvenDurations()`를 쓰려는데, 쿼리를 실행하기도 전에 리포지토리 빈을 만드는 단계에서 예외가 납니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전(따라서 Spring Data JPA·Hibernate 버전)은 4.0.8 기준으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `findEvenDurations()`의 시그니처와 결과(통화 시간이 짝수인 기록만, `id` 오름차순)는 그대로여야 합니다.
