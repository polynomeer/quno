# 과제

`WarDeploymentTests`가 실패합니다. 이 애플리케이션은 WAR로 만들어 외부 Tomcat에 배포하고, 서버마다 다른 설정(`storage.root` 등)은 Tomcat의 `context.xml`에 `<Parameter>`로 넣어 둡니다. `WarDeploymentTests`는 그 방식을 흉내 내어(서블릿 컨텍스트의 초기화 파라미터로 설정을 주고 `ServletInitializer`로 기동) 설정이 적용되는지 확인합니다. 이전 메이저 버전에서는 같은 방식이 잘 동작했는데 지금은 설정이 보이지 않아 기동에 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전은 4.0.0으로 고정되어 있습니다(조직 정책). 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - 설정은 계속 `context.xml`의 `<Parameter>`(서블릿 컨텍스트 초기화 파라미터)로 주입되어야 합니다. `application.properties`에 값을 넣거나 `storage.root`에 기본값을 두는 방식으로 우회하지 마세요.
  - WAR 배포 방식(`SpringBootServletInitializer`)은 유지되어야 합니다.
