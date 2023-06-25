package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Bus
import com.google.android.gms.maps.model.LatLng

data class Buses(val items: List<Bus>) {

    operator fun plus(rhs: Buses): Buses =
        Buses(items + rhs.items)

}
