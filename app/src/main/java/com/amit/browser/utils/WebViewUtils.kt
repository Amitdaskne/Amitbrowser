package com.amit.browser.utils

import android.net.Uri
import java.net.URL

object WebViewUtils {
    
    fun formatUrl(input: String): String {
        var url = input.trim()
        
        // If it's already a valid URL, return it
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        // If it's a search query (contains spaces or no dots)
        if (url.contains(" ") || !url.contains(".")) {
            return "https://www.google.com/search?q=${Uri.encode(url)}"
        }
        
        // Otherwise, assume it's a domain
        return "https://$url"
    }
    
    fun shouldOverrideUrlLoading(url: Uri): Boolean {
        val scheme = url.scheme ?: return false
        val host = url.host ?: return false
        
        // Allow http and https
        if (scheme == "http" || scheme == "https") {
            return false
        }
        
        // Handle special schemes
        return when (scheme) {
            "tel", "mailto", "geo", "sms" -> true
            else -> false
        }
    }
    
    fun extractDomain(url: String): String {
        return try {
            URL(url).host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
