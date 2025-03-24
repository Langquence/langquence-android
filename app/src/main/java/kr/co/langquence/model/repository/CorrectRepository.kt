package kr.co.langquence.model.repository

import kr.co.langquence.model.domain.CorrectAnswer
import retrofit2.http.Body
import retrofit2.http.POST

interface CorrectRepository {
	private companion object {
		const val CORRECT_API_URL: String = "correct"
	}

	@POST(CORRECT_API_URL)
	suspend fun request(@Body request: ByteArray): CorrectAnswer
}