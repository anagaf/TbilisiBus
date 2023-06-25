package com.anagaf.tbilisibus.data.ttc

import com.google.android.gms.maps.model.LatLng

data class RouteInfo(val stops: List<LatLng>, val shapePoints: List<LatLng>)