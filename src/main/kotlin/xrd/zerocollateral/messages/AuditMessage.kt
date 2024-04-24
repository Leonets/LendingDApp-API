package xrd.zerocollateral.messages

class Coin(
    val coin: String, val symbol: String, val  name: String, val icon_url: String
)

class CoinMessage(
    data: List<Coin>
) {
    var coins: List<Coin> = data
}

class Market(
    val component: String, val coin: String, val  tlender: String, val tborrower: String, val allowed_tusers: String,
    val rate_type: String, val duration_type: String
)

class MarketMessage(
    data: List<Market>
) {
    var markets: List<Market> = data
}

class ComponentAddress(
    val collateral: String, symbol: String, val name: String, val icon_url: String
)

class ComponentAddressMessage(
    data: List<ComponentAddress>
) {
    var ComponentAddresses: List<ComponentAddress> = data
}


//for feedback
data class FeedbackData(
    val overall_opinion: String,
    val functions: List<String>,
    val understood_contract_protection: String,
    val loan_protection: List<String>,
    val errors_occurred: String,
    val nft_received: String,
    val nft_removed: String,
    val feedback: String,
    val stokenet_address: String,
    val mainnet_address: String,
    val telegram_nickname: String,
    val blackbox_testing: String
)


