package xrd.zerocollateral

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.MapEnvironment
import org.http4k.lens.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Config {

    private const val propsSeparator = "*separator*"

    private val serviceBaseUrlKey = EnvironmentKey.string().required("SERVICE_BASE_URL")

    private val serverPortKey = EnvironmentKey.int().required("SERVER_PORT")

    private val serviceUserKey = EnvironmentKey.string().required("SERVICE_USER")

    private val servicePassKey = EnvironmentKey.string().required("SERVICE_PASS")

    private val debugEnabledKey = EnvironmentKey.boolean().required("DEBUG_ENABLED")

    private val targetEnvKey = EnvironmentKey.string().required("TARGET_ENV")

    private val snsEndpointOverrideKey = EnvironmentKey.string().optional("SNS_ENDPOINT_OVERRIDE")

    private val serviceVersionKey = EnvironmentKey.string().optional("SERVICE_VERSION")

    private val serviceBuildTimeKey = EnvironmentKey.string().optional("BUILD_TIME")

    private val dbUsernameKey = EnvironmentKey.string().required("DB_USER")

    private val dbPassKey = EnvironmentKey.string().required("DB_PASS")

    private val dbNameKey = EnvironmentKey.string().required("DB_NAME")

    private val dbEndpointKey = EnvironmentKey.authority().required("DB_HOST_AND_PORT")

    private val dbMaxActiveKey = EnvironmentKey.int().optional("DB_MAX_ACTIVE_CONNECTIONS")

    private val dbConnectionTTLMillisKey = EnvironmentKey.long().required("DB_CONNECTION_TTL_MILLIS")

    private val dbConnectionIdleTimeoutMillisKey = EnvironmentKey.long().required("DB_CONNECTION_IDLE_MILLIS")

    private val dbConnectionCreateTimeoutMillisKey = EnvironmentKey.long().required("DB_CONNECTION_CREATE_TIMEOUT_MILLIS")

    private val dbQueryTimeoutMillisKey = EnvironmentKey.long().required("DB_QUERY_TIMEOUT_MILLIS")

    private val dbEncryptionEnabledKey = EnvironmentKey.boolean().required("DB_ENCRYPTION_ENABLED")

    private val developmentModeEnabledKey = EnvironmentKey.boolean().required("DEVELOPMENT_MODE")

    private val proxyUsernameKey = EnvironmentKey.string().optional("PROXY_USER")

    private val proxyPassKey = EnvironmentKey.string().optional("PROXY_PASS")

    private val proxyBaseUrlKey = EnvironmentKey.string().optional("PROXY_BASE_URL")

    private val proxyConnectionTimeoutMillisKey = EnvironmentKey.int().optional("PROXY_CONNECTION_TIMEOUT_MILLIS")

    private val proxyRequestTimeoutMillisKey = EnvironmentKey.int().optional("PROXY_REQUEST_TIMEOUT_MILLIS")


    private val env = MapEnvironment.from(System.getProperties(), separator = propsSeparator) overrides
            MapEnvironment.from(System.getenv().toProperties(), separator = propsSeparator) overrides
            Environment.defaults(
                serverPortKey of 8081,
                debugEnabledKey of false,
                targetEnvKey of "local",
                dbConnectionTTLMillisKey of 5.minutes.inWholeMilliseconds,
                dbConnectionIdleTimeoutMillisKey of 1.minutes.inWholeMilliseconds,
                dbConnectionCreateTimeoutMillisKey of  30.seconds.inWholeMilliseconds,
                dbQueryTimeoutMillisKey of 2.seconds.inWholeMilliseconds,
                dbEncryptionEnabledKey of false,
                developmentModeEnabledKey of false,
            )

    val serverPort = serverPortKey[env]

    val debugEnabled = debugEnabledKey[env]

    val environment = targetEnvKey[env]

    val serviceBaseUrl = serviceBaseUrlKey[env]

    val serviceUser = serviceUserKey[env]

    val servicePass = servicePassKey[env]

    val dbUsername = dbUsernameKey[env]

    val dbPass = dbPassKey[env]

    val dbName = dbNameKey[env]

    val dbEndpoint = dbEndpointKey[env]

    val dbMaxActive = dbMaxActiveKey[env]

    val dbConnectionTTLMillis = dbConnectionTTLMillisKey[env]

    val dbConnectionIdleTimeoutMillis = dbConnectionIdleTimeoutMillisKey[env]

    val dbConnectionCreateTimeoutMillis = dbConnectionCreateTimeoutMillisKey[env]

    val dbQueryTimeoutMillis = dbQueryTimeoutMillisKey[env]

    val dbEncryptionEnabled = dbEncryptionEnabledKey[env]

    val serviceVersion = serviceVersionKey[env] ?: "Not Assigned"

    val buildTime = serviceBuildTimeKey[env] ?: "Not Assigned"

    val developmentModeEnabled = developmentModeEnabledKey[env]

    val proxyUsername = proxyUsernameKey[env]

    val proxyPass = proxyPassKey[env]

    val proxyBaseUrl = proxyBaseUrlKey[env]

    val proxyConnectionTimeout = proxyConnectionTimeoutMillisKey[env]

    val proxyRequestTimeoutMillis = proxyRequestTimeoutMillisKey[env]

}