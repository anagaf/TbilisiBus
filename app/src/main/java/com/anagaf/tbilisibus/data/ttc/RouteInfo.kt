package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.ShapePoint
import com.anagaf.tbilisibus.data.Stop

data class RouteInfo(val stops: List<Stop>, val shapePoints: List<ShapePoint>)