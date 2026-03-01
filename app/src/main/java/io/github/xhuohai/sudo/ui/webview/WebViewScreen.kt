package io.github.xhuohai.sudo.ui.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String = "",
    onBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var pageTitle by remember { mutableStateOf(title) }
    
    val cookie by viewModel.cookie.collectAsState()
    
    // Sync cookie
    LaunchedEffect(cookie, url) {
        if (!cookie.isNullOrEmpty()) {
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            // Extract domain from url or use linux.do
            val domain = "linux.do" // Simplified
            cookieManager.setCookie("https://$domain", cookie)
            // Also set for www just in case
            cookieManager.setCookie("https://www.$domain", cookie)
            cookieManager.flush()
        }
    }
    
    // Handle hardware back button
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = pageTitle.ifEmpty { "加载中..." },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (canGoBack) {
                        webView?.goBack()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                        contentDescription = if (canGoBack) "返回" else "关闭"
                    )
                }
            },
            actions = {
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: url))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "在浏览器中打开")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Progress bar
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        
                        // Fix for Google Login in WebView
                        // Remove "; wv" from user agent to bypass Google's "disallowed_useragent" error
                        userAgentString = userAgentString.replace("; wv", "") + " Window"
                        
                        // Support opening new windows for OAuth popups
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val reqUrl = request?.url?.toString() ?: return false
                            
                            // Let OAuth/Login flows proceed in WebView
                            if (reqUrl.contains("linux.do") || 
                                reqUrl.contains("accounts.google.com") || 
                                reqUrl.contains("github.com/login") ||
                                reqUrl.contains("auth")) {
                                return false // Load in WebView
                            }
                            
                            // Open other external links in browser
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl)))
                            return true
                        }
                        
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            canGoBack = view?.canGoBack() ?: false
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                        
                        override fun onReceivedTitle(view: WebView?, receivedTitle: String?) {
                            if (!receivedTitle.isNullOrEmpty()) {
                                pageTitle = receivedTitle
                            }
                        }
                        
                        // Handle new window requests (e.g., from target="_blank" or window.open in OAuth)
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            val newWebView = WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        this@apply.loadUrl(request?.url.toString())
                                        return true
                                    }
                                }
                            }
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = newWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }
                    
                    webView = this
                }
            },
            update = { view ->
                // Update logic if needed
            },
            modifier = Modifier.fillMaxSize()
        )
            
        // Load URL and sync cookies
        LaunchedEffect(cookie, url, webView) {
            val view = webView ?: return@LaunchedEffect
            
            if (!cookie.isNullOrEmpty()) {
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                // Linux.do specific domains
                val domain = "linux.do"
                cookieManager.setCookie("https://$domain", cookie)
                cookieManager.setCookie("https://www.$domain", cookie)
                cookieManager.flush()
            }
            
            // Only load if URL has changed or not loaded yet
            if (view.url != url) {
                 view.loadUrl(url)
            }
        }
    }
}
