package com.extacia.portfolio.model

import java.time.LocalDateTime

data class Repository(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int,  // GitHub returns these as Int
    val forks: Int,
    val openIssues: Int,
    val watchers: Int,
    val isArchived: Boolean,
    val isFork: Boolean,
    val isPrivate: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val pushedAt: LocalDateTime?,
    val cloneUrl: String,
    val htmlUrl: String,
    val homepage: String?,
    val topics: List<String>,
    val languages: Map<String, Long>  // Language bytes can be Long
)
