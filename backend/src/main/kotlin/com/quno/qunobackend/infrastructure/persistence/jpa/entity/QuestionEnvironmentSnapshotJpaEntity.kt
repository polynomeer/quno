package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "question_environment_snapshots")
class QuestionEnvironmentSnapshotJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "question_id", nullable = false, unique = true) val questionId: Long,
    @Column(name = "version_number", nullable = false) val versionNumber: Int,
    @Column(name = "schema_version", nullable = false) val schemaVersion: Int,
    @Column(name = "source", nullable = false) val source: String,
    @Column(name = "collector") val collector: String?,
    @Column(name = "fingerprint", nullable = false) val fingerprint: String,
    @Column(name = "server_redactions", nullable = false) val serverRedactions: Int,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document", nullable = false)
    val document: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
)
