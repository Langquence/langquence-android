package kr.co.langquence.data.repository

import kr.co.langquence.model.domain.CorrectAnswer
import kr.co.langquence.model.repository.CorrectRepository
import javax.inject.Inject

class CorrectRepositoryImpl @Inject constructor(
	private val api: CorrectRepository
) {
	suspend fun request(request: ByteArray): CorrectAnswer = api.request(request)
}