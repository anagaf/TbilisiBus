package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.RouteShape
import com.anagaf.tbilisibus.data.Stop
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.model.LatLng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteShapeResponseParserTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("ttc_route_shape_response.json")
        val mapper = ObjectMapper()
        val deserializer = RouteShapeResponseParser()
        val shape = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedShape = RouteShape(
            stops = listOf(
                Stop(2188, Location(41.73948915307085, 44.96345490217209), Direction.Backward),
                Stop(4128, Location(41.73815219765541, 44.96386528015137), Direction.Backward),
            ),
            points = listOf(
                LatLng(41.73948915307085, 44.96345490217209),
                LatLng(41.7393030217242, 44.96352195739746),
            )
        )
        assertEquals(expectedShape, shape)
    }
}