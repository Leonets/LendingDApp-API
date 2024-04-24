package xrd.zerocollateral

import ChallengeStore
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import xrd.zerocollateral.ServiceLifecycle.ServiceStatus.STARTED
import xrd.zerocollateral.ServiceLifecycle.ServiceStatus.STOPPED
import xrd.zerocollateral.ServiceLifecycle.triggers
import xrd.zerocollateral.http4k.CatchAndLogFailuresFilter
import xrd.zerocollateral.servers.ZeroCollateralHandlers.DatabaseHandler
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging.logger
import org.http4k.core.*
import org.http4k.filter.*
import org.http4k.lens.*
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Jetty
import org.http4k.server.asServer
import xrd.zerocollateral.servers.InfrastructureHandlers
import xrd.zerocollateral.servers.ZeroCollateralHandlers
import xrd.zerocollateral.servers.StaticConfigHandler
import java.security.Security


fun main() {

    Security.setProperty("networkaddress.cache.ttl", "60")

//    val flyway = with(Flyway.configure()) {
//        locations("/db/mysql/migration")
//        dataSource("jdbc:mysql://${Config.dbEndpoint}/${Config.dbName}", Config.dbUsername, Config.dbPass)
//        load()
//    }

    val logger = logger("xrd.zerocollateral")

    val maxActiveDbConnections: Int = Config.dbMaxActive ?: 5

    logger.info { "Assigned $maxActiveDbConnections max active DB connections" }

    val cacheConnectionPoolConfiguration = ConnectionPoolConfiguration(
        coroutineDispatcher = Dispatchers.IO,
        maxActiveConnections = Config.dbMaxActive ?: 5
    )

    val cacheConnectionPool = ConnectionPool(
        MySQLConnectionFactory(
            Configuration(
                username = Config.dbUsername,
                password = Config.dbPass,
                host = Config.dbEndpoint.host.value,
                port = Config.dbEndpoint.port?.value ?: 3306,
                database = Config.dbName,
            )
        ), cacheConnectionPoolConfiguration)

    val trapeziteDatabaseHandler = DatabaseHandler(cacheConnectionPool)

    val contexts = RequestContexts()

    val infraBase = "/infra"
    val infraRoutes = routes(
        "$infraBase/health" bind Method.GET to InfrastructureHandlers.HealthCheckHandler(
            Constants.SERVICE_NAME,
            Config.serviceVersion,
            Config.buildTime
        ),
    )

    val docsRoutes = routes(
        "docs" bind Method.GET to {
            Response(Status.FOUND).header("Location", "/docs/index.html")
        },
        "/docs" bind Method.GET to static(ResourceLoader.Classpath("/static/swagger")),
        "/contracts/openapi.yml" bind Method.GET to {
            val bodyLens = Body.string(ContentType.APPLICATION_YAML).toLens()
            val spec = object {}.javaClass.getResource("/contracts/openapi.yml")?.readText()?.replace("<version>",
                Config.serviceVersion
            ) ?: ""
            Response(Status.OK).with(bodyLens of spec)
        }
    )

    val basicAuthFilter = ServerFilters.BasicAuth("Returns Images", Credentials(Config.serviceUser, Config.servicePass))

    val path = Path.string()

    val apiBase = "/api"
    val apiRoutes = routes(
        "$apiBase/config/static" bind Method.GET to StaticConfigHandler,
        "$apiBase/coins" bind Method.GET to basicAuthFilter(ZeroCollateralHandlers.lookupCoinDetails(trapeziteDatabaseHandler, Query.string().optional("allowedTuser"))),
        "$apiBase/markets" bind Method.GET to basicAuthFilter(ZeroCollateralHandlers.lookupMarkets(trapeziteDatabaseHandler, Query.string().optional("coin"), Query.string().optional("allowedTuser"))),
        "$apiBase/collateral" bind Method.GET to basicAuthFilter(ZeroCollateralHandlers.lookupCollateral(trapeziteDatabaseHandler, Query.string().optional("componentAddress")))
    )

    val rolaRoutes = routes(
        "$apiBase/create-challenge" bind Method.POST to ChallengeStore.createChallenge(),
        "$apiBase/verify" bind Method.POST to ChallengeStore.verifyChallenge()
    )

    val feedbackRoutes = routes(
        "$apiBase/submit_feedback" bind Method.POST to ZeroCollateralHandlers.feedback()
    )

    val scrollPath = "$apiBase/catalog/datasets"

//    val apiCatalogRoutes = routes(
//            "$apiBase/catalog/search" bind Method.POST to basicAuthFilter(ReturnsImagesDynamoHandlers.searchReturnsInformation(scrollPath, storedReturnImageCatalog, Config.dynamoScrollLimit)),
//            "$apiBase/catalog/datasets" bind Method.GET to basicAuthFilter(ReturnsImagesDynamoHandlers.scrollReturnsInformation(scrollPath, storedReturnImageCatalog, Config.dynamoScrollLimit, Query.string().optional("indexName"), Query.boolean().optional("onlyRef"))))

    val service = routes(infraRoutes, docsRoutes, apiRoutes, rolaRoutes, feedbackRoutes)
    val serviceRoutes = when {
        Config.debugEnabled -> DebuggingFilters.PrintRequestAndResponse().then(service)
        else -> service
    }

    val serverFilters = ServerFilters.InitialiseRequestContext(contexts).then(CatchAndLogFailuresFilter())
    val conditionedFilters = when (Config.developmentModeEnabled) {
        true -> {
            val corsPolicy = CorsPolicy(
                originPolicy = OriginPolicy.AllowAll(),
                headers = listOf("*"),
                methods = Method.values().toList(),
                false
            )
            serverFilters.then(ServerFilters.Cors(corsPolicy))
        }
        false -> serverFilters
    }

    val server = conditionedFilters.then(serviceRoutes).asServer(Jetty(port = Config.serverPort))

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            logger.info("Executing graceful shutdown sequence...")

            if(ServiceLifecycle.currentStatus == STARTED) {
                ServiceLifecycle.targetStatus = STOPPED
            } else {
                logger.warn { "Service did not start successfully (Current status: ${ServiceLifecycle.currentStatus}). Shutdown sequence WILL NOT be executed" }
            }

            logger.info("Shutdown sequence completed")
        }
    })

//    ServiceLifecycle[STARTED] triggers flyway::migrate

    ServiceLifecycle[STARTED] triggers {
//        enabledEntryPoints.forEach {
//            it.value.start()
//            logger.info("${it.key} started")
//        }
    }

    ServiceLifecycle[STARTED] triggers {
        server.start()
        logger.info("Server started on {} (v. {})", server.port(), Config.serviceVersion)
    }

    ServiceLifecycle[STOPPED] triggers {
//        enabledEntryPoints.forEach {
//            logger.info("Stopping ${it.key}...")
//            it.value.close()
//            logger.info("${it.key} stopped")
//        }
    }

    ServiceLifecycle[STOPPED] triggers {
        cacheConnectionPool.disconnect().get()
        logger.info { "DB Pool disconnected" }
    }

    ServiceLifecycle[STOPPED] triggers {
        server.stop()
        logger.info("Server stopped on {} (v. {})", server.port(), Config.serviceVersion)
    }

    ServiceLifecycle.targetStatus = STARTED
}