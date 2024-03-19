package xrd.zerocollateral.servers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.pool.ConnectionPool
import mu.KLoggable
import mu.KLogger
import org.http4k.core.*
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.format.Jackson.auto
import org.http4k.lens.Lens
import xrd.zerocollateral.messages.*

object ZeroCollateralHandlers: KLoggable {

    override val logger: KLogger = logger()

    class TrapeziteDatabaseHandler (private val connectionPool: ConnectionPool<MySQLConnection>) {

        private val coinDetailsFullQuery = """
            SELECT DISTINCT c.*
            FROM coin c
            INNER JOIN loan_market lm ON c.coin = lm.coin
            WHERE 
                CASE
                    WHEN ? = 'ALL' THEN  lm.allowed_tusers IN ('ALL')
                    WHEN ? = 'REGISTERED' THEN lm.allowed_tusers IN ('ALL', 'REGISTERED')
                    WHEN ? = 'KYC' THEN lm.allowed_tusers IN ('ALL', 'REGISTERED', 'KYC')
                    ELSE FALSE
                END      
        """.trimIndent()


        private val marketListFullQuery = """
            SELECT component,coin,tlender,tborrower,
                allowed_tusers,rate_type,duration_type
                FROM loan_market
                WHERE coin = ? AND allowed_tusers = ?""".trimIndent()

        private val collateralListFullQuery = """
            SELECT ac.collateral, c.symbol, c.name, c.icon_url
                FROM accepted_collateral ac
                JOIN coin c ON ac.collateral = c.coin
                WHERE ac.component = ?""".trimIndent()

        private val objectMapper = ObjectMapper()

        fun lookupCoinDetails(allowedTuser: String): Result<List<Coin>> = runCatching {
            val rowList = connectionPool.sendPreparedStatement(coinDetailsFullQuery, listOf(allowedTuser, allowedTuser, allowedTuser)).get().rows
            return when (rowList.size) {
                0 -> Result.failure(Exception("Cannot lookup any coin info for $allowedTuser"))
                else -> {
                    val logData = rowList.map {
                        Coin(
                            it.getString("coin") ?: "-",
                            it.getString("symbol") ?: "-",
                            it.getString("name") ?: "-",
                            it.getString("icon_url") ?: "-")
                    }
                    Result.success(logData)
                }
            }
        }

        fun lookupMarketList(coin: String, allowedTuser: String): Result<List<Market>> = runCatching {
            val rowList = connectionPool.sendPreparedStatement(marketListFullQuery, listOf(coin, allowedTuser)).get().rows
            return when (rowList.size) {
                0 -> Result.failure(Exception("Cannot lookup any market info for $coin and $allowedTuser"))
                else -> {
                    val logData = rowList.map {
                        Market(
                            it.getString("component") ?: "-",
                            it.getString("coin") ?: "-",
                            it.getString("tlender") ?: "-",
                            it.getString("tborrower") ?: "-",
                            it.getString("allowed_tusers") ?: "-",
                            it.getString("rate_type") ?: "-",
                            it.getString("duration_type") ?: "-"
                            )
                    }
                    Result.success(logData)
                }
            }
        }

        fun lookupCollateralList(componentAddress: String): Result<List<ComponentAddress>> = runCatching {
            val rowList = connectionPool.sendPreparedStatement(collateralListFullQuery, listOf(componentAddress)).get().rows
            return when (rowList.size) {
                0 -> Result.failure(Exception("Cannot lookup any collateral info for  $componentAddress"))
                else -> {
                    val logData = rowList.map {
                        ComponentAddress(
                            it.getString("collateral") ?: "-",
                            it.getString("symbol") ?: "-",
                            it.getString("name") ?: "-",
                            it.getString("icon_url") ?: "-")
                    }
                    Result.success(logData)
                }
            }
        }
    }


    fun lookupCoinDetails(dataService: TrapeziteDatabaseHandler, allowedTuserLens: Lens<Request, String?>) = { request: Request ->
        val allowedTuser = allowedTuserLens(request)
        when {
            allowedTuser.isNullOrBlank() -> Response(Status.BAD_REQUEST)
            else -> {
                when (val coinsReference = dataService.lookupCoinDetails(allowedTuser).getOrNull()) {
                    null -> Response(NOT_FOUND)
                    else -> {
                        Response(Status.OK).with(Body.auto<CoinMessage>().toLens() of CoinMessage(coinsReference))
                    }
                }
            }
        }
    }

    fun lookupMarkets(dataService: TrapeziteDatabaseHandler, coinLens: Lens<Request, String?>, allowedTuserLens: Lens<Request, String?>) = { request: Request ->
        val coin = coinLens(request)
        val allowedTuser = allowedTuserLens(request).toString()
        when {
            coin.isNullOrBlank() -> Response(Status.BAD_REQUEST)
            else -> {
                when (val marketsReference = dataService.lookupMarketList(coin, allowedTuser).getOrNull()) {
                    null -> Response(NOT_FOUND)
                    else -> {
                        Response(Status.OK).with(Body.auto<MarketMessage>().toLens() of MarketMessage(marketsReference))
                    }
                }
            }
        }
    }


    fun lookupCollateral(dataService: TrapeziteDatabaseHandler, componentAddressLens: Lens<Request, String?>) = { request: Request ->
        val componentAddress = componentAddressLens(request)
        when {
            componentAddress.isNullOrBlank() -> Response(Status.BAD_REQUEST)
            else -> {
                when (val collateralReference = dataService.lookupCollateralList(componentAddress).getOrNull()) {
                    null -> Response(NOT_FOUND)
                    else -> {
                        Response(Status.OK).with(Body.auto<ComponentAddressMessage>().toLens() of ComponentAddressMessage(collateralReference))
                    }
                }
            }
        }
    }

}
