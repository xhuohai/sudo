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
import android.webkit.JavascriptInterface
import kotlinx.coroutines.Dispatchers
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
                    onLoginSuccess = { cookie, username, avatarUrl ->
                        viewModel.saveLoginInfo(cookie, username, avatarUrl)
                        onLoginSuccess()
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun LoginWebView(
    onTitleChange: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onLoginSuccess: (cookie: String, username: String, avatarUrl: String) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webView = this

                addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun onLoginData(username: String, avatarUrl: String) {
                        scope.launch(Dispatchers.Main) {
                            val cookies = CookieManager.getInstance().getCookie("https://linux.do") ?: ""
                            if (cookies.contains("_t=")) {
                                onLoginSuccess(cookies, username, avatarUrl)
                            }
                        }
                    }
                }, "AndroidBridge")

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    javaScriptCanOpenWindowsAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    // Important: Allow third-party cookies for OAuth
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    
                    // Sneak past Cloudflare Turnstile by masking the WebView signature
                    // while keeping the exact JS engine version fingerprint unchanged.
                    userAgentString = userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
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

                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.e("WebViewConsole", "\${consoleMessage?.message()} -- From line \${consoleMessage?.lineNumber()} of \${consoleMessage?.sourceId()}")
                        return super.onConsoleMessage(consoleMessage)
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
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                        
                        // Check if login successful
                        val cleanUrl = url?.split("?")?.get(0)?.split("#")?.get(0)
                        if (cleanUrl == "https://linux.do/" || cleanUrl == "https://linux.do") {
                            val cookieManager = CookieManager.getInstance()
                            val cookies = cookieManager.getCookie("https://linux.do")
                            
                            if (cookies != null && cookies.contains("_t=")) {
                                // Extract username via JS Bridge calling current.json natively
                                // We do not need to fetch `/session/current.json` in JS (it causes 429 Too Many Requests).
                                // We just need to signal Android that login succeeded, and rely on CookieManager to have the tokens.
                                view?.evaluateJavascript(
                                    """
                                    AndroidBridge.onLoginData("User", "");
                                    """.trimIndent(), null
                                )
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
