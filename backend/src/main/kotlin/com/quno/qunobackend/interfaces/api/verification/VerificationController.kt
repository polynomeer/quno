package com.quno.qunobackend.interfaces.api.verification

import com.quno.qunobackend.application.verification.EvidenceCard
import com.quno.qunobackend.application.verification.QuestionVerificationService
import com.quno.qunobackend.application.verification.VerificationResult
import com.quno.qunobackend.domain.verification.QuestionClassification
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Verification levels and Evidence Cards (Phase 94, ADR-0089). Under the question visibility gate (ADR-0085). */
@RestController
@RequestMapping("/api/v1/questions/{id}")
class VerificationController(
    private val service: QuestionVerificationService,
) {
    @GetMapping("/verification")
    fun get(@PathVariable id: Long): VerificationResult = service.get(id)

    @PutMapping("/verification/classification")
    fun classify(
        @AuthenticationPrincipal actorId: Long,
        @PathVariable id: Long,
        @RequestBody request: ClassifyQuestionRequest,
    ): VerificationResult = service.classify(id, actorId, request.classification)

    @GetMapping("/evidence-card")
    fun evidenceCard(@PathVariable id: Long): EvidenceCard = service.evidenceCard(id)
}

data class ClassifyQuestionRequest(val classification: QuestionClassification? = null)
