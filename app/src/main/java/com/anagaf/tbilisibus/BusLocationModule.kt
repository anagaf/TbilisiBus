package com.anagaf.tbilisibus

import com.anagaf.tbilisibus.data.BusLocationsProvider
import com.anagaf.tbilisibus.data.ttl.TtlBusLocationsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class BusLocationModule {

    @Binds
    abstract fun bindBusLocationProvider(
        ttlBusLocationProvider: TtlBusLocationsProvider
    ): BusLocationsProvider
}