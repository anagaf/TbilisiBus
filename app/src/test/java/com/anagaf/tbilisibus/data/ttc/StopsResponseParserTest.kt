package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StopsResponseParserTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("ttc_stops_response.json")
        val mapper = ObjectMapper()
        val deserializer = StopsResponseParser()
        val stops = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedStops = listOf(
            Stop(
                2949,
                Location(lat = 41.61098244835267, lon = 44.90064024925233),
                Direction.Forward
            ),
            Stop(
                4365,
                Location(lat = 41.6098081451219, lon = 44.90467010065914),
                Direction.Backward
            )
        )
        val expectedResponse = Stops(expectedStops)
        assertEquals(expectedResponse, stops)
    }
}