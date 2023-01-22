package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.Coords
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class TtlResponseDeserializerTest {
    @Test
    fun deserialize() {
        val stream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream("ttl_response.json")
        val mapper = ObjectMapper()
        val deserializer = TtlResponseDeserializer()
        val response = deserializer.deserialize(
            mapper.factory.createParser(stream),
            mapper.deserializationContext
        )
        val expectedBusLocations = listOf<BusLocation>(
            BusLocation(Coords(41.76619056790743, 44.77884267730354)),
            BusLocation(Coords(41.74894996470203, 44.777173414131894)),
        )
        val expectedResponse = TtlResponse(expectedBusLocations)
        assertEquals(expectedResponse, response)
    }
}