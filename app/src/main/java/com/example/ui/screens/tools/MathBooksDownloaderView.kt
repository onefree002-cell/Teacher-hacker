package com.example.ui.screens.tools

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

const val MATH_EDU_URL = "https://mathedu03.eyoo.org/"

@Composable
fun MathBooksDownloaderView(
    onNavigateToStudyFiles: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showInAppBrowser by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("math_books_downloader_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F766E),
                                    Color(0xFF0D9488),
                                    NavyPrimary
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "بوابة تحميل كتب الرياضيات الخارجية 🌐",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "mathedu03.eyoo.org",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Text(
                            text = "مستودع ومكتبة شاملة لتحميل كافة كتب الرياضيات الخارجية (المعاصر، الأضواء، سلاح التلميذ، الشامل)، مذكرات التأسيس، ونماذج التوجيه والامتحانات لجميع المراحل بصيغة PDF مباشرة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons inside Hero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showInAppBrowser = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("open_in_app_browser_btn")
                            ) {
                                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصفح داخل التطبيق 📱", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MATH_EDU_URL))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_in_system_browser_btn")
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("المتصفح الخارجي", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Share & Copy Bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رابط الموقع المباشر:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MATH_EDU_URL,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Math Edu Books URL", MATH_EDU_URL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ رابط الموقع بنجاح 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_math_edu_url_btn")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الرابط", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "📚 موقع كتب ومذكرات الرياضيات الخارجية لجميع الصفوف الدراسية:\n$MATH_EDU_URL\n\nتمت المشاركة من تطبيق دفتر وكشكول المعلم."
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة رابط موقع الرياضيات"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر المشاركة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("share_math_edu_url_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "مشاركة الرابط", tint = Color(0xFF25D366))
                        }
                    }
                }
            }
        }

        // Available Content Categories on the website
        item {
            Text(
                text = "📚 ما يحتويه موقع mathedu03.eyoo.org:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SiteCategoryCard(
                    icon = Icons.Filled.AutoStories,
                    title = "الكتب الخارجية للرياضيات (PDF)",
                    subtitle = "سلسلة المعاصر، الأضواء، سلاح التلميذ، الشامل، الماهر لكافة المراحل",
                    color = Color(0xFF0284C7)
                )
                SiteCategoryCard(
                    icon = Icons.Filled.School,
                    title = "المراحل الدراسية كاملة",
                    subtitle = "الابتدائي (1-6)، الإعدادي (1-3)، الثانوي (1-3) عام ولغات",
                    color = Color(0xFF0D9488)
                )
                SiteCategoryCard(
                    icon = Icons.Filled.FactCheck,
                    title = "نماذج امتحانات التوجيه وبنوك الأسئلة",
                    subtitle = "امتحانات نصف ونهاية العام، اختبارات الشهور، ونماذج الوزارة الاسترشادية",
                    color = AmberGold
                )
                SiteCategoryCard(
                    icon = Icons.Filled.EditNote,
                    title = "مذكرات التأسيس والشرح التفاعلي",
                    subtitle = "ملازم إعداد نخبة من أفضل موجهي ومعلمي الرياضيات بالجمهورية",
                    color = Color(0xFF8B5CF6)
                )
            }
        }

        // Step by Step guide to import into App Library
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "كيفية الاستفادة من الكتب داخل التطبيق 💡",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    GuideStepItem(
                        step = "1",
                        title = "تصفح وحمل الكتاب من الموقع",
                        desc = "اضغط على زر التصفح، اختر الصف والمادة، واضغط تحميل لملف الـ PDF على هاتفك."
                    )
                    GuideStepItem(
                        step = "2",
                        title = "ارفع الملف في قسم (كتب ومذكرات المعلم)",
                        desc = "من قسم الكتب في التطبيق اضغط (رفع ملف جديد) ليتم حفظه بأمان في مجلد التطبيق الخاص."
                    )
                    GuideStepItem(
                        step = "3",
                        title = "استخدم أدوات الشرح والهندسة الذكية",
                        desc = "افتح الكتاب داخل التطبيق للشرح على الشاشة، الرسم بالمسطرة والفرجار والمنقلة، واستخدام آلة كاسيو الحاسبة."
                    )

                    if (onNavigateToStudyFiles != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToStudyFiles,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("go_to_study_files_from_tool")
                        ) {
                            Icon(Icons.Filled.LibraryBooks, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح مكتبة الكتب والمذكرات داخل التطبيق 📚")
                        }
                    }
                }
            }
        }
    }

    // In-App Browser Dialog (WebView)
    if (showInAppBrowser) {
        InAppMathEduBrowserDialog(
            url = MATH_EDU_URL,
            onDismiss = { showInAppBrowser = false }
        )
    }
}

@Composable
private fun SiteCategoryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    step: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(NavyPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(step, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppMathEduBrowserDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("موقع كتب الرياضيات الخارجية") }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                        }
                    },
                    actions = {
                        // Navigation controls
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "للخلف")
                        }
                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "للأمام")
                        }
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "تحديث")
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = "فتح في المتصفح الخارجي")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Loading progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = AmberGold
                    )
                }

                // WebView Container
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        url?.let { currentUrl = it }
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        url?.let { currentUrl = it }
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val requestUrl = request?.url?.toString() ?: return false
                                        currentUrl = requestUrl
                                        // If user clicks a direct download or external intent, let system handle or load
                                        if (requestUrl.endsWith(".pdf", ignoreCase = true) ||
                                            requestUrl.contains("download", ignoreCase = true) ||
                                            requestUrl.contains("drive.google.com") ||
                                            requestUrl.contains("mediafire.com")
                                        ) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                                context.startActivity(intent)
                                                Toast.makeText(context, "جارِ بدء تحميل الكتاب 📥", Toast.LENGTH_SHORT).show()
                                                return true
                                            } catch (e: Exception) {
                                                // fallback to webview loading
                                            }
                                        }
                                        return false
                                    }
                                }

                                setDownloadListener { dUrl, userAgent, contentDisposition, mimetype, contentLength ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dUrl))
                                        context.startActivity(intent)
                                        Toast.makeText(context, "جارِ بدء تحميل الملف 📥", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر تحميل الرابط", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                loadUrl(url)
                                webViewInstance = this
                            }
                        },
                        update = { webView ->
                            webViewInstance = webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
