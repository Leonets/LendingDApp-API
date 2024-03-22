import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.util.encoders.Hex
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.string
import xrd.zerocollateral.RequestDocument
import xrd.zerocollateral.servers.ZeroCollateralHandlers
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*

//data class SignedChallenge
data class SignedChallenge(
    val challenge: String,
    val proof: Proof,
    val address: String,
    val type: String
)

//data class for mapping Proof
data class Proof(
    val publicKey: String,
    val signature: String,
    val curve: String // You can change this to an enum if you have predefined options
)

//data class for mapping ledger entity state response
data class LedgerState(
    val network: String,
    val state_version: Long,
    val proposer_round_timestamp: String,
    val epoch: Int,
    val round: Int
)

data class Field(
    val element_kind: String,
    val elements: List<Element>,
    val kind: String
)

data class Element(
    val variant_id: Int,
    val fields: List<Field>,
    val kind: String,
    val hex: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Typed(
    val values: List<Value>?,
    val type: String
)

data class Value @JsonCreator constructor(
    @JsonProperty("hash_hex") val hash_hex: String,
    @JsonProperty("key_hash_type") val key_hash_type: String
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(hash_hex: String) : this(hash_hex, "")
}

data class Item(
    val key: String,
    val value: ItemValue,
    val is_locked: Boolean,
    val last_updated_at_state_version: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemValue(
    val raw_hex: String,
    val typed: Typed
)

data class Metadata(
    val total_count: Int,
    val items: List<Item>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Items(
    val address: String,
    val metadata: Metadata
)

data class LedgerResponse(
    val ledger_state: LedgerState,
    val items: List<Items>
)

class ChallengeRequest(rootNode: JsonNode) : RequestDocument(rootNode) {

    val applicationName: String?
        get() = node("$.applicationName").string()
}

val lens = Body.auto<List<SignedChallenge>>().toLens()

class ChallengeMessage(
    val challenge: String
)
class VerificationMessage(
    val verification: Boolean
)

private fun secureRandom(length: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

private data class Challenge(val expires: Long)

object ChallengeStore : HttpHandler {

    val logger: KLogger = ZeroCollateralHandlers.logger()

    private val bodyLens = Body.string(contentType = ContentType.APPLICATION_JSON).toLens()

    override fun invoke(req: Request): Response = Response(Status.OK).with(bodyLens of create())

    private val challenges = HashMap<String, Challenge>()

    val lens = Body.auto<List<SignedChallenge>>().toLens()

    //main function to create a Challenge
    fun createChallenge() = { request: Request ->
        val bodyLens = Body.auto<JsonNode>().map { ChallengeRequest(it) }.toLens()
        val searchRequest = bodyLens(request)
        logger.info { " application name ${searchRequest.applicationName}" }
        Response(Status.OK).with(Body.auto<ChallengeMessage>().toLens() of ChallengeMessage(create()))
    }

    //main function to verify a Challenge
    fun verifyChallenge() = { request: Request ->
        logger.info( " request ${request.bodyString() }" )
        val signedChallenges: List<SignedChallenge> = lens(request)

        val isChallengeValid = signedChallenges.all { challenge ->
            ChallengeStore.verify(challenge)
        }

        if (!isChallengeValid) {
            Response(Status.CLIENT_TIMEOUT).with(Body.auto<String>().toLens() of "Challenge Not Valid Anymore")
        }

        val authenticated = verifyChallengeWithLedger(signedChallenges)
        logger.info("authenticated => ${authenticated}")

        Response(Status.OK).with(Body.auto<VerificationMessage>().toLens() of VerificationMessage(authenticated))
    }

    fun verifyChallengeWithLedger(signedChallenges: List<SignedChallenge>) : Boolean {
        signedChallenges
            .filter { it.type != "account" }
            .map {
                val challenge = it.challenge
                val dAppDefinitionAddress = "account_tdx_2_12870m7gklv3p90004zjnm39jrhpf2vseejrgpncptl7rhsagz8yjm9"
                val origin = "https://test.zerocollateral.eu/"

                val result = createSignatureMessage(challenge, dAppDefinitionAddress, origin)
                logger.info( " signatureMessage ${it.type} =>  ${result.getOrNull()} ")

                val pubKeyHashed = createPublicKeyHash(it.proof.publicKey)
                val hashedPublicKey = when (pubKeyHashed.isSuccess) {
                    true -> pubKeyHashed.getOrNull()
                    else -> "failed"
                }
                logger.info( " hash of the publicKey provided request body (proof) ${it.type} =>  ${hashedPublicKey} ")

                // Define the base URL and request body
                val baseUrl = "https://stokenet.radixdlt.com/state/entity/details"
                val addresses = listOf(it.address)

                val requestBodyJson = "{\"addresses\": ${addresses.joinToString(",", "[\"", "\"]")}, \"aggregation_level\": \"Vault\"}"
                logger.info("Request Body: ${requestBodyJson}")

                // Create HttpClient instance
                val client = HttpClient.newHttpClient()

                // Create HttpRequest with POST method and JSON body
                val request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                        .build()

                // Send the request and handle the response
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                logger.info("Response Code: ${response.statusCode()}")
                //            logger.info("Response Body: ${response.body()}")

                val mapper = jacksonObjectMapper()
                val responseData: LedgerResponse = mapper.readValue(response.body())

                val ownerKeyMetadataValue = responseData.items.get(0).metadata.items.get(0).value.typed.values!!.get(0).hash_hex
                logger.info("ownerKeyMetadataValue: ${ownerKeyMetadataValue}")

                return when (hashedPublicKey == ownerKeyMetadataValue) {
                    true -> true
                    else -> false
                }
            }
            .get(0)
    }

    //Utility functions

    //creates a Challenge
    fun create(): String {
        val challenge = secureRandom(32)
        val expires = System.currentTimeMillis() + 1000 * 60 * 5 // expires in 5 minutes
        challenges[challenge] = Challenge(expires)
        logger.info { " new challenge = " + challenge }
        return challenge
    }

    //Check if challenge has expired
    fun verify(signedChallenge: SignedChallenge): Boolean {
        val challengeValue = signedChallenge.challenge
        val challenge = challenges.remove(challengeValue)
        return challenge?.expires ?: 0 > System.currentTimeMillis()
    }

    //to read the result of hashing
    sealed class PublicKeyHashResult {
        data class Success(val publicKeyHash: String) : PublicKeyHashResult()
        data class Failure(val error: Exception) : PublicKeyHashResult()
    }

    //to hash the publicKey provided request body
    fun createPublicKeyHash(publicKey: String): Result<String> {
        return runCatching {
            val publicKeyBuffer = Hex.decode(publicKey)
            val blake2b = Blake2bDigest(256)
            blake2b.update(publicKeyBuffer, 0, publicKeyBuffer.size)
            val hash = ByteArray(32)
            blake2b.doFinal(hash, 0)

            // Extracting the last 29 bytes of the hash
            val publicKeyHashBuffer = hash.copyOfRange(3, 32)

            // Converting the hash bytes to hexadecimal string
            Hex.toHexString(publicKeyHashBuffer)
        }
    }

    //Construct the signature message
    fun createSignatureMessage(
        challenge: String,
        dAppDefinitionAddress: String,
        origin: String
    ): Result<String> {
        val prefix = "R".toByteArray(StandardCharsets.US_ASCII)
        val lengthOfDappDefAddress = "dappAdd"
        val lengthOfDappDefAddressBuffer = lengthOfDappDefAddress.toByteArray(StandardCharsets.UTF_8)
        val dappDefAddressBuffer = dAppDefinitionAddress.toByteArray(StandardCharsets.UTF_8)
        val originBuffer = origin.toByteArray(StandardCharsets.UTF_8)
        val challengeBuffer = Hex.decode(challenge)

        val messageBuffer = ByteArray(
            prefix.size +
                    challengeBuffer.size +
                    lengthOfDappDefAddressBuffer.size +
                    dappDefAddressBuffer.size +
                    originBuffer.size
        )
        System.arraycopy(prefix, 0, messageBuffer, 0, prefix.size)
        System.arraycopy(challengeBuffer, 0, messageBuffer, prefix.size, challengeBuffer.size)
        System.arraycopy(
            lengthOfDappDefAddressBuffer,
            0,
            messageBuffer,
            prefix.size + challengeBuffer.size,
            lengthOfDappDefAddressBuffer.size
        )
        System.arraycopy(
            dappDefAddressBuffer,
            0,
            messageBuffer,
            prefix.size + challengeBuffer.size + lengthOfDappDefAddressBuffer.size,
            dappDefAddressBuffer.size
        )
        System.arraycopy(
            originBuffer,
            0,
            messageBuffer,
            prefix.size + challengeBuffer.size + lengthOfDappDefAddressBuffer.size + dappDefAddressBuffer.size,
            originBuffer.size
        )

        val blake2b = Blake2bDigest(256)
        blake2b.update(messageBuffer, 0, messageBuffer.size)
        val hash = ByteArray(32)
        blake2b.doFinal(hash, 0)

        val hexString = Hex.toHexString(hash)

        return Result.success(hexString)
    }
}
