package com.lu4p.fokuslauncher.utils

/**
 * Drawer "dot search" syntax: `. <query>` for default web search, `.<c> <query>` for a
 * single-character alias.
 */
sealed interface DotSearchParsed {
    data class Default(val searchText: String) : DotSearchParsed

    data class Alias(val aliasChar: Char, val searchText: String) : DotSearchParsed
}

object DotSearchSyntax {
    private val aliasPattern = Regex("^\\.([a-z])\\s+(.+)$")
    private val defaultPattern = Regex("^\\.\\s+(.+)$")

    /**
     * Parses [trimmedQuery] after [String.trimStart] has been applied (or equivalent). Returns
     * null if the text does not match dot-search grammar.
     */
    fun parse(trimmedQuery: String): DotSearchParsed? {
        if (!trimmedQuery.startsWith('.')) return null
        aliasPattern.matchEntire(trimmedQuery)?.let { match ->
            val char = match.groupValues[1].single()
            val text = match.groupValues[2].trim()
            if (text.isNotEmpty()) return DotSearchParsed.Alias(char, text)
        }
        defaultPattern.matchEntire(trimmedQuery)?.let { match ->
            val text = match.groupValues[1].trim()
            if (text.isNotEmpty()) return DotSearchParsed.Default(text)
        }
        return null
    }

    /** True when [query] might still become a valid dot-search (user still typing). */
    fun isPossibleDotSearchPrefix(trimmedQuery: String): Boolean = trimmedQuery.startsWith('.')
}
