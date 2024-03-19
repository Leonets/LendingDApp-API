package xrd.zerocollateral.servers

import org.http4k.core.*
import org.http4k.format.Jackson.auto

object InfrastructureHandlers {

    class HealthCheckHandler(private val serviceName: String, private val version: String, private val buildTime: String) : HttpHandler {

        data class HealthCheck(val status: String, val serviceName: String, val version: String, val buildTime: String)

        private val checkLens = Body.auto<HealthCheck>().toLens()

        override fun invoke(request: Request): Response {
            return checkLens.inject(HealthCheck("OK", serviceName, version, buildTime), Response(Status.OK))
        }

    }
}