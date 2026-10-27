package com.quno.qunobackend.interfaces.api.stack

import com.quno.qunobackend.application.stack.usecase.ParseDependencyFileUseCase
import com.quno.qunobackend.interfaces.api.question.DependencyResponse
import com.quno.qunobackend.interfaces.api.question.ParseDependencyFileRequest
import com.quno.qunobackend.interfaces.api.question.ParsedDependencyFileResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** Parses a pasted build file for the ask form's preview (Phase 63, ADR-0076). Stores nothing. */
@RestController
class StackFingerprintController(
    private val parseDependencyFileUseCase: ParseDependencyFileUseCase,
) {
    @PostMapping("/api/v1/stack-fingerprints/parse")
    fun parse(@Valid @RequestBody request: ParseDependencyFileRequest): ParsedDependencyFileResponse = parseDependencyFileUseCase.execute(request.fileName, request.content).let { parsed ->
        ParsedDependencyFileResponse(parsed.format, parsed.dependencies.map { DependencyResponse(it.ecosystem, it.name, it.version) })
    }
}
