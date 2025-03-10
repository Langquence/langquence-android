package kr.co.langquence.presentation.viewmodel.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _isListeningMode = MutableStateFlow(false)
    val isListeningMode: StateFlow<Boolean> = _isListeningMode.asStateFlow()

    fun toggleListeningMode() {
        _isListeningMode.value = !_isListeningMode.value
    }

    fun startVoiceRecognition() {
        _isListeningMode.value = true
    }

    fun stopVoiceRecognition() {
        _isListeningMode.value = false
    }
}