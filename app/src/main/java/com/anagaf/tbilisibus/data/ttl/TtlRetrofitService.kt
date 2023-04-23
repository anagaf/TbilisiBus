package com.anagaf.tbilisibus.data.ttl

import com.anagaf.tbilisibus.data.BusLocations
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TtlRetrofitService {
    @GET("/otp/routers/ttc/buses")
    fun getBusLocations(
        @Query("routeNumber") number: Int,
        @Query("forward") forward: Int
    ): Call<BusLocations>
}