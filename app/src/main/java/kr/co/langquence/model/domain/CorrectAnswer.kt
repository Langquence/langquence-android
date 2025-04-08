package kr.co.langquence.model.domain

data class CorrectAnswer(
    val id: Long,
    val original: String,
    val needsCorrection: Boolean,
    val corrected: String,
    val explanation: String,
    val alternatives: List<String>
)