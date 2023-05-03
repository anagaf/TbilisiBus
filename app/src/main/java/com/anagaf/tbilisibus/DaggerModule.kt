package com.anagaf.tbilisibus

import android.app.Application
import com.anagaf.tbilisibus.data.DataProvider
import com.anagaf.tbilisibus.data.ttc.TtcDataProvider
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
    fun provideDataProvider(): DataProvider = TtcDataProvider()

    @Provides
    @ViewModelScoped
    fun providePreferences(app: Application): Preferences =
        AndroidPreferences(app.applicationContext)
}