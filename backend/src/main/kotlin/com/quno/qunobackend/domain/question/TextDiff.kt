package com.quno.qunobackend.domain.question

enum class DiffLineType { EQUAL, ADDED, REMOVED }

data class DiffLine(val type: DiffLineType, val text: String)

/**
 * Line-level LCS diff (see vision.md "Qv1과 Qv2의 Diff를 보여줍니다").
 * O(n*m) time/space — fine for question-body-sized text, not meant for huge documents.
 */
object TextDiffer {

    fun diffLines(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        val n = oldLines.size
        val m = newLines.size

        val lcsLength = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcsLength[i][j] = if (oldLines[i] == newLines[j]) {
                    lcsLength[i + 1][j + 1] + 1
                } else {
                    maxOf(lcsLength[i + 1][j], lcsLength[i][j + 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                oldLines[i] == newLines[j] -> {
                    result += DiffLine(DiffLineType.EQUAL, oldLines[i])
                    i++
                    j++
                }
                lcsLength[i + 1][j] >= lcsLength[i][j + 1] -> {
                    result += DiffLine(DiffLineType.REMOVED, oldLines[i])
                    i++
                }
                else -> {
                    result += DiffLine(DiffLineType.ADDED, newLines[j])
                    j++
                }
            }
        }
        while (i < n) {
            result += DiffLine(DiffLineType.REMOVED, oldLines[i])
            i++
        }
        while (j < m) {
            result += DiffLine(DiffLineType.ADDED, newLines[j])
            j++
        }
        return result
    }
}
