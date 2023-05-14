package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location

data class SituationImage(val routeNumber: Int, val markers: List<Marker>) {

    data class Marker(
        val type: Type,
        val location: Location,
        val title: String,
        val direction: Direction,
        val heading: Float?
    ) {

        enum class Type {
            Bus,
            Stop
        }
    }
}
