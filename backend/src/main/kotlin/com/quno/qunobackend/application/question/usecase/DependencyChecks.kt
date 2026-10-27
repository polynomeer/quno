package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.domain.stack.StackDependency

/** Shared by create and revise: duplicates collapse, and a request may not exceed the per-question cap (ADR-0076). */
internal fun checkedDependencies(dependencies: List<StackDependency>): List<StackDependency> {
    val distinct = dependencies.distinct()
    require(distinct.size <= StackDependency.MAX_PER_QUESTION) {
        "a question can carry at most ${StackDependency.MAX_PER_QUESTION} dependencies"
    }
    return distinct
}
