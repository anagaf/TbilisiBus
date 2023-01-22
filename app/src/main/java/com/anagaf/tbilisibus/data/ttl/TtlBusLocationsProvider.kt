package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocationsProvider
import com.anagaf.tbilisibus.data.BusLocations
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
        objectMapperModule.addDeserializer(BusLocations::class.java, TtlResponseDeserializer())
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
        val response = retrofitService.getBusLocations(routeNumber).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("TTL request failed with code ${response.code()}")
        }
        logger.debug { "Request succeeded" }
        return response.body()!!
    }
}