package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class StopsResponseParser : JsonDeserializer<Stops>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): Stops {
        val stops = mutableListOf<Stop>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val stopsArray = rootNode.get("Stops")
        stopsArray.elements().forEach {
            stops.add(parseStop(it))
        }
        return Stops(stops)
    }

    private fun parseStop(node: JsonNode): Stop {
        val id = node["StopId"].asInt()
        val lat = node["Lat"].asDouble()
        val lon = node["Lon"].asDouble()
        val direction = if (node["Forward"].asBoolean()) Direction.Forward else Direction.Backward
        return Stop(id, Location(lat, lon), direction)
    }
}