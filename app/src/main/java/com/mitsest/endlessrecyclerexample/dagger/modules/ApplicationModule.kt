package com.mitsest.endlessrecyclerexample.dagger.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.mitsest.endlessrecyclerexample.BuildConfig
import com.mitsest.endlessrecyclerexample.PersonApiClient
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class ApplicationModule {

    @Provides
    @Singleton
    internal fun providesContext(application: Application): Context {
        return application
    }


    @Provides
    @Singleton
    fun providesMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().addInterceptor(interceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofitJson(moshi: Moshi, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://10.0.2.2:8000")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun providePersonAPiClient(retrofit: Retrofit): PersonApiClient {
        return retrofit.create(PersonApiClient::class.java)
    }


    @Provides
    @Singleton
    fun providesSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".shared_preferences", Context.MODE_PRIVATE)

}
