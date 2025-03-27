package kr.co.langquence.data.repository

import kr.co.langquence.data.remote.CorrectApi
import kr.co.langquence.model.domain.CorrectAnswer
import kr.co.langquence.model.repository.CorrectRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class CorrectRepositoryImpl @Inject constructor(
    private val api: CorrectApi
) : CorrectRepository {
    override suspend fun request(body: ByteArray): CorrectAnswer {
        val requestBody = body.toRequestBody("application/octet-stream".toMediaTypeOrNull())

        return api.request(requestBody).toEntity()
    }
}