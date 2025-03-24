package kr.co.langquence.data.repository

import kr.co.langquence.data.remote.CorrectApi
import kr.co.langquence.model.domain.CorrectAnswer
import kr.co.langquence.model.repository.CorrectRepository
import javax.inject.Inject

class CorrectRepositoryImpl @Inject constructor(
	private val api: CorrectApi
): CorrectRepository {
	override suspend fun request(body: ByteArray): CorrectAnswer = api.request(body)
}