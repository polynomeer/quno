package com.quno.qunobackend.application.stack.usecase

import com.quno.qunobackend.domain.stack.DependencyFileParser
import com.quno.qunobackend.domain.stack.ParsedDependencyFile
import org.springframework.stereotype.Service

/** Stack fingerprint preview (Phase 63, ADR-0076): parse only. The pasted file is never stored —
 * only the entries the author keeps reach the question, through create/revise. */
@Service
class ParseDependencyFileUseCase(
    private val dependencyFileParser: DependencyFileParser,
) {
    fun execute(fileName: String?, content: String): ParsedDependencyFile = dependencyFileParser.parse(fileName, content)
}
