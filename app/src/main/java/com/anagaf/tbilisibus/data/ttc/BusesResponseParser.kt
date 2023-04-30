package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class BusesResponseParser : JsonDeserializer<Buses>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): Buses {
        val buses = mutableListOf<Bus>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val busArray = rootNode.get("bus")
        busArray.elements().forEach {
            buses.add(parseBus(it))
        }
        return Buses(buses)
    }

    private fun parseBus(node: JsonNode): Bus {
        val lat = node["lat"].asDouble()
        val lon = node["lon"].asDouble()
        val direction = if (node["forward"].asBoolean()) Direction.Forward else Direction.Backward
        val nextStopId = node["nextStopId"].asInt()
        return Bus(Location(lat, lon), direction, nextStopId)
    }
}