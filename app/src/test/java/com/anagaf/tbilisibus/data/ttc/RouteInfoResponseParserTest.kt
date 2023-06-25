package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.model.LatLng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteInfoResponseParserTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("ttc_route_shape_response.json")
        val mapper = ObjectMapper()
        val deserializer = RouteInfoResponseParser()
        val shape = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedRouteInfo = RouteInfo(
            stops = listOf(
                Stop(LatLng(41.73948915307085, 44.96345490217209)),
                Stop(LatLng(41.73815219765541, 44.96386528015137)),
            ),
            shapePoints = listOf(
                ShapePoint(LatLng(41.73948915307085, 44.96345490217209)),
                ShapePoint(LatLng(41.7393030217242, 44.96352195739746)),
            )
        )
        assertEquals(expectedRouteInfo, shape)
    }
}