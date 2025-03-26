package kr.co.langquence.model.usecase

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kr.co.langquence.model.domain.CorrectAnswer
import kr.co.langquence.model.domain.Resource
import kr.co.langquence.model.repository.CorrectRepository
import javax.inject.Inject

private val log = KotlinLogging.logger {}

class CorrectUseCase @Inject constructor(
    private val repository: CorrectRepository
) {
    operator fun invoke(bytes: ByteArray): Flow<Resource<CorrectAnswer>> = flow {
        try {
            emit(Resource.Loading())
            val response = repository.request(bytes)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            log.error { "Error occurred: ${e.message}" }

            emit(Resource.Error(e.message))
        }
    }.flowOn(Dispatchers.IO)
}