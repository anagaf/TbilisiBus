package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Direction
import com.anagaf.tbilisibus.data.Location

data class BusMarkerDescription(
    val location: Location,
    val title: String,
    val direction: Direction,
    val heading: Float?
)
