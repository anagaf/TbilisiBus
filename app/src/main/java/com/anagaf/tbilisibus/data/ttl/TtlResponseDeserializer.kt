package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.Coords
import com.anagaf.tbilisibus.data.BusLocations
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class TtlResponseDeserializer : JsonDeserializer<BusLocations>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): BusLocations {
        val busLocations = mutableListOf<BusLocation>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val busArray = rootNode.get("bus")
        busArray.elements().forEach {
            busLocations.add(parseBusLocation(it))
        }
        return BusLocations(busLocations)
    }

    private fun parseBusLocation(node: JsonNode): BusLocation {
        val lat = node["lat"].asDouble()
        val lon = node["lon"].asDouble()
        return BusLocation(Coords(lat, lon))
    }
}