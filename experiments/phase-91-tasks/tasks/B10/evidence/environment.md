# 환경 스냅샷 (실패한 개발 머신)

| 항목 | 값 |
|---|---|
| OS | Windows 11 |
| Docker | Docker Desktop |
| Spring Boot Gradle 플러그인 | 4.0.1 |
| 추가 설치 | Google Cloud SDK (`docker-credential-gcloud`는 `docker-credential-gcloud.cmd`로 제공됨) |
| 레지스트리 | europe-west3-docker.pkg.dev (Google Artifact Registry) |

`%USERPROFILE%\.docker\config.json`:

```json
{
  "auths": {},
  "credsStore": "desktop",
  "credHelpers": {
    "europe-west3-docker.pkg.dev": "gcloud"
  }
}
```
