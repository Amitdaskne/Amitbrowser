package com.amit.browser.utils

import android.net.Uri
import java.net.URL

object WebViewUtils {
    
    fun formatUrl(input: String): String {
        var url = input.trim()
        
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        if (url.contains(" ") || !url.contains(".")) {
            return "https://www.google.com/search?q=${Uri.encode(url)}"
        }
        
        return "https://$url"
    }
    
    fun shouldOverrideUrlLoading(url: Uri): Boolean {
        val scheme = url.scheme ?: return false
        val host = url.host ?: return false
        
        if (scheme == "http" || scheme == "https") {
            return false
        }
        
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
