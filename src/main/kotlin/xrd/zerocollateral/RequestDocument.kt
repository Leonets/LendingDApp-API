package xrd.zerocollateral

import com.fasterxml.jackson.databind.JsonNode
import com.gucci.oms.commons.json.JacksonDocument

abstract class RequestDocument(root: JsonNode): JacksonDocument(root)