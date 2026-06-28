package com.amit.browser.presentation.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amit.browser.R
import com.amit.browser.presentation.ui.components.BrowserToolbar
import com.amit.browser.presentation.ui.components.TabSwitcher
import com.amit.browser.presentation.ui.viewmodel.BrowserViewModel
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltNavGraphViewModels()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val url by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.loadProgress.collectAsState()
    val title by viewModel.currentTitle.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val currentTabId by viewModel.currentTabId.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val showTabSwitcher by viewModel.showTabSwitcher.collectAsState()
    
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    val microphonePermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                    webView?.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView?.onResume()
                    webView?.resumeTimers()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webView?.destroy()
                    webView = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrowserToolbar(
                url = url,
                title = title,
                isLoading = isLoading,
                isIncognito = isIncognito,
                isBookmarked = isBookmarked,
                onUrlSubmit = { newUrl ->
                    viewModel.navigateToUrl(newUrl)
                },
                onBackClick = {
                    webView?.goBack()
                },
                onForwardClick = {
                    webView?.goForward()
                },
                onReloadClick = {
                    webView?.reload()
                },
                onBookmarkClick = {
                    viewModel.toggleBookmark()
                },
                onTabSwitcherClick = {
                    viewModel.toggleTabSwitcher()
                },
                onNewTabClick = {
                    viewModel.createNewTab()
                },
                onMenuClick = {
                    // Show menu
                }
            )
            
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                setGeolocationEnabled(true)
                            }
                            
                            webViewClient = viewModel.createWebViewClient(ctx)
                            webChromeClient = viewModel.createWebChromeClient(
                                context = ctx,
                                onRequestPermission = { permissions ->
                                    permissions.forEach { permission ->
                                        when (permission) {
                                            Manifest.permission.CAMERA -> cameraPermissionState.launchPermissionRequest()
                                            Manifest.permission.RECORD_AUDIO -> microphonePermissionState.launchPermissionRequest()
                                            Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionState.launchPermissionRequest()
                                        }
                                    }
                                }
                            )
                            
                            if (url.isNotEmpty()) {
                                loadUrl(url)
                            } else {
                                loadUrl("about:blank")
                            }
                            
                            webView = this
                        }
                    },
                    update = { view ->
                        // Update webview if needed
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
        
        if (showTabSwitcher) {
            TabSwitcher(
                tabs = tabs,
                currentTabId = currentTabId,
                onTabSelected = { tabId ->
                    viewModel.selectTab(tabId)
                },
                onTabClosed = { tabId ->
                    viewModel.closeTab(tabId)
                },
                onNewTabClick = {
                    viewModel.createNewTab()
                },
                onDismiss = {
                    viewModel.toggleTabSwitcher()
                }
            )
        }
    }
}
