package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.android.gms.maps.model.LatLng

class RouteInfoResponseParser : JsonDeserializer<RouteInfo>() {

    override fun deserialize(
        parser: JsonParser?,
        ctxt: DeserializationContext?
    ): RouteInfo {
        val rootNode: JsonNode = parser?.codec?.readTree(parser)
            ?: throw JsonParseException(parser, "Cannot access root node")
        return RouteInfo(stops = parseStops(rootNode), shapePoints = parseShapePoints(rootNode))
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
        val lat = node["Lat"].asDouble()
        val lon = node["Lon"].asDouble()
        return Stop(LatLng(lat, lon))
    }

    private fun parseShapePoints(rootNode: JsonNode): List<ShapePoint> {
        val points = mutableListOf<ShapePoint>()
        val shapeNode = rootNode.get("Shape")
        val shapeStr = shapeNode.asText()
        shapeStr.split(',').forEach {
            val latLng = it.split(':')
            points.add(ShapePoint(LatLng(latLng[1].toDouble(), latLng[0].toDouble())))
        }
        return points
    }
}