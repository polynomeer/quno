# 과제

`MemberRepositoryTests`가 실패합니다. 회원의 표시 이름(닉네임이 있으면 닉네임, 없으면 이름)을 페이지 단위로 조회하는 `MemberRepository.findDisplayNames(Pageable)`를 쓰려는데, 리포지토리 빈을 만드는 단계에서 예외가 납니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전(따라서 Spring Data JPA·Hibernate 버전)은 4.0.8 기준으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `findDisplayNames`의 시그니처와 결과는 그대로여야 합니다. 특히 `totalElements`는 중복을 제거한 표시 이름의 개수입니다(활성 회원 수가 아닙니다).
