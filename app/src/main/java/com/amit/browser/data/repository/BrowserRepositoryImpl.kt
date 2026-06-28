package com.amit.browser.data.repository

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.amit.browser.data.model.Bookmark
import com.amit.browser.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "browser_prefs")

@Singleton
class BrowserRepositoryImpl @Inject constructor(
    private val context: Context
) : BrowserRepository {
    
    companion object {
        private val BOOKMARKS_KEY = stringSetPreferencesKey("bookmarks")
        private val HISTORY_KEY = stringSetPreferencesKey("history")
    }
    
    override suspend fun addBookmark(bookmark: Bookmark) {
        context.dataStore.edit { preferences ->
            val currentBookmarks = preferences[BOOKMARKS_KEY] ?: emptySet()
            val newBookmark = "${bookmark.url}|${bookmark.title}|${bookmark.timestamp}"
            preferences[BOOKMARKS_KEY] = currentBookmarks + newBookmark
        }
    }
    
    override suspend fun removeBookmark(url: String) {
        context.dataStore.edit { preferences ->
            val currentBookmarks = preferences[BOOKMARKS_KEY] ?: emptySet()
            val filtered = currentBookmarks.filter { !it.startsWith("$url|") }.toSet()
            preferences[BOOKMARKS_KEY] = filtered
        }
    }
    
    override suspend fun getBookmarks(): Flow<List<Bookmark>> {
        return context.dataStore.data.map { preferences ->
            val bookmarkStrings = preferences[BOOKMARKS_KEY] ?: emptySet()
            bookmarkStrings.mapNotNull { str ->
                val parts = str.split("|")
                if (parts.size >= 3) {
                    Bookmark(
                        url = parts[0],
                        title = parts[1],
                        timestamp = parts[2].toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else null
            }
        }
    }
    
    override suspend fun isBookmarked(url: String): Boolean {
        val bookmarks = context.dataStore.data.map { preferences ->
            preferences[BOOKMARKS_KEY] ?: emptySet()
        }.first()
        return bookmarks.any { it.startsWith("$url|") }
    }
    
    override suspend fun addHistory(url: String, title: String) {
        context.dataStore.edit { preferences ->
            val currentHistory = preferences[HISTORY_KEY] ?: emptySet()
            val newEntry = "$url|$title|${System.currentTimeMillis()}"
            val updated = (currentHistory + newEntry).takeLast(100).toSet()
            preferences[HISTORY_KEY] = updated
        }
    }
    
    override suspend fun getHistory(): Flow<List<Pair<String, String>>> {
        return context.dataStore.data.map { preferences ->
            val historyStrings = preferences[HISTORY_KEY] ?: emptySet()
            historyStrings.mapNotNull { str ->
                val parts = str.split("|")
                if (parts.size >= 2) {
                    Pair(parts[0], parts[1])
                } else null
            }.reversed()
        }
    }
    
    override suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(HISTORY_KEY)
        }
    }
    
    override suspend fun clearCache() {
        WebStorage.getInstance().deleteAllData()
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }
    
    override suspend fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
    
    override suspend fun clearAllData() {
        clearCache()
        clearCookies()
        clearHistory()
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
