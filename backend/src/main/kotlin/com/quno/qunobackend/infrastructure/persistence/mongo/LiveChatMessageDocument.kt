package com.quno.qunobackend.infrastructure.persistence.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/** First real MongoDB usage in this codebase (Phase 24, ADR-0036) — see LiveChatMessage's kdoc
 * for why chat messages are the natural fit for the document store this project already
 * provisions but had never used. */
@Document(collection = "live_chat_messages")
class LiveChatMessageDocument(
    @Id
    val id: String? = null,

    @Indexed
    val roomId: Long,

    val senderId: Long,
    val body: String,
    val createdAt: Instant,
)
