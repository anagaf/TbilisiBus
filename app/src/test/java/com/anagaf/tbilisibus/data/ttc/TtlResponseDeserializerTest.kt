package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.Location
import com.anagaf.tbilisibus.data.Buses
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
        val deserializer = BusesResponseParser()
        val response = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedBusLocations = listOf<Bus>(
            Bus(Location(41.76619056790743, 44.77884267730354)),
            Bus(Location(41.74894996470203, 44.777173414131894)),
        )
        val expectedResponse = Buses(expectedBusLocations)
        assertEquals(expectedResponse, response)
    }
}