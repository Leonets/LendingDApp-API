package commons.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.node.*
import com.jayway.jsonpath.*
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.math.BigDecimal
import java.math.BigInteger

open class JacksonDocument(private val rootNode: JsonNode) {

    private val readContext: ReadContext = createContext(rootNode)

    constructor(document: String): this(readTree(document))

    fun node(path: String): JsonNode = this.readContext.read(path) ?: NullNode.instance

    fun string(path: String): String? = node(path).string()

    fun boolean(path: String, autoConvert: Boolean = true): Boolean? = node(path).boolean(autoConvert)

    fun number(path: String): BigDecimal? = node(path).number()

    fun <T> morph(deepCopy: Boolean = false, factory: (rootNode: JsonNode, readContext: ReadContext) -> T) :T {
        return if(deepCopy) {
            val rootClone: JsonNode = rootNode.deepCopy()
            val readContextClone: ReadContext = createContext(rootClone)
            factory(rootClone, readContextClone)
        } else {
            factory(rootNode, readContext)
        }
    }

    fun toJsonNode(deepCopy: Boolean = false): JsonNode = if(deepCopy) rootNode.deepCopy() else rootNode

    companion object {

        private val objectMapper: ObjectMapper = with(ObjectMapper()) {
            configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
            configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

        private val jsonPathConfig: Configuration = with(Configuration.builder()) {
            jsonProvider(JacksonJsonNodeJsonProvider(objectMapper))
            mappingProvider(JacksonMappingProvider(objectMapper))
            options(
                Option.DEFAULT_PATH_LEAF_TO_NULL,
                Option.SUPPRESS_EXCEPTIONS
            )
            build()
        }

        class ObjectNodeBuilder(private val objectNode: ObjectNode) {

            infix fun String.to(value: String?) {
                objectNode.put(this, value)
            }

            infix fun String.to(value: Boolean?) {
                objectNode.put(this, value)
            }

            infix fun String.to(value: Number?) {
                if(value != null) objectNode.put(this, toBigDecimal(value)) else objectNode.replace(this, NullNode.instance)
            }

            infix fun String.to(value: JsonNode?) {
                objectNode.replace(this, value)
            }
        }

        object arr: ArrayNode(objectMapper.nodeFactory) {

            override operator fun get(fieldName: String): ArrayNode {
                return addToArray(objectMapper.createArrayNode(), fieldName)
            }

            override operator fun get(index: Int): ArrayNode {
                return addToArray(objectMapper.createArrayNode(), index)
            }

            operator fun get(node: JsonNode): ArrayNode {
                return addToArray(objectMapper.createArrayNode(), node)
            }

            operator fun get(vararg elements: Any?): ArrayNode {
                return elements.fold(objectMapper.createArrayNode()) { container, value -> addToArray(container, value) }
            }

            operator fun get(elements: Iterable<Any?>): ArrayNode {
                return elements.fold(objectMapper.createArrayNode()) { container, value -> addToArray(container, value) }
            }
        }

        fun obj(block: (ObjectNodeBuilder.() -> Unit)): ObjectNode {
            val objectNode = objectMapper.createObjectNode()
            block(ObjectNodeBuilder(objectNode))
            return objectNode
        }

        fun JsonNode.string(): String? = when(this) {
            is NullNode -> null
            else -> textValue()
        }

        fun JsonNode.boolean(autoConvert: Boolean = true): Boolean? = when(this) {
            is NullNode -> null
            is BooleanNode -> booleanValue()
            is TextNode -> if(autoConvert) {
                textValue()?.lowercase()?.toBooleanStrictOrNull()
            } else null
            else -> null
        }

        fun JsonNode.number(): BigDecimal? = when(this) {
            is NumericNode -> toBigDecimal(numberValue())
            else -> null
        }

        fun JsonNode.toJsonString(writer: ObjectWriter = objectMapper.writerWithDefaultPrettyPrinter()): String = writer.writeValueAsString(this)

        fun JsonNode.toMinifiedJsonString(): String = objectMapper.writeValueAsString(this)

        fun ObjectNode.toJacksonDocument(): JacksonDocument = JacksonDocument(this)

        fun JacksonDocument.toJsonString(writer: ObjectWriter = objectMapper.writerWithDefaultPrettyPrinter()): String = rootNode.toJsonString(writer)

        fun JacksonDocument.toMinifiedJsonString(): String = rootNode.toMinifiedJsonString()

        private fun addToArray(arrayNode: ArrayNode, value: Any?): ArrayNode {
            return when(value) {
                is String -> arrayNode.add(value)
                is Number -> arrayNode.add(toBigDecimal(value))
                is Boolean -> arrayNode.add(value)
                is JsonNode -> arrayNode.add(value)
                else -> arrayNode
            }
        }
        private fun toBigDecimal(number: Number?) : BigDecimal = when(number) {
            is Int -> BigDecimal.valueOf(number.toLong())
            is Long -> BigDecimal.valueOf(number)
            is Short -> BigDecimal.valueOf(number.toLong())
            is Double -> BigDecimal.valueOf(number)
            is Float -> BigDecimal.valueOf(number.toDouble())
            is BigDecimal -> number
            is BigInteger -> number.toBigDecimal()
            else -> throw Exception("Number type is expected")
        }

        private fun createContext(node: JsonNode): DocumentContext = JsonPath.using(jsonPathConfig).parse(node)

        private fun readTree(document: String): JsonNode = objectMapper.readTree(document)

    }

}
