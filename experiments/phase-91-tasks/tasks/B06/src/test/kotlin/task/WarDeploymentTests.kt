package task

import jakarta.servlet.Filter
import jakarta.servlet.FilterRegistration
import jakarta.servlet.Servlet
import jakarta.servlet.ServletRegistration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext

/** MockServletContext는 필터/서블릿 등록을 지원하지 않아서, 등록 요청을 조용히 받아 주는 컨텍스트를 쓴다. */
class AcceptingServletContext : MockServletContext() {
	override fun addFilter(
		filterName: String,
		filter: Filter,
	): FilterRegistration.Dynamic = Mockito.mock(FilterRegistration.Dynamic::class.java)

	override fun addServlet(
		servletName: String,
		servlet: Servlet,
	): ServletRegistration.Dynamic = Mockito.mock(ServletRegistration.Dynamic::class.java)
}

/** 외부 Tomcat이 하는 일(서블릿 컨텍스트에 context.xml 파라미터를 넣고 초기화 진입점을 호출)을 흉내 낸다. */
class TestableServletInitializer : ServletInitializer() {
	fun start(servletContext: AcceptingServletContext): WebApplicationContext = createRootApplicationContext(servletContext)!!
}

class WarDeploymentTests {
	@Test
	fun `context xml 파라미터로 준 설정이 애플리케이션에 적용된다`() {
		val servletContext = AcceptingServletContext()
		servletContext.addInitParameter("storage.root", "/var/data/files")
		servletContext.addInitParameter("storage.max-files", "250")

		val context = TestableServletInitializer().start(servletContext)
		try {
			val settings = context.getBean(StorageSettings::class.java)
			assertThat(settings.root).isEqualTo("/var/data/files")
			assertThat(settings.maxFiles).isEqualTo(250)
		} finally {
			(context as AutoCloseable).close()
		}
	}
}
