package com.extacia.portfolio.dto

import com.extacia.portfolio.model.Repository
import java.time.format.DateTimeFormatter

data class RepositoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val url: String,
    val homepage: String?,
    val topics: List<String>,
    val updatedAt: String,
    val languages: Map<String, Long>,
    val isArchived: Boolean,
    val isFork: Boolean,
    val cloneUrl: String,
    val hasIssues: Boolean,
    val size: String?
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

        fun fromRepository(repo: Repository): RepositoryResponse {
            // Calculate total size safely
            val totalBytes = repo.languages.values.sum()

            return RepositoryResponse(
                id = repo.id,
                name = repo.name,
                description = repo.description ?: "No description available",
                language = repo.language ?: "Unknown",
                stars = repo.stars,
                forks = repo.forks,
                url = repo.htmlUrl,
                homepage = repo.homepage,
                topics = repo.topics,
                updatedAt = repo.updatedAt.format(formatter),
                languages = repo.languages,
                isArchived = repo.isArchived,
                isFork = repo.isFork,
                cloneUrl = repo.cloneUrl,
                hasIssues = repo.openIssues > 0,
                size = formatSize(totalBytes)
            )
        }

        private fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}
