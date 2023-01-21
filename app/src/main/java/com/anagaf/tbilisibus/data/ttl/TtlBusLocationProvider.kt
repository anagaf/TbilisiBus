package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocation
import com.anagaf.tbilisibus.data.BusLocationProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import mu.KotlinLogging
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Inject

class TtlBusLocationProvider @Inject constructor() : BusLocationProvider {

    private val retrofitService: TtlRetrofitService by lazy {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(TtlResponse::class.java, TtlResponseDeserializer())
        objectMapper.registerModule(objectMapperModule)

        Retrofit.Builder()
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .baseUrl("http://transfer.ttc.com.ge")
            .build()
            .create(TtlRetrofitService::class.java)
    }


    override suspend fun getBusLocations(routeNumber: Int): List<BusLocation> {
        val response = retrofitService.getBusLocations(routeNumber).execute()
        if (!response.isSuccessful) {
            throw java.lang.Exception("TTL request failed with code ${response.code()}")
        }
        return response.body()!!
    }
}