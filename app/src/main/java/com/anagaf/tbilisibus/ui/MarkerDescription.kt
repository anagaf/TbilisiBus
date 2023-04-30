package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location

enum class MarkerType {
    Bus,
    Stop
}

data class MarkerDescription(
    val type: MarkerType,
    val location: Location,
    val title: String,
    val direction: Direction,
    val heading: Float?
)
