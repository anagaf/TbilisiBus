package com.anagaf.tbilisibus.data.ttc

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TtcRetrofitService {

    @GET("/otp/routers/ttc/buses")
    fun getBuses(
        @Query("routeNumber") number: Int,
        @Query("forward") forward: Int
    ): Call<DirectionBuses>

    @GET("otp/routers/ttc/routeInfo?type=bus")
    fun getRouteInfo(
        @Query("routeNumber") number: Int,
        @Query("forward") forward: Int
    ): Call<DirectionRouteInfo>


}