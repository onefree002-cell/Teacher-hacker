package com.example.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.local.entity.ReportSettingEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    initialStudentId: Long? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialStudentId) {
        if (initialStudentId != null && initialStudentId != 0L) {
            viewModel.selectStudent(initialStudentId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مركز طباعة وتصدير التقارير",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // MULTI-REPORT PRINTING / BATCH HUB
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Print, contentDescription = null, tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "طباعة أكثر من تقرير دفعة واحدة (Batch Printing)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary
                            )
                        }

                        Text(
                            "يمكنك طباعة ملف تقارير شامل يحتوي على تقرير لكل طالب في المجموعة أو كشف الدرجات المصفوفي دفعة واحدة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )

                        if (state.groups.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = groupDropdownExpanded,
                                onExpandedChange = { groupDropdownExpanded = it }
                            ) {
                                val gName = state.groups.find { it.id == state.selectedGroupId }?.name ?: "كل المجموعات"
                                OutlinedTextField(
                                    value = gName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("اختر المجموعة لتصدير تقاريرها") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = groupDropdownExpanded,
                                    onDismissRequest = { groupDropdownExpanded = false }
                                ) {
                                    state.groups.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text(g.name) },
                                            onClick = {
                                                viewModel.selectGroup(g.id)
                                                groupDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button 1: Group Dossier
                            Button(
                                onClick = {
                                    viewModel.generateGroupDossierBatch(context, state.selectedGroupId) { file ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("تم إنشاء ملف التقارير المجمع لجميع الطلاب بنجاح")
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف التجميعي"))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier.weight(1f).testTag("batch_dossier_btn")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ملف كل الطلاب (PDF)", style = MaterialTheme.typography.labelSmall)
                            }

                            // Button 2: Grade Matrix Sheet
                            FilledTonalButton(
                                onClick = {
                                    viewModel.generateGradeMatrixSheet(context, state.selectedGroupId) { file ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("تم إنشاء كشف الدرجات المجمع بنجاح")
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الدرجات"))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("grade_matrix_btn")
                            ) {
                                Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("كشف الدرجات (PDF)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Excel Export All Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccessContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تصدير إكسيل شامل لكل البيانات", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                            Text("الطلاب، المجموعات، الحصص، الحضور، الامتحانات، والمالية", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                viewModel.generateExcelExport(context) { file ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم تصدير ملف الإكسيل بنجاح")
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الإكسيل"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_excel_btn")
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel")
                        }
                    }
                }
            }

            // Student Selection for Individual Report
            item {
                SectionHeader(title = "تقرير متابعة تفصيلي لطالب محدد")
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = it }
                ) {
                    val sName = state.students.firstOrNull { it.id == state.selectedStudentId }?.name ?: "اختر الطالب"
                    OutlinedTextField(
                        value = sName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الطالب") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("report_student_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        state.students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    viewModel.selectStudent(s.id)
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Report Header Customizer
            item {
                SectionHeader(title = "تخصيص عنوان وعناصر التقرير")
                OutlinedTextField(
                    value = state.reportSetting.headerTitle,
                    onValueChange = { viewModel.updateReportSetting(state.reportSetting.copy(headerTitle = it)) },
                    label = { Text("عنوان التقرير في الأعلى") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("report_title_input")
                )
            }

            // Toggle Switches for Customizing Report Elements
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("عناصر التقرير المضمنة:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        HorizontalDivider()

                        ReportToggleRow("اسم الطالب وبياناته", state.reportSetting.showStudentName) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showStudentName = it))
                        }
                        ReportToggleRow("المجموعة الدراسية", state.reportSetting.showGroup) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showGroup = it))
                        }
                        ReportToggleRow("الصف الدراسي", state.reportSetting.showGrade) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showGrade = it))
                        }
                        ReportToggleRow("هاتف الطالب", state.reportSetting.showPhone) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showPhone = it))
                        }
                        ReportToggleRow("هاتف ولي الأمر", state.reportSetting.showParentPhone) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showParentPhone = it))
                        }
                        ReportToggleRow("سجل الحضور والغياب", state.reportSetting.showAttendance) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showAttendance = it))
                        }
                        ReportToggleRow("نتائج الامتحانات والاختبارات", state.reportSetting.showExams) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showExams = it))
                        }
                        ReportToggleRow("الملخص المالي والمدفوعات", state.reportSetting.showPayments) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showPayments = it))
                        }
                        ReportToggleRow("ملاحظات وتوجيهات المدرس", state.reportSetting.showNotes) {
                            viewModel.updateReportSetting(state.reportSetting.copy(showNotes = it))
                        }
                    }
                }
            }

            // PDF Action Button
            item {
                Button(
                    onClick = {
                        viewModel.generatePdfReport(context) { file ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم إنشاء تقرير الطالب بنجاح")
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير الطالب"))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generate_pdf_report_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إنشاء ومشاركة تقرير الطالب الفردي PDF")
                }
            }
        }
    }
}

@Composable
fun ReportToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
