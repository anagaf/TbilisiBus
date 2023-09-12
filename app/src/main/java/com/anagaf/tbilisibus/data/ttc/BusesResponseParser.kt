package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Bus
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.android.gms.maps.model.LatLng

class BusesResponseParser : JsonDeserializer<DirectionBuses>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): DirectionBuses {
        val buses = mutableListOf<Bus>()

        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        val busArray = rootNode.get("bus")
        busArray.elements().forEach {
            buses.add(parseBus(it))
        }
        return DirectionBuses(buses)
    }

    private fun parseBus(node: JsonNode): Bus {
        val lat = node["lat"].asDouble()
        val lon = node["lon"].asDouble()
        return Bus(LatLng(lat, lon))
    }
}