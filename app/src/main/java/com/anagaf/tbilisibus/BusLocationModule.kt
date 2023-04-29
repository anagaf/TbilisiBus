package com.anagaf.tbilisibus

import com.anagaf.tbilisibus.data.BusesOnRouteProvider
import com.anagaf.tbilisibus.data.ttl.TtlBusesOnRouteProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class BusLocationModule {

    @Binds
    abstract fun bindBusLocationProvider(
        ttlBusLocationProvider: TtlBusesOnRouteProvider
    ): BusesOnRouteProvider
}