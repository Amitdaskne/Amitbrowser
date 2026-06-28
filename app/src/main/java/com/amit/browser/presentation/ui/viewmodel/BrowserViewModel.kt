package com.amit.browser.presentation.ui.viewmodel

import android.content.Context
import android.os.Build
import android.webkit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.amit.browser.data.model.Tab
import com.amit.browser.data.model.Bookmark
import com.amit.browser.domain.repository.BrowserRepository
import com.amit.browser.utils.WebViewUtils
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repository: BrowserRepository
) : ViewModel() {
    
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    
    private val _currentTabId = MutableStateFlow<String?>(null)
    val currentTabId: StateFlow<String?> = _currentTabId.asStateFlow()
    
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()
    
    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()
    
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()
    
    private val _showTabSwitcher = MutableStateFlow(false)
    val showTabSwitcher: StateFlow<Boolean> = _showTabSwitcher.asStateFlow()
    
    init {
        loadInitialTab()
    }
    
    private fun loadInitialTab() {
        viewModelScope.launch {
            val newTab = Tab(
                id = System.currentTimeMillis().toString(),
                url = "https://www.google.com",
                title = "New Tab",
                isIncognito = false
            )
            _tabs.value = listOf(newTab)
            _currentTabId.value = newTab.id
            _currentUrl.value = newTab.url
            _currentTitle.value = newTab.title
        }
    }
    
    fun navigateToUrl(url: String) {
        viewModelScope.launch {
            val formattedUrl = WebViewUtils.formatUrl(url)
            _currentUrl.value = formattedUrl
            
            val currentTab = _tabs.value.find { it.id == _currentTabId.value }
            if (currentTab != null) {
                val updatedTab = currentTab.copy(
                    url = formattedUrl,
                    title = formattedUrl
                )
                _tabs.value = _tabs.value.map {
                    if (it.id == updatedTab.id) updatedTab else it
                }
            }
            
            checkBookmarkStatus(formattedUrl)
        }
    }
    
    fun createNewTab(url: String = "https://www.google.com", incognito: Boolean = false) {
        viewModelScope.launch {
            val newTab = Tab(
                id = System.currentTimeMillis().toString(),
                url = url,
                title = "New Tab",
                isIncognito = incognito
            )
            _tabs.value = _tabs.value + newTab
            selectTab(newTab.id)
        }
    }
    
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val updatedTabs = _tabs.value.filter { it.id != tabId }
            if (updatedTabs.isEmpty()) {
                createNewTab()
            } else {
                _tabs.value = updatedTabs
                if (tabId == _currentTabId.value) {
                    selectTab(updatedTabs.last().id)
                }
            }
        }
    }
    
    fun selectTab(tabId: String) {
        viewModelScope.launch {
            val tab = _tabs.value.find { it.id == tabId }
            if (tab != null) {
                _currentTabId.value = tabId
                _currentUrl.value = tab.url
                _currentTitle.value = tab.title
                _isIncognito.value = tab.isIncognito
                _showTabSwitcher.value = false
            }
        }
    }
    
    fun toggleTabSwitcher() {
        _showTabSwitcher.value = !_showTabSwitcher.value
    }
    
    fun toggleBookmark() {
        viewModelScope.launch {
            if (_isBookmarked.value) {
                repository.removeBookmark(_currentUrl.value)
                _isBookmarked.value = false
            } else {
                val bookmark = Bookmark(
                    url = _currentUrl.value,
                    title = _currentTitle.value,
                    timestamp = System.currentTimeMillis()
                )
                repository.addBookmark(bookmark)
                _isBookmarked.value = true
            }
        }
    }
    
    private fun checkBookmarkStatus(url: String) {
        viewModelScope.launch {
            _isBookmarked.value = repository.isBookmarked(url)
        }
    }
    
    fun createWebViewClient(context: Context): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                _isLoading.value = true
                if (url != null) {
                    _currentUrl.value = url
                }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                _isLoading.value = false
                _loadProgress.value = 1f
                if (url != null) {
                    _currentTitle.value = view?.title ?: url
                    val currentTab = _tabs.value.find { it.id == _currentTabId.value }
                    if (currentTab != null) {
                        val updatedTab = currentTab.copy(
                            title = view?.title ?: url
                        )
                        _tabs.value = _tabs.value.map {
                            if (it.id == updatedTab.id) updatedTab else it
                        }
                    }
                }
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let { url ->
                    return WebViewUtils.shouldOverrideUrlLoading(url)
                }
                return false
            }
            
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                _isLoading.value = false
            }
        }
    }
    
    fun createWebChromeClient(
        context: Context,
        onRequestPermission: (Array<String>) -> Unit
    ): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                _loadProgress.value = newProgress / 100f
            }
            
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    _currentTitle.value = it
                }
            }
            
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    onRequestPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }
                callback?.invoke(origin, true, false)
            }
            
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let {
                    val resources = it.resources
                    if (resources != null) {
                        onRequestPermission(resources)
                    }
                }
            }
        }
    }
}
