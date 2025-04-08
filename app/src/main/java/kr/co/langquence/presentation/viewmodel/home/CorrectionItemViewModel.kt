package kr.co.langquence.presentation.viewmodel.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.co.langquence.model.domain.CorrectAnswer
import kr.co.langquence.model.usecase.CorrectUseCase
import javax.inject.Inject

// @TODO: View에 보여줄 데이터로 가공해야 함.
data class CorrectionItem(
    val id: Long,
    val original: String,
    val needsCorrection: Boolean,
    val corrected: String,
    val explanation: String,
    val alternatives: List<String>
) {
    companion object {
        fun fromDomain(domain: CorrectAnswer): CorrectionItem {
            return CorrectionItem(
                id = domain.id,
                original = domain.original,
                needsCorrection = domain.needsCorrection,
                corrected = domain.corrected,
                explanation = domain.explanation,
                alternatives = domain.alternatives
            )
        }
    }
}

@HiltViewModel
class CorrectionItemViewModel @Inject constructor(
    private val correctUseCase: CorrectUseCase
) : ViewModel() {
    private val _items = MutableStateFlow<List<CorrectionItem>>(emptyList())
    val items: StateFlow<List<CorrectionItem>> = _items.asStateFlow()

    fun saveItem(item: CorrectionItem) {
        _items.value += item
    }
}