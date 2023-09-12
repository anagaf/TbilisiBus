package com.anagaf.tbilisibus.data.ttc

import com.anagaf.tbilisibus.data.Direction

internal fun getForwardDirectionCode(direction: Direction): Int {
    return when (direction) {
        Direction.Forward -> 1
        Direction.Backward -> 0
    }
}
