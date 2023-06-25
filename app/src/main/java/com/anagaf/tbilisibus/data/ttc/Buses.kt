package com.anagaf.tbilisibus.data.ttc

import com.google.android.gms.maps.model.LatLng

data class Buses(val items: List<LatLng>) {

    operator fun plus(rhs: Buses): Buses =
        Buses(items + rhs.items)

}
