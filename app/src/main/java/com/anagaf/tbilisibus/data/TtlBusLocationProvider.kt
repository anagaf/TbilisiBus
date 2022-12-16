package com.anagaf.tbilisibus.data

import javax.inject.Inject

class TtlBusLocationProvider @Inject constructor() : BusLocationProvider {

    override suspend fun getBusLocations(): List<BusLocation> {
        return emptyList()
    }
}