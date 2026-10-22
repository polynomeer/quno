package task

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.stereotype.Component

@SpringBootApplication
class Application

/** 업로드 파일을 저장할 위치. 서버마다 다르므로 Tomcat의 context.xml `<Parameter>`로 지정한다. */
@Component
class StorageSettings(
	@Value("\${storage.root}") val root: String,
	@Value("\${storage.max-files:100}") val maxFiles: Int,
)

/** WAR로 배포할 때 외부 Tomcat이 호출하는 진입점. */
open class ServletInitializer : SpringBootServletInitializer() {
	override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder = builder.sources(Application::class.java)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
