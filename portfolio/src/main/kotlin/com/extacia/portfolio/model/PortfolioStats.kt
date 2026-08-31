package com.extacia.portfolio.model

data class PortfolioStats(
    val totalRepos: Int,
    val totalStars: Int,
    val totalForks: Int,
    val totalWatchers: Int,
    val languages: Map<String, Int>,  // Changed to Int
    val topLanguages: List<LanguageStats>,
    val mostStarredRepo: String?,
    val mostForkedRepo: String?,
    val recentlyUpdated: List<String>,
    val repoCountByType: RepoTypeCounts
)

data class LanguageStats(
    val name: String,
    val count: Int,
    val percentage: Double
)

data class RepoTypeCounts(
    val public: Int,
    val private: Int,
    val forked: Int,
    val archived: Int
)
