package com.quno.qunobackend.infrastructure.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.quno.qunobackend.domain.directask.ConfirmedPayment
import com.quno.qunobackend.domain.directask.PaymentConfirmationFailedException
import com.quno.qunobackend.domain.directask.PaymentGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Base64

/**
 * Talks to Toss Payments' Core API (Phase 25, ADR-0037) — confirm/cancel only, no webhooks (the
 * redirect+confirm flow is sufficient for card payments, this app's only payment method). Auth is
 * HTTP Basic with the secret key as username and an empty password, base64-encoded — Toss's own
 * documented scheme (`secretKey:` including the trailing colon).
 *
 * Bodies are serialized/parsed by hand with the injected [ObjectMapper] instead of letting
 * `RestClient.body(Any)`/`.body(Class)` pick a message converter — `RestClient.builder()`'s
 * static factory doesn't attach this project's Jackson 3 (`tools.jackson`) converter, so a
 * request body built that way silently serialized as `{}` (caught by a local mock Toss server
 * during verification, see ADR-0037). Passing raw JSON strings sidesteps converter selection
 * entirely and reuses the exact ObjectMapper already proven elsewhere in this codebase (e.g.
 * SpikeDetectionRepositoryAdapter's Redis cache).
 */
@Component
class TossPaymentGateway(
    private val objectMapper: ObjectMapper,
    @Value("\${quno.toss.secret-key}") secretKey: String,
    @Value("\${quno.toss.api-base-url}") apiBaseUrl: String,
) : PaymentGateway {

    private val restClient = RestClient.builder()
        .baseUrl(apiBaseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("$secretKey:".toByteArray()))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(5_000)
                setReadTimeout(10_000)
            },
        )
        .build()

    override fun confirm(paymentKey: String, orderId: String, amount: Long): ConfirmedPayment {
        val requestBody = objectMapper.writeValueAsString(TossConfirmRequest(paymentKey, orderId, amount))
        val responseBody = try {
            restClient.post().uri("/v1/payments/confirm").body(requestBody).retrieve().body(String::class.java)
        } catch (e: RestClientResponseException) {
            throw PaymentConfirmationFailedException("Toss confirm failed: ${e.statusCode} ${e.responseBodyAsString}")
        } ?: throw PaymentConfirmationFailedException("Toss confirm returned an empty body")

        val response = objectMapper.readValue(responseBody, TossConfirmResponse::class.java)
        return ConfirmedPayment(
            paymentKey = response.paymentKey,
            orderId = response.orderId,
            totalAmount = response.totalAmount,
            approvedAt = Instant.parse(response.approvedAt),
        )
    }

    override fun cancel(paymentKey: String, reason: String) {
        val requestBody = objectMapper.writeValueAsString(TossCancelRequest(reason))
        try {
            restClient.post().uri("/v1/payments/{paymentKey}/cancel", paymentKey).body(requestBody).retrieve().toBodilessEntity()
        } catch (e: RestClientResponseException) {
            throw PaymentConfirmationFailedException("Toss cancel failed: ${e.statusCode} ${e.responseBodyAsString}")
        }
    }
}

private data class TossConfirmRequest(val paymentKey: String, val orderId: String, val amount: Long)

private data class TossCancelRequest(val cancelReason: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TossConfirmResponse(val paymentKey: String, val orderId: String, val totalAmount: Long, val approvedAt: String)
