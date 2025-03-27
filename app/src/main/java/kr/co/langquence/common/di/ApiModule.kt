package kr.co.langquence.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.langquence.data.remote.CorrectApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Singleton
    @Provides
    fun provideCorrectApi(retrofit: Retrofit.Builder): CorrectApi {
        return retrofit
            .build()
            .create(CorrectApi::class.java)
    }
}