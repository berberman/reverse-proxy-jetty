import org.eclipse.jetty.client.api.Response
import org.eclipse.jetty.proxy.AsyncProxyServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.log.StdErrLog
import javax.servlet.http.HttpServletRequest

fun Array<String>.main() {
	val rawPort = firstOrNull { it.startsWith("-port=") }
	val rawTarget = firstOrNull { it.startsWith("-target=") }
	var portResult: String
	var targetResult = "http://idea.lanyus.com:80"
	var port = 2333
	rawPort?.let {
		portResult = it.substring("-port=".length, it.length)
		try {
			port = portResult.toInt()
		} catch (e: NumberFormatException) {
			throw IllegalArgumentException("端口得是个整数")
		}
	}
	rawTarget?.let {
		targetResult = it.substring("-target=".length, it.length)
	}
	server(port, targetResult).apply {
		start()
		logger.info("服务器开启 端口:$port 目标地址:$targetResult")
		join()
	}
}

val logger: StdErrLog = StdErrLog.getLogger(Server::class.java)

fun server(port: Int, target: String) = Server(port).apply {
	handler = ServletHandler().apply {
		addServletWithMapping(ServletHolder(object : AsyncProxyServlet() {
			override fun rewriteTarget(clientRequest: HttpServletRequest?) =
					target

			override fun newWriteListener(request: HttpServletRequest?, proxyResponse: Response?) =
					object : StreamWriter(request, proxyResponse) {
						init {
							request?.let {
								logger.info("请求: ${request.remoteAddr} : ${it.method}--> (${it.requestURI})")
							}
						}
					}
		}).apply {
			setInitParameter("maxThreads", "100")
		}, "/*")
	}
}
