package xrd.zerocollateral.http4k

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.slf4j.LoggerFactory

object CatchAndLogFailuresFilter {

    private val log = LoggerFactory.getLogger(CatchAndLogFailuresFilter::class.java)

    operator fun invoke(errorStatus: Status = Status.INTERNAL_SERVER_ERROR): Filter = Filter { next ->
        {
            try {
                next(it)
            } catch (e: Exception) {
                log.error("Errors occurred while processing request: {}", e.message, e)
                Response(errorStatus)
            }
        }
    }
}
