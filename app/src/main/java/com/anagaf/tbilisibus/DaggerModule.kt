package com.anagaf.tbilisibus

import android.app.Application
import com.anagaf.tbilisibus.data.SituationProvider
import com.anagaf.tbilisibus.data.ttc.TtcSituationProvider
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
    fun provideDataProvider(): SituationProvider = TtcSituationProvider()

    @Provides
    @ViewModelScoped
    fun providePreferences(app: Application): Preferences =
        AndroidPreferences(app.applicationContext)
}