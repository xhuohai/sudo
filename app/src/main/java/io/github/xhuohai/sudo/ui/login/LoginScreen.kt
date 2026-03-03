package io.github.xhuohai.sudo.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var pageTitle by remember { mutableStateOf("登录 Linux.do") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = pageTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // WebView
                LoginWebView(
                    onTitleChange = { pageTitle = it },
                    onProgressChange = { progress = it / 100f },
                    onLoadingChange = { isLoading = it },
                    onLoginSuccess = { cookie, username ->
                        scope.launch {
                            viewModel.saveLoginInfo(cookie, username)
                            onLoginSuccess()
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebView(
    onTitleChange: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onLoginSuccess: (cookie: String, username: String) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webView = this

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    // Important: Allow third-party cookies for OAuth
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Enable third-party cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        title?.let { onTitleChange(it) }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChange(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        
                        // Allow Google OAuth urls to open in the WebView
                        if (url.contains("accounts.google.com") || 
                            url.contains("github.com/login") ||
                            url.contains("linux.do")) {
                            return false
                        }
                        
                        return false
                    }
                    
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChange(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChange(false)
                        
                        // Check if login successful
                        url?.let { currentUrl ->
                            // Check if we're on the main forum page (not login or oauth)
                            if (currentUrl.startsWith("https://linux.do") && 
                                !currentUrl.contains("/login") &&
                                !currentUrl.contains("/auth/") &&
                                !currentUrl.contains("/session/")) {
                                
                                val cookieManager = CookieManager.getInstance()
                                val cookies = cookieManager.getCookie("https://linux.do")
                                
                                if (cookies != null && cookies.contains("_t=")) {
                                    // Successfully logged in, extract username
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            // Try multiple selectors to find username
                                            var selectors = [
                                                '.header-dropdown-toggle.current-user a',
                                                '.current-user a[href*="/u/"]',
                                                '[data-user-card]'
                                            ];
                                            for (var i = 0; i < selectors.length; i++) {
                                                var el = document.querySelector(selectors[i]);
                                                if (el) {
                                                    var href = el.getAttribute('href') || el.getAttribute('data-user-card');
                                                    if (href) {
                                                        var match = href.match(/\/u\/([^\/]+)/);
                                                        if (match) return match[1];
                                                    }
                                                }
                                            }
                                            return '';
                                        })()
                                        """.trimIndent()
                                    ) { username ->
                                        val cleanUsername = username.replace("\"", "").trim()
                                        if (cleanUsername.isNotEmpty() && cleanUsername != "null") {
                                            onLoginSuccess(cookies, cleanUsername)
                                        } else {
                                            // Even without username, if we have session cookie, consider logged in
                                            onLoginSuccess(cookies, "User")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Clear old cookies first to ensure fresh login
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                
                // Load login page
                loadUrl("https://linux.do/login")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
