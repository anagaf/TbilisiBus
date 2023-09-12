package com.anagaf.tbilisibus

import android.annotation.SuppressLint
import android.app.Application
import com.anagaf.tbilisibus.app.AppDataStore
import com.anagaf.tbilisibus.app.AppDataStoreImpl
import com.anagaf.tbilisibus.app.Preferences
import com.anagaf.tbilisibus.app.PreferencesImpl
import com.anagaf.tbilisibus.data.Bus
import com.anagaf.tbilisibus.data.RouteInfoCacheImpl
import com.anagaf.tbilisibus.data.RouteRepository
import com.anagaf.tbilisibus.data.RouteRepositoryImpl
import com.anagaf.tbilisibus.data.ttc.BusesResponseParser
import com.anagaf.tbilisibus.data.ttc.DirectionBuses
import com.anagaf.tbilisibus.data.ttc.RouteInfoResponseParser
import com.anagaf.tbilisibus.data.ttc.TtcRetrofitService
import com.anagaf.tbilisibus.data.ttc.TtcBusesDataSource
import com.anagaf.tbilisibus.data.ttc.TtcRouteInfoDataSource
import com.anagaf.tbilisibus.ui.LocationProvider
import com.anagaf.tbilisibus.ui.SystemLocationProvider
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@Module
@InstallIn(ViewModelComponent::class)
internal object ViewModelHiltModule {

    @Provides
    @ViewModelScoped
    fun provideTtcRetrofitService(): TtcRetrofitService {
        val objectMapper = ObjectMapper()

        val objectMapperModule = SimpleModule()
        objectMapperModule.addDeserializer(DirectionBuses::class.java, BusesResponseParser())
        objectMapperModule.addDeserializer(
            com.anagaf.tbilisibus.data.ttc.DirectionRouteInfo::class.java,
            RouteInfoResponseParser()
        )
        objectMapper.registerModule(objectMapperModule)

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC

        // trusting all certificates is a bit dangerous but it's ok for this app
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val httpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .addInterceptor(interceptor)
            .build()

        return Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
//            .baseUrl("http://transfer.ttc.com.ge:8080")
            .baseUrl("https://transfer.msplus.ge:2443")
            .build()
            .create(TtcRetrofitService::class.java)
    }

    @Provides
    @ViewModelScoped
    fun provideRouteRepository(ttcRetrofitService: TtcRetrofitService): RouteRepository {
        val busesDataSource = TtcBusesDataSource(ttcRetrofitService)
        val routeInfoDataSource = TtcRouteInfoDataSource(ttcRetrofitService)
        val routeInfoCache = RouteInfoCacheImpl()
        return RouteRepositoryImpl(busesDataSource, routeInfoDataSource, routeInfoCache)
    }

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

    @Provides
    @ViewModelScoped
    fun locationProvider(app: Application): LocationProvider =
        SystemLocationProvider(app.applicationContext)

}