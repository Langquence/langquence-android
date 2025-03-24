package kr.co.langquence.infrastructure.network.interceptor

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import kr.co.langquence.infrastructure.network.NetworkException
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject

private val log = KotlinLogging.logger {}

@Module
@InstallIn(SingletonComponent::class)
class ErrorInterceptor @Inject constructor() : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())

		if (!response.isSuccessful) {
			log.info { "Request failed: ${response.code}" }
			log.info { "Request failed: ${response.message}" }

			throw NetworkException(response.code, response.message)
		}

		response.extractResponseJson()

		return response
	}

	private fun Response.extractResponseJson(): JSONObject {
		val jsonString = this.peekBody(Long.MAX_VALUE).string()
		return try {
			JSONObject(jsonString)
		} catch (e: Exception) {
			log.info { "not json response: $jsonString" }

			throw NetworkException(999, "not json type")
		}
	}
}