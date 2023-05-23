package com.anagaf.tbilisibus

import android.app.Application
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.AppDataStoreImpl
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.app.PreferencesImpl
import com.anagaf.tbilisibus.data.Buses
import com.anagaf.tbilisibus.data.RouteProvider
import com.anagaf.tbilisibus.data.Stops
import com.anagaf.tbilisibus.data.ttc.BusesResponseParser
import com.anagaf.tbilisibus.data.ttc.StopsResponseParser
import com.anagaf.tbilisibus.data.ttc.TtcRetrofitService
import com.anagaf.tbilisibus.data.ttc.TtcRouteProvider
import com.anagaf.tbilisibus.ui.SystemTimeProvider
import com.anagaf.tbilisibus.ui.TimeProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

@Module
@InstallIn(ViewModelComponent::class)
internal object ViewModelHiltModule {

    @Provides
    @ViewModelScoped
    fun provideTtcRetrofitService(): TtcRetrofitService {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(Buses::class.java, BusesResponseParser())
        objectMapperModule.addDeserializer(Stops::class.java, StopsResponseParser())
        objectMapper.registerModule(objectMapperModule)

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        return Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .baseUrl("http://transfer.ttc.com.ge:8080")
            .build()
            .create(TtcRetrofitService::class.java)
    }

    @Provides
    @ViewModelScoped
    fun provideDataProvider(ttcRetrofitService: TtcRetrofitService): RouteProvider =
        TtcRouteProvider(ttcRetrofitService)

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