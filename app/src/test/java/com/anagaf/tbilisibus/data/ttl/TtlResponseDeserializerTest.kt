package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusOnRoute
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.BusesOnRoute
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class TtlResponseDeserializerTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream("ttl_response.json")
        val mapper = ObjectMapper()
        val deserializer = BusLocationsResponseParser()
        val response = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedBusLocations = listOf<BusOnRoute>(
            BusOnRoute(Location(41.76619056790743, 44.77884267730354)),
            BusOnRoute(Location(41.74894996470203, 44.777173414131894)),
        )
        val expectedResponse = BusesOnRoute(expectedBusLocations)
        assertEquals(expectedResponse, response)
    }
}