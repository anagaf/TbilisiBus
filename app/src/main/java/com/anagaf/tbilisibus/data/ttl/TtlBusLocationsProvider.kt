package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationsProvider
import com.anagaf.tbilisibus.data.BusLocations
import com.anagaf.tbilisibus.data.Direction
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TtlBusLocationsProvider @Inject constructor() : BusLocationsProvider {

    private val retrofitService: TtlRetrofitService by lazy {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(List::class.java, TtlResponseDeserializer())
        objectMapper.registerModule(objectMapperModule)

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .baseUrl("http://transfer.ttc.com.ge:8080")
            .build()
            .create(TtlRetrofitService::class.java)
    }


    override suspend fun getBusLocations(routeNumber: Int): BusLocations {
        return withContext(Dispatchers.IO) {
            requestBusLocations(routeNumber)
        }
    }

    private fun requestBusLocations(routeNumber: Int): BusLocations {
        logger.debug { "Making request" }

        val forwardLocations = requestBusLocation(routeNumber, Direction.Forward)
        val backwardLocations = requestBusLocation(routeNumber, Direction.Backward)

        return forwardLocations + backwardLocations
    }

    private fun requestBusLocation(routeNumber: Int, direction: Direction): List<BusLocation> {
        logger.debug { "Making request" }

        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getBusLocations(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("TTL request failed with code ${response.code()}")
        }
        logger.debug { "Request succeeded" }
        return response.body()!!
    }

    private fun getForwardDirectionCode(direction: Direction): Int {
        return when (direction) {
            Direction.Forward -> 1;
            Direction.Backward -> 0;
        }
    }
}