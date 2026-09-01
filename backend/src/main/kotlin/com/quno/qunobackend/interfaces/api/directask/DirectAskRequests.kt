package com.quno.qunobackend.interfaces.api.directask

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateDirectAskRequest(
    @field:NotNull
    val targetUserId: Long,
    @field:Size(max = 1000)
    val message: String? = null,
)

data class ConfirmDirectAskPaymentRequest(
    @field:NotBlank
    val orderId: String,
    @field:NotBlank
    val paymentKey: String,
    @field:NotNull @field:Positive
    val amount: Long,
)
