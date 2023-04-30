package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.DataProvider
import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Stop
import com.anagaf.tbilisibus.data.Stops
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

class TtcDataProvider @Inject constructor() : DataProvider {

    private val retrofitService: TtcRetrofitService by lazy {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(Buses::class.java, BusesResponseParser())
        objectMapperModule.addDeserializer(Stops::class.java, StopsResponseParser())
        objectMapper.registerModule(objectMapperModule)

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .baseUrl("http://transfer.ttc.com.ge:8080")
            .build()
            .create(TtcRetrofitService::class.java)
    }


    override suspend fun getBusesOnRoute(routeNumber: Int): Buses =
        withContext(Dispatchers.IO) {
            requestBusesOnRoute(routeNumber)
        }

    override suspend fun getStops(routeNumber: Int): Stops = withContext(Dispatchers.IO) {
        requestStops(routeNumber)
    }

    private fun requestBusesOnRoute(routeNumber: Int): Buses =
        requestBuses(routeNumber, Direction.Forward) +
                requestBuses(routeNumber, Direction.Backward)


    private fun requestBuses(routeNumber: Int, direction: Direction): Buses {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getBuses(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("Buses request failed with code ${response.code()}")
        }
        return response.body()!!
    }

    private fun requestStops(routeNumber: Int): Stops =
        requestStops(routeNumber, Direction.Forward) +
                requestStops(routeNumber, Direction.Backward)

    private fun requestStops(routeNumber: Int, direction: Direction): Stops {
        val forward = getForwardDirectionCode(direction)
        val response = retrofitService.getStopLocations(routeNumber, forward).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("Stops request request failed with code ${response.code()}")
        }
        val stops = response.body()!!
        return Stops(stops.items.map { Stop(it.id, it.location, direction) })
    }

    private fun getForwardDirectionCode(direction: Direction): Int {
        return when (direction) {
            Direction.Forward -> 1
            Direction.Backward -> 0
        }
    }
}