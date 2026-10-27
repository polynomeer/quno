package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "question_error_signatures")
class QuestionErrorSignatureJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Column(name = "exception_class", nullable = false)
    val exceptionClass: String,

    @Column(name = "signature_hash")
    val signatureHash: String?,

    @Column(name = "message_key", nullable = false)
    val messageKey: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
