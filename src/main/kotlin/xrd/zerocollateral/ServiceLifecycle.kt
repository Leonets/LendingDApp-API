package xrd.zerocollateral

import mu.KLoggable
import mu.KLogger
import kotlin.properties.Delegates

object ServiceLifecycle: KLoggable {

    enum class ServiceStatus {
        CREATED, INITIALIZING, INITIALIZED, STARTING, STARTED, STOPPING, STOPPED
    }

    override val logger: KLogger = logger()

    var targetStatus: ServiceStatus by Delegates.observable(ServiceStatus.CREATED) { _, old, target ->
        logger.info { "Target lifecycle status $target has been requested" }

        ServiceStatus.values().filter { it.ordinal > old.ordinal && it.ordinal <= target.ordinal }.forEach {
            propagate(it)
        }

        currentStatus = target
    }

    var currentStatus: ServiceStatus = ServiceStatus.CREATED

    private val callbacks: Map<ServiceStatus, MutableList<() -> Unit>> = mapOf(
        ServiceStatus.CREATED to mutableListOf(),
        ServiceStatus.INITIALIZING to mutableListOf(),
        ServiceStatus.INITIALIZED to mutableListOf(),
        ServiceStatus.STARTING to mutableListOf(),
        ServiceStatus.STARTED to mutableListOf(),
        ServiceStatus.STOPPING to mutableListOf(),
        ServiceStatus.STOPPED to mutableListOf()
    )

    infix fun MutableList<() -> Unit>.triggers(callback: () -> Unit) {
        this.add(callback)
    }

    operator fun get(status: ServiceStatus): MutableList<() -> Unit> = callbacks[status]!!

    private fun propagate(target: ServiceStatus) {
        val hooks = callbacks[target]!!
        val totalHooks = hooks.size

        logger.info { "Executing $target status hooks [$totalHooks]" }

        hooks.forEachIndexed { idx, hook ->
            val reportedHookIdx = idx + 1

            try {
                hook()
                logger.info { "$targetStatus lifecycle Hook ($reportedHookIdx / $totalHooks) has been executed" }
            } catch(e: Exception) {
                logger.warn(e) { "$targetStatus lifecycle Hook ($reportedHookIdx / $totalHooks) execution has failed" }
                throw e
            }
        }

        logger.info { "Target lifecycle status $target has been reached" }
    }
}
