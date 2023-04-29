package com.anagaf.tbilisibus.ui

import com.anagaf.tbilisibus.data.Location

data class BusMarkerDescription(
    val location: Location,
    val title: String,
    val hsv_color: Float,
    val direction: Float
)
