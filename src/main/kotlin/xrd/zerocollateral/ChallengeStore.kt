import com.fasterxml.jackson.databind.JsonNode
import mu.KLogger
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.string
import xrd.zerocollateral.RequestDocument
import xrd.zerocollateral.servers.ZeroCollateralHandlers
import java.security.SecureRandom
import java.util.*

data class SignedChallenge(
    val publicKey: String,
    val signature: String,
    val curve: String // You can change this to an enum if you have predefined options
)

/*{
    "applicationName": "RadixCharts",
    "dAppDefinitionAddress": "account_rdx16x9mmsy3gasxrn7d2jey6cnzflk8a5w24fghjg082xqa3ncgxjqct3",
    "expectedOrigin": "https://radixcharts.com/",
    "expires": 60,
    "networkId": 1
}*/

class ChallengeRequest(rootNode: JsonNode) : RequestDocument(rootNode) {

    val applicationName: String?
        get() = node("$.applicationName").string()

    /*val applicationName: String,
    val dAppDefinitionAddress: String,
    val expectedOrigin: String,
    val expires: Int,
    val networkId: Int*/
}

//export type SignedChallenge = {
//    address: string;
//    type: 'persona' | 'account';
//    challenge: string;
//    proof: {
//        publicKey: string;
//        signature: string;
//        curve: 'curve25519' | 'secp256k1';
//    };
//};

val lens = Body.auto<List<SignedChallenge>>().toLens()

class ChallengeMessage(
val challenge: String
)

object ChallengeStore : HttpHandler {

val logger: KLogger = ZeroCollateralHandlers.logger()

private val bodyLens = Body.string(contentType = ContentType.APPLICATION_JSON).toLens()

override fun invoke(req: Request): Response = Response(Status.OK).with(bodyLens of create())

private val challenges = HashMap<String, Challenge>()


fun create(): String {
    val challenge = secureRandom(32)
    val expires = System.currentTimeMillis() + 1000 * 60 * 5 // expires in 5 minutes
    challenges[challenge] = Challenge(expires)
    logger.info { " new challenge = " + challenge }
    return challenge
}

fun createChallenge() = { request: Request ->
    val bodyLens = Body.auto<JsonNode>().map { ChallengeRequest(it) }.toLens()
    val searchRequest = bodyLens(request)
    logger.info { " application name ${searchRequest.applicationName}" }
    Response(Status.OK).with(Body.auto<ChallengeMessage>().toLens() of ChallengeMessage(create()))
}

fun verify(signedChallenge: SignedChallenge): Boolean {
    val signature = signedChallenge.signature
    val challenge = challenges.remove(signature)
    return challenge?.expires ?: 0 > System.currentTimeMillis()
}

private fun secureRandom(length: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

private data class Challenge(val expires: Long)

/*
    walletSuccessResponse {
        discriminator: 'authorizedRequest',
        auth:
        {
            discriminator: 'loginWithChallenge',
            persona:
            {
                    identityAddress: 'identity_tdx_2_12fhdtpga9dykpzjvjccy5j6mpt25lgvhwce53rrn869fdxvdeq86t9',
                    label: 'Leonets dApp3'
            },
            challenge: 'e8d2b3231884b35ca712f1748a3904f0cc3ed1d1e661a7f8d173a1473080c17a',
            proof:
            {
                    publicKey: 'a12f3b6884797a44d979907eaaaba52644592d4d4e6093098311e67620e9d9cf',
                    signature: '0f69c5acca59e780abe766d070056297d2beb31adcc50043e5723c761e8ff86935ab4e3a6592269906a91ba0d51ed1675a780f7c4b3f669ad95d72c0f867c003',
                    curve: 'curve25519'
            }
        },
        ongoingAccounts:
        {
            accounts:
            [
                {
                        address: 'account_tdx_2_12870m7gklv3p90004zjnm39jrhpf2vseejrgpncptl7rhsagz8yjm9',
                        label: 'Leonets Stoke',
                        appearanceId: 0
                }
            ],
            challenge: 'e8d2b3231884b35ca712f1748a3904f0cc3ed1d1e661a7f8d173a1473080c17a',
            proofs:
            [
                {
                    accountAddress: 'account_tdx_2_12870m7gklv3p90004zjnm39jrhpf2vseejrgpncptl7rhsagz8yjm9',
                    proof:
                    {
                            publicKey: '71b480232435963743746d6d4924b1ee4cf4b1e23acc594d816ceaac5c50ee22',
                            signature: 'c4251a2acf0a021bfd84222dfd6dd0c1b16b6acb736429b81db56beebe780570997f7159ee7f2507d0363d0711c4da792da7c4d1168f4289b143f3de66043a0a',
                            curve: 'curve25519'
                    }
                }
            ]
        }
    }
*/
    //Input
//    {
//        "address": "identity_rdx1224d84j908t2te3j9vxul9l2ktnvjulzqk53e8afl688g4k597smjn",
//        "challenge": "9178c46b079d4b1a889305a08b1152e1c8b3bae7abfe36df734ac3c96f6c2f19",
//        "proof": {
//        "curve": "curve25519",
//        "publicKey": "297a92d920988644a3b1b7953b71790ed4dfc0bb14a63af65d13463e39c2775c",
//        "signature": "a1daadc11b2af9908774f309d6ac8d6413e320a28b07803eab07c6b5d143eb88b142a5c91f59b6ef226a3385ee2eae8688de2d284483c46322d0b521988dfd0b"
//    },
//        "type": "persona"
//    }
//    e output
//    {
//        "verification": "successful",
//        "code": 200,
//        "time_utc": 1703151719
//    }
fun verifyChallenge() = { request: Request ->

    logger.info( " request ${request.bodyString() }" )
    val signedChallenges: List<SignedChallenge> = lens(request)

    val isChallengeValid = signedChallenges.all { challenge ->
        ChallengeStore.verify(challenge)
    }

    if (!isChallengeValid) {
        Response(Status.CLIENT_TIMEOUT).with(Body.auto<String>().toLens() of "Challenge Not Valid Anymore")
    }

    //questo credo sia il punto in cui verificare una signature
//        val result = request.body.map { signedChallenge ->
//            rolaInstance.verifySignedChallenge(signedChallenge)
//        }.combine()

    //poi si deve andare a recuperare il metadata dell'account address
//        We query the ledger through the gateway API to get `metadata`
//        for the address provided in the request body.
//        In the `metadata` we are specifically looking for `owner_keys` field.
//        E se esiste si deve andare a fare questa operazione
//        If the `owner_keys` metadata value is set
//        we need to hash the publicKey provided request body and
//        check if it matches the value in `owner_keys`.

//        if (result.isErr()) {
//            Response(Status.SERVICE_UNAVAILABLE).with(Body.auto<String>().toLens() of "Challenge Validation Not Possible")
//        }

    // The signature is valid and the public key is owned by the user
//        res.send(mapOf("valid" to true))
    Response(Status.OK).with(Body.auto<String>().toLens() of "Challenge Valid")
}

}
