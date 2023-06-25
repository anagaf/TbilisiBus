package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.RouteShape
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TtcRetrofitService {
    @GET("/otp/routers/ttc/buses")
    fun getBuses(
        @Query("routeNumber") number: Int,
        @Query("forward") forward: Int
    ): Call<Buses>

    @GET("otp/routers/ttc/routeInfo?type=bus")
    fun getRouteShape(
        @Query("routeNumber") number: Int,
        @Query("forward") forward: Int
    ): Call<RouteShape>


}