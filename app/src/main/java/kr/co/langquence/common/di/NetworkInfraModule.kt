package kr.co.langquence.common.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.langquence.infrastructure.network.interceptor.ErrorInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkInfraModule {
	@Singleton
	@Provides
	fun provideGsonBuilder(): Gson {
		return GsonBuilder().create()
	}

	@Singleton
	@Provides
	fun provideRetrofit(
		gson: Gson,
		okHttpClient: OkHttpClient
	): Retrofit.Builder {
		return Retrofit.Builder()
			.baseUrl("http://192.168.35.73:8080/api/v1/")
			.addConverterFactory(GsonConverterFactory.create(gson))
			.client(okHttpClient)
	}

	@Singleton
	@Provides
	fun provideInterceptor (
		errorInterceptor: ErrorInterceptor
	): OkHttpClient {
		val loggingInterceptor = HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BASIC
		}

		return OkHttpClient.Builder()
			.addInterceptor(loggingInterceptor)
			.addInterceptor(errorInterceptor)
			.build()
	}

	@Singleton
	@Provides
	fun provideErrorInterceptor() : Interceptor {
		return ErrorInterceptor()
	}
}