package kr.co.langquence.data.remote

import kr.co.langquence.model.domain.CorrectAnswer
import retrofit2.http.Body
import retrofit2.http.POST

interface CorrectApi {
	private companion object {
		const val CORRECT_API_URL: String = "correct"
	}

	@POST(CORRECT_API_URL)
	suspend fun request(@Body request: ByteArray): CorrectAnswer
}