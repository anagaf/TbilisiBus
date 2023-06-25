package com.anagaf.tbilisibus.data.ttc

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.android.gms.maps.model.LatLng

class BusesResponseParser : JsonDeserializer<Buses>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): Buses {
        val buses = mutableListOf<LatLng>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val busArray = rootNode.get("bus")
        busArray.elements().forEach {
            buses.add(parseBus(it))
        }
        return Buses(buses.toList())
    }

    private fun parseBus(node: JsonNode): LatLng {
        val lat = node["lat"].asDouble()
        val lon = node["lon"].asDouble()
        return LatLng(lat, lon)
    }
}