package kr.co.langquence.presentation.viewmodel.state

import kr.co.langquence.model.domain.CorrectAnswer

class CorrectState (
    var data: CorrectAnswer? = null,
    var error: Throwable? = null,
    var reason: String? = "An error occurred"
)