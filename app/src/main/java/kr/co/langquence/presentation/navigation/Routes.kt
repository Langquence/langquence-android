package kr.co.langquence.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * 앱 내 네비게이션 경로 정의
 */
object Routes {
    @Serializable
    object Home

    @Serializable
    object Profile

    @Serializable
    object Correction
}