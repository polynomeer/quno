# Phase 91 과제 목록 (사전 등록 후보)

[후보 목록](../../docs/product/phase-91-task-candidates.md)에서 고른 30개와 구현 상태다. **이 파일이 커밋된 시점의 git 해시를 사전 등록으로 삼는다**(시드 큐레이션 전). 이후 과제를 바꾸면 사유를 아래 변경 이력에 남긴다.

- 층: A 최신(보고일 ≥ 2026-07-19), B 롱테일·환경 의존, C 통제군(흔한 에러, 합성).
- 형식: R 재현형, D Docker 필요 재현형, L 로그 재생형(루브릭 채점, R/D와 따로 집계).
- 고정 버전: 과제가 요구하는 Boot 버전(조직 정책으로 올리지 못함). L은 환경 스냅샷에 있다.
- **학습 시점 주의**: B층 보고일은 2025-08~2026-02로 일부 에이전트의 학습 데이터에 이미 있을 수 있다. 실험 시작 시 에이전트의 학습 시점을 기록하고, 그 이전 보고 과제는 "최신" 효과를 해석할 때 구분한다. A층 10개는 모두 2026-07-19 이후다.

| ID | 층 | 형식 | 출처 | 보고일 | 고정 버전 | 증상 |
|---|---|---|---|---|---|---|
| A01 | A | R | spring-boot#51872† | 2026-09-28 | 4.0.8 | DevTools + 직접 DataSource → NoClassDefFoundError |
| A02 | A | R | spring-boot#52249† | 2026-10-15 | 4.0.8 | 빈 issuer-uri → 모든 JWT 거부 |
| A03 | A | R | spring-framework#37309 | 2026-09-20 | 4.0.8 | @CacheEvict가 Flux를 첫 요소로 자름 |
| A04 | A | R | spring-framework#37264 | 2026-09-10 | 4.0.8 | WebFlux 멀티파트 빈 파일 유실 (Kotlin) |
| A05 | A | R | spring-data-jpa#4359 | 2026-09-19 | 4.0.8 | HQL % 연산자 미지원 |
| A06 | A | R | spring-data-jpa#4341 | 2026-09-03 | 4.0.8 | DISTINCT 파생 count 쿼리 손상 |
| A07 | A | R | spring-boot#52121† | 2026-10-09 | 4.0.8 | Mongo 비밀번호 공백이 +로 인코딩 |
| A08 | A | R | spring-boot#52207† | 2026-10-14 | 4.0.8 | 이스케이프된 #·! 키 유실 |
| A09 | A | R | spring-framework#37427 | 2026-10-08 | 4.0.8 | 긴 Duration 타임아웃 int 오버플로 |
| A10 | A | R | spring-boot#51447 | 2026-08-25 | 4.0.8 | OAuth2ClientProperties 빈 없음 |
| B01 | B | D | spring-boot#49012 | 2026-01-28 | 4.0.2 | Flyway Unsupported Database PostgreSQL 18.1 (Docker) |
| B02 | B | R | spring-boot#49311 | 2026-02-24 | 4.0.3 | Tomcat max-part-count 413 |
| B03 | B | R | spring-boot#48880 | 2026-01-19 | 4.0.1 | Kafka 트랜잭션 @Transactional 무시 |
| B04 | B | R | spring-boot#48653 | 2025-12-31 | 4.0.1 | RANDOM_PORT 관리 포트 PortInUse |
| B05 | B | R | spring-boot#48240 | 2025-11-22 | 4.0.8 | Spring Data JDBC + AOT JdbcDialect (편차) |
| B06 | B | R | spring-boot#48254 | 2025-11-23 | 4.0.0 | WAR 배포 Environment (MockServletContext 하네스, 편차) |
| B07 | B | L | testcontainers-java#11212 | 2025-11-11 | - | Docker 29 최소 API 1.44 |
| B08 | B | L | spring-boot#46665 | 2025-08-04 | - | Apple Silicon imagePlatform buildpack |
| B09 | B | L | gradle#35204 | 2025-10-02 | - | JDK 25 + Kotlin DSL 타깃 불일치 경고 |
| B10 | B | L | spring-boot#48961 | 2026-01-24 | - | Windows .cmd 자격 증명 헬퍼 |
| C01 | C | R | Boot 2.6 릴리스 노트 | - | 4.0.8 | BeanCurrentlyInCreationException |
| C02 | C | R | Kotlin all-open 문서 | - | 4.0.8 | final @Configuration 프록시 |
| C03 | C | R | Kotlin no-arg 문서 | - | 4.0.8 | JPA 엔티티 기본 생성자 없음 |
| C04 | C | R | Gradle 호환성 표 | - | 4.0.8 | Unsupported class file major version (JaCoCo 변형) |
| C05 | C | R | Boot how-to | - | 4.0.8 | PortInUseException 8080 |
| C06 | C | R | Boot how-to | - | 4.0.8 | DataSource url 미설정 |
| C07 | C | R | Spring 문서 | - | 4.0.8 | NoUniqueBeanDefinitionException |
| C08 | C | R | Hibernate | - | 4.0.8 | LazyInitializationException |
| C09 | C | R | Spring 문서 | - | 4.0.8 | @Transactional 자기 호출 |
| C10 | C | R | Kotlin 문서 | - | 4.0.8 | lateinit 미초기화 |

`†` = 수정 PR 번호. 검증: 재현형 26개는 `scripts/verify-repro.sh`(수정 전 증상 재현)와 `scripts/verify-solution.sh`(정답 패치 통과)를 순차 실행해 통과했고, L 4개는 형식 확인만 통과한다(내용은 루브릭 채점).

## 변경 이력

| 날짜 | 변경 | 이유 |
|---|---|---|
| 2026-10-20 | 최초 작성 (30개) | 시드 큐레이션 전 사전 등록 |
