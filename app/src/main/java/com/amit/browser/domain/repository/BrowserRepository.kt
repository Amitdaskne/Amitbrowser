package com.amit.browser.domain.repository

import com.amit.browser.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BrowserRepository {
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun removeBookmark(url: String)
    suspend fun getBookmarks(): Flow<List<Bookmark>>
    suspend fun isBookmarked(url: String): Boolean
    
    suspend fun addHistory(url: String, title: String)
    suspend fun getHistory(): Flow<List<Pair<String, String>>>
    suspend fun clearHistory()
    
    suspend fun clearCache()
    suspend fun clearCookies()
    suspend fun clearAllData()
}
