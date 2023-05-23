package com.anagaf.tbilisibus

import android.app.Application
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.AppDataStoreImpl
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.app.PreferencesImpl
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.ttc.TtcRouteProvider
import com.anagaf.tbilisibus.ui.SystemTimeProvider
import com.anagaf.tbilisibus.ui.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object DaggerModule {

    @Provides
    @ViewModelScoped
    fun provideDataProvider(): RouteProvider = TtcRouteProvider()

    @Provides
    @ViewModelScoped
    fun provideDataStore(app: Application): AppDataStore =
        AppDataStoreImpl(app.applicationContext)

    @Provides
    @ViewModelScoped
    fun providePreferences(): Preferences =
        PreferencesImpl()

    @Provides
    @ViewModelScoped
    fun timeProvider(): TimeProvider =
        SystemTimeProvider()

}