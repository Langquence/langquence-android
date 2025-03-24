package kr.co.langquence.data.repository

import kr.co.langquence.data.remote.CorrectApi
import kr.co.langquence.model.domain.CorrectAnswer
import javax.inject.Inject

class CorrectRepositoryImpl @Inject constructor(
	private val api: CorrectApi
) {
	suspend fun request(request: ByteArray): CorrectAnswer = api.request(request)
}