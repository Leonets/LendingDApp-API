package xrd.zerocollateral

import com.fasterxml.jackson.databind.JsonNode
import commons.json.JacksonDocument

abstract class RequestDocument(root: JsonNode): JacksonDocument(root)