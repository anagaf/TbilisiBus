package com.anagaf.tbilisibus.data

import com.google.android.gms.maps.model.LatLng

data class RouteShape(val stops: List<Stop>, val points: List<LatLng>) {

    operator fun plus(rhs: RouteShape): RouteShape =
        RouteShape(stops + rhs.stops, points + rhs.points)

}