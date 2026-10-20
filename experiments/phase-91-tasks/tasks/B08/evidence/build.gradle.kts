tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    builder = "paketobuildpacks/builder-noble-java-tiny@sha256:7d2fdc07afb6d54cd5dd61fb466c966dd9f3dc1dd133ffd3714ddf086000b773"
    imageName = "demo:v1"
    environment = mapOf("BP_OPENTELEMETRY_ENABLED" to "true")
    imagePlatform = "linux/amd64"
    buildpacks = listOf("paketobuildpacks/java", "paketobuildpacks/opentelemetry:2")
}
