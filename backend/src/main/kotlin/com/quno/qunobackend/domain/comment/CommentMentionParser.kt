package com.quno.qunobackend.domain.comment

/** Extracts `@nickname` tokens from a comment body (ADR-0031 #3). Only exact matches on
 * alphanumeric/underscore/hyphen nicknames are found — a nickname containing spaces or other
 * punctuation cannot be mentioned this way, since sign-up places no format constraint on
 * nicknames. Parsing happens at comment creation only, never on edit. */
object CommentMentionParser {
    private val MENTION_PATTERN = Regex("""@([\w-]+)""")

    fun parseNicknames(body: String): Set<String> =
        MENTION_PATTERN.findAll(body).map { it.groupValues[1] }.toSet()
}
