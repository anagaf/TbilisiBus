package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocations
import com.anagaf.tbilisibus.data.BusesOnRoute
import com.anagaf.tbilisibus.data.DataProvider
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

class TtlBusesOnRouteProvider @Inject constructor() : DataProvider {

    private val retrofitService: TtlRetrofitService by lazy {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(BusLocations::class.java, BusLocationsResponseParser())
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


    override suspend fun getBusesOnRoute(routeNumber: Int): BusesOnRoute {
        return withContext(Dispatchers.IO) {
            requestBusesOnRoute(routeNumber)
        }
    }

    private fun requestBusesOnRoute(routeNumber: Int): BusesOnRoute {
        return BusesOnRoute(
            requestBusLocations(routeNumber, Direction.Forward),
            requestBusLocations(routeNumber, Direction.Backward)
        )
    }

    private fun requestBusLocations(routeNumber: Int, direction: Direction): BusLocations {
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
            Direction.Forward -> 1
            Direction.Backward -> 0
        }
    }
}