package com.anagaf.tbilisibus.data

data class Stops(val items: List<Stop>) {

    operator fun plus(rhs: Stops): Stops =
        Stops(items + rhs.items)
}
