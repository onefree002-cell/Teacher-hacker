package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.TeacherEntity
import com.example.util.QrBarcodeUtils

@Composable
fun TeacherProfileShareDialog(
    teacher: TeacherEntity,
    onDismiss: () -> Unit,
    onEditProfileRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }

    val cleanWhatsapp = teacher.whatsapp.ifEmpty { teacher.phone }.replace("+", "").replace(" ", "")
    val waNumber = if (cleanWhatsapp.startsWith("0")) "2$cleanWhatsapp" else cleanWhatsapp
    val directWhatsappLink = "https://wa.me/$waNumber?text=${Uri.encode("السلام عليكم أستاذ ${teacher.name}، أود الاستفسار عن تفاصيل ومواعيد المجموعات الدراسية.")}"

    // Generate real scannable QR Code
    val qrBitmap = remember(teacher) {
        try {
            QrBarcodeUtils.generateQrBitmap(
                content = directWhatsappLink,
                size = 320,
                darkColor = android.graphics.Color.rgb(15, 23, 42),
                lightColor = android.graphics.Color.WHITE
            )
        } catch (_: Exception) {
            null
        }
    }

    val formattedShareText = remember(teacher) {
        """
        ✨ *بطاقة تعريفية بالأستاذ / ${teacher.name}* ✨
        📚 *المادة:* ${teacher.subject}
        🎖️ *المسمى:* ${teacher.title.ifEmpty { "أستاذ المادة والمشرف الأكاديمي" }}
        ⭐ *الخبرة والمؤهلات:* ${teacher.experienceYears.ifEmpty { "خبرة سنوات طويلة من التميز" }} • ${teacher.degrees.ifEmpty { "بكالوريوس ودبلوم تربوي" }}
        
        🎯 *المراحل والمناهج الدراسية:*
        ${teacher.stagesTaught.ifEmpty { "المرحلة الثانوية والإعدادية (عام ولغات)" }}
        
        🏢 *السنتر والمقر الرئيسي:*
        ${teacher.centerName.ifEmpty { "سنتر التفوق" }} - ${teacher.address.ifEmpty { "المقر الرئيسي" }}
        
        🏆 *مميزات نظام التدريس والمتابعة:*
        ${teacher.teachingFeatures.ifEmpty { "• متابعة أسبوعية دقيقة للحضور والواجبات\n• امتحانات دورية وبنك أسئلة شامل\n• تقارير إلكترونية دورية لولي الأمر" }}
        
        📞 *للحجز والاستفسار والتواصل المباشر:*
        📱 اتصال: ${teacher.phone}
        💬 واتساب: https://wa.me/$waNumber
        
        ✨ _معاً نحو القمة والتفوق الدراسي بإذن الله_ ✨
        """.trimIndent()
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label بنجاح! 📋", Toast.LENGTH_SHORT).show()
    }

    fun shareTextViaWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(intent, "مشاركة بيانات المعلم عبر")
            context.startActivity(chooser)
        }
    }

    fun exportAndSharePortfolioPdf() {
        isGeneratingPdf = true
        try {
            val pdfFile = PdfReportExporter.generateTeacherPortfolioPdf(context, teacher)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "فتح ومشاركة بورتفوليو المعلم PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء إنشاء الملف: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isGeneratingPdf = false
        }
    }

    fun exportAndShareBusinessCardsPdf() {
        isGeneratingPdf = true
        try {
            val pdfFile = PdfReportExporter.generateTeacherBusinessCardsSheetPdf(context, teacher)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "فتح ومشاركة شيت كروت العمل PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء إنشاء الملف: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isGeneratingPdf = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("teacher_profile_share_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "كارت وبورتفوليو المعلم التعريفي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "مشاركة البيانات بصيغ راقية ومنسقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Luxury Visual Preview Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0F172A), // Luxury Dark Navy
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFDE68A))), RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    teacher.centerName.ifEmpty { "أكاديمية التفوق التعليمية" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFDE68A)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "الأستاذ / ${teacher.name}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "${teacher.title.ifEmpty { "أستاذ المادة والمشرف الأكاديمي" }} • ${teacher.subject}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                "⭐ ${teacher.experienceYears.ifEmpty { "خبرة سنوات طويلة من التميز" }} ⭐",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Scannable QR Code Box
                            if (qrBitmap != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .padding(4.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "رمز QR للمعلم",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "امسح الـ QR للتواصل المباشر عبر واتساب",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Contact Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable { copyToClipboard(teacher.phone, "رقم الهاتف") }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(teacher.phone, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable { copyToClipboard(teacher.whatsapp, "رقم الواتساب") }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(teacher.whatsapp, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Grid
                    Text(
                        "خيارات المشاركة والطباعة الفورية:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 1. Full A4 Portfolio Flyer PDF
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportAndSharePortfolioPdf() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "بروفايل وبورتفوليو المعلم PDF (ورقة A4 فاخرة)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "تصميم ملكي مزخرف بالـ QR للمشاركة مع أولياء الأمور",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 2. Printable Business Cards Sheet (8 cards per A4 page)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportAndShareBusinessCardsPdf() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Style, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "شيت كروت العمل الشخصية (8 كروت للقص)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        "جاهزة للطباعة والتوزيع على الطلاب وأولياء الأمور",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            Icon(Icons.Filled.Print, contentDescription = null, tint = Color(0xFFD97706))
                        }
                    }

                    // 3. Share Formatted WhatsApp Text Bio
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shareTextViaWhatsApp(formattedShareText) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF059669),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "مشاركة رسالة الواتساب التعريفية المنسقة",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        "نص أنيق بجميع التفاصيل والروابط للجروبات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF059669))
                        }
                    }

                    // Copy Text & Copy Link Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copyToClipboard(formattedShareText, "نص بطاقة المعلم") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ النص", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { copyToClipboard(directWhatsappLink, "رابط واتساب المباشر") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ رابط الواتساب", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Done / Edit profile button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onEditProfileRequest != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditProfileRequest()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل البيانات")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تم والانتهاء")
                    }
                }
            }
        }
    }
}
