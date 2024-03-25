import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import lsis.axlsign
import lsis.toIntArray
import mu.KLoggable
import mu.KLogger
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.util.encoders.Hex
import org.http4k.core.*
import org.http4k.format.Jackson.auto
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
    val dAppDefinitionAddress: String?
        get() = node("$.dAppDefinitionAddress").string()
    val origin: String?
        get() = node("$.origin").string()

    val timeout: Int
        get() = node("$.timeout").intValue()
    val networkId: Int
        get() = node("$.networkId").intValue()
}

class ChallengeMessage(
    val challenge: String
)
class VerificationMessage(
    val verification: Boolean
)

data class Challenge(val expires: Long, val dAppDefinitionAddress: String, val origin: String, val timeout: Int)

object ChallengeStore : KLoggable {

    override val logger: KLogger = ZeroCollateralHandlers.logger()

    private val challenges = HashMap<String, Challenge>()

    val lens = Body.auto<List<SignedChallenge>>().toLens()

    //main function to create a Challenge
    fun createChallenge() = { request: Request ->
        val bodyLens = Body.auto<JsonNode>().map { ChallengeRequest(it) }.toLens()
        val searchRequest = bodyLens(request)
        when {
            searchRequest.dAppDefinitionAddress.isNullOrBlank()
                || searchRequest.timeout<=0
                || searchRequest.origin.isNullOrBlank() -> Response(Status.BAD_REQUEST)
            else -> {
                logger.info { "ApplicationName / dAppDefinitionAddress / origin " +
                        " ${searchRequest.applicationName} - ${searchRequest.dAppDefinitionAddress} - ${searchRequest.origin}" }
                Response(Status.OK).with(Body.auto<ChallengeMessage>().toLens() of ChallengeMessage(create(searchRequest)))
            }
        }
    }

    //main function to verify a Challenge
    fun verifyChallenge() = { request: Request ->
        logger.info( " request ${request.bodyString() }" )
        val signedChallenges: List<SignedChallenge> = lens(request)

        when {
            signedChallenges.filter { it.type == "account" }.size == 0
                                    -> Response(Status.BAD_REQUEST)
            else -> {
                val firstVerificationChallenge = signedChallenges
                    .filter { it.type == "account" }
                    .firstOrNull()
                val verificationResult = verify(firstVerificationChallenge!!)

                when {
                    signedChallenges
                        .filter { it.type == "account" }
                        .all { verificationChallenge ->
                            verificationResult.isVerified
                        } -> {
                        val authenticated = verifyChallengeWithLedger(signedChallenges, verificationResult.removedChallenge)
                        logger.info("authenticated => $authenticated")
                        Response(Status.OK).with(Body.auto<VerificationMessage>().toLens() of VerificationMessage(authenticated))
                    }
                    else -> {
                        Response(Status.CLIENT_TIMEOUT).with(Body.auto<VerificationMessage>().toLens() of VerificationMessage(false))
                    }
                }
            }
        }
    }

    //verify pub_key comparing with that on the ledger using gateway api
    private fun verifyChallengeWithLedger(signedChallenges: List<SignedChallenge>, removedChallenge: Challenge?) : Boolean {
        signedChallenges
            .filter { it.type == "account" }
            .map {
                val challenge = it.challenge
                val dAppDefinitionAddress = removedChallenge!!.dAppDefinitionAddress
                val origin = removedChallenge.origin

                // Construct the signature message
                val result = createSignatureMessage(challenge, dAppDefinitionAddress, origin)
                logger.info( " signatureMessage ${it.type} =>  ${result.getOrNull()} ")

                // Hash the publicKey provided in the request body
                val pubKeyHashed = createPublicKeyHash(it.proof.publicKey)
                val hashedPublicKey = when (pubKeyHashed.isSuccess) {
                    true -> pubKeyHashed.getOrNull()
                    else -> "failed"
                }
                logger.info( " hash of the publicKey provided request body (proof) [from request] ${it.type} =>  $hashedPublicKey ")

                // Check if the signature is valid
                val resultVerifyReal = axlsign.verify(it.proof.publicKey.toIntArray(), result.getOrNull()!!.toIntArray(), it.proof.signature.toIntArray())
                logger.info { "[axlsign] pubkey => ${it.proof.publicKey}" }
                logger.info { "[axlsign] signature => ${it.proof.signature}" }
                logger.info { "[axlsign] signature verified [1=true, 0=false] => $resultVerifyReal" }

                // Define the base URL and request body
                val baseUrl = "https://stokenet.radixdlt.com/state/entity/details"
                val addresses = listOf(it.address)

                val requestBodyJson = "{\"addresses\": ${addresses.joinToString(",", "[\"", "\"]")}, \"aggregation_level\": \"Vault\"}"

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
                logger.info("Response Code from gateway api: ${response.statusCode()}")
                //            logger.info("Response Body: ${response.body()}")

                val mapper = jacksonObjectMapper()
                val responseData: LedgerResponse = mapper.readValue(response.body())

                // Get owner_keys from metadata if the value is set
                val ownerKeyMetadataValue = responseData.items[0].metadata.items[0].value.typed.values!![0].hash_hex
                logger.info("ownerKeyMetadataValue [from ledger] => $ownerKeyMetadataValue")

                return when (hashedPublicKey == ownerKeyMetadataValue) {
                    true -> true
                    else -> false
                }
            }[0]
    }

    //Utility functions

    //creates a Challenge
    fun create(challengeRequest: ChallengeRequest): String {
        val challenge = secureRandom(32)
        val expires = System.currentTimeMillis() + 1000 * challengeRequest.timeout // expires in 5 minutes
        challenges[challenge] = Challenge(expires, challengeRequest.dAppDefinitionAddress!!, challengeRequest.origin!!, challengeRequest.timeout)
        logger.info { " new challenge = $challenge expires at $expires" }
        return challenge
    }

    //Check if challenge has expired
    data class VerificationResult(val isVerified: Boolean, val removedChallenge: Challenge?)

    fun verify(signedChallenge: SignedChallenge): VerificationResult {
        val challengeValue = signedChallenge.challenge
        val challenge = challenges.remove(challengeValue)
        val now = System.currentTimeMillis()
        val isVerified = (challenge?.expires ?: 0) > now
        logger.info { " challenge expiring at = $challenge?.expires and now is $now" }
        return VerificationResult(isVerified, challenge)
    }

    //to read the result of hashing
    sealed class PublicKeyHashResult {
        data class Success(val publicKeyHash: String) : PublicKeyHashResult()
        data class Failure(val error: Exception) : PublicKeyHashResult()
    }

    //to hash the publicKey provided request body
    private fun createPublicKeyHash(publicKey: String): Result<String> {
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
    private fun createSignatureMessage(
        challenge: String,
        dAppDefinitionAddress: String,
        origin: String
    ): Result<String> {
        val prefix = "R".toByteArray(StandardCharsets.US_ASCII)
        val lengthOfDappDefAddress = dAppDefinitionAddress.length
        val lengthOfDappDefAddressBuffer = lengthOfDappDefAddress.toString(16).toByteArray(StandardCharsets.US_ASCII)
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

    private fun secureRandom(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
