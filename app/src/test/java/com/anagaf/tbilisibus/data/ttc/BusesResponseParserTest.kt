package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Direction
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.model.LatLng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BusesResponseParserTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            ?.getResourceAsStream("ttc_buses_response.json")
        val mapper = ObjectMapper()
        val deserializer = BusesResponseParser()
        val buses = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedBuses = listOf(
            LatLng(41.76619056790743, 44.77884267730354),
            LatLng(41.74894996470203, 44.777173414131894),
        )
        val expectedResponse = Buses(expectedBuses)
        assertEquals(expectedResponse, buses)
    }
}