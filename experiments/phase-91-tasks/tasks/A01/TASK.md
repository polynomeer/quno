# 과제

`./gradlew bootRun`으로 애플리케이션을 실행하면 기동에 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew bootRun`이 기동에 성공해 `STARTUP_OK`를 출력).
- 제약: Spring Boot 버전은 4.0.8로 고정되어 있습니다(조직 정책). 올리지 마세요.
- `Application.kt`의 `startupCheck` 빈과 `DataSourceConfig`의 동작(직접 정의한 단일 `DataSource`)은 바꾸지 마세요.
