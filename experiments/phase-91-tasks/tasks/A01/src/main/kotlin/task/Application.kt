package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Proxy
import javax.sql.DataSource
import kotlin.system.exitProcess

@SpringBootApplication
class Application {
	/** 기동 검증용: 컨텍스트가 정상 기동하면 표시를 남기고 종료한다. */
	@Bean
	fun startupCheck() =
		ApplicationRunner {
			println("STARTUP_OK")
			exitProcess(0)
		}
}

@Configuration(proxyBeanMethods = false)
class DataSourceConfig {
	/** 사내 공통 라이브러리가 제공하는 것으로 가정한 단일 DataSource. 이 과제에서는 연결하지 않는다. */
	@Bean
	fun dataSource(): DataSource =
		Proxy.newProxyInstance(
			DataSource::class.java.classLoader,
			arrayOf(DataSource::class.java),
		) { _, method, _ -> error("not used in this task: ${method.name}") } as DataSource
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
