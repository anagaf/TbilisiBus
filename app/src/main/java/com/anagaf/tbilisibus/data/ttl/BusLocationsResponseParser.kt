package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocations
import com.anagaf.tbilisibus.data.Location
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

class BusLocationsResponseParser : JsonDeserializer<BusLocations>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): BusLocations {
        val locations = mutableListOf<Location>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val busArray = rootNode.get("bus")
        busArray.elements().forEach {
            locations.add(parseBusLocation(it))
        }
        return BusLocations(locations)
    }

    private fun parseBusLocation(node: JsonNode): Location {
        val lat = node["lat"].asDouble()
        val lon = node["lon"].asDouble()
        return Location(lat, lon)
    }
}