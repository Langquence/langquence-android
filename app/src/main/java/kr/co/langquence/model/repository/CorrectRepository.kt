package kr.co.langquence.model.repository

import kr.co.langquence.model.domain.CorrectAnswer

interface CorrectRepository {
	suspend fun request(body: ByteArray): CorrectAnswer
}