package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.RouteShape
import com.anagaf.tbilisibus.data.Stop
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.android.gms.maps.model.LatLng

class RouteShapeResponseParser : JsonDeserializer<RouteShape>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): RouteShape {
        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        return RouteShape(stops = parseStops(rootNode), points = parsePoints(rootNode))
    }

    private fun parseStops(rootNode: JsonNode): List<Stop> {
        val stops = mutableListOf<Stop>()
        val stopsArray = rootNode.get("RouteStops")
        stopsArray.elements().forEach {
            stops.add(parseStop(it))
        }
        return stops
    }

    private fun parseStop(node: JsonNode): Stop {
        val id = node["StopId"].asInt()
        val lat = node["Lat"].asDouble()
        val lon = node["Lon"].asDouble()
        val direction = if (node["Forward"].asBoolean()) Direction.Forward else Direction.Backward
        return Stop(id, Location(lat, lon), direction)
    }

    private fun parsePoints(rootNode: JsonNode): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val shapeNode = rootNode.get("Shape")
        val shapeStr = shapeNode.asText()
        shapeStr.split(',').forEach {
            val latLng = it.split(':')
            points.add(LatLng(latLng[1].toDouble(), latLng[0].toDouble()))
        }
        return points
    }
}