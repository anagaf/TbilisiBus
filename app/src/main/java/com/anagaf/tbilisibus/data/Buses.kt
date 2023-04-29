package com.anagaf.tbilisibus.data

data class Buses(val items: List<Bus>) {

    operator fun plus(rhs: Buses): Buses =
        Buses(items + rhs.items)

}
