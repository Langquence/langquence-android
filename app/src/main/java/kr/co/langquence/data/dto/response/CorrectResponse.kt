package kr.co.langquence.data.dto.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kr.co.langquence.model.domain.CorrectAnswer

@Parcelize
data class CorrectResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("original")
    val original: String,
    @SerializedName("needs_correction")
    val needsCorrection: Boolean,
    @SerializedName("corrected")
    val corrected: String,
    @SerializedName("explanation")
    val explanation: String,
    @SerializedName("alternatives")
    val alternatives: List<String>,
) : Parcelable {
    fun toEntity(): CorrectAnswer = CorrectAnswer(
        id = id,
        original = original,
        needsCorrection = needsCorrection,
        corrected = corrected,
        explanation = explanation,
        alternatives = alternatives
    )
}
