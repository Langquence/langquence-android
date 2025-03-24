package kr.co.langquence.data.remote

import kr.co.langquence.data.dto.response.CorrectResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CorrectApi {
	private companion object {
		const val CORRECT_API_URL: String = "correct"
	}

	@POST(CORRECT_API_URL)
	@Headers(
		"Content-Type: application/octet-stream",
		"Accept: application/json"
	)
	suspend fun request(@Body request: ByteArray): CorrectResponse
}