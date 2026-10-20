# 과제

첨부 파일 업로드 API(`POST /uploads`, `multipart/form-data`)에서, 빈 파일(0바이트)을 올릴 때 요청 안의 파트 순서에 따라 결과가 달라집니다. `UploadControllerTests`의 "빈 파일이 다른 파트보다 앞에 있어도 수신된다" 테스트가 실패합니다. 같은 빈 파일인데 파일 파트가 맨 뒤에 있으면 `file`이 정상적으로 들어오지만, 뒤에 다른 파트가 더 있으면 컨트롤러에는 `null`로 들어옵니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전(따라서 Spring Framework 버전)은 4.0.8 기준으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - 업로드 API의 경로, 요청 형식(`multipart/form-data`의 `file`·`action`·`reason` 파트), 응답 본문(`received: <파일명>` / `no file received`)은 그대로 유지되어야 합니다. 내용이 있는 파일의 동작도 지금과 같아야 합니다.
