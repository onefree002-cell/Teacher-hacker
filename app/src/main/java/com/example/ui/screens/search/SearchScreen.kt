package com.example.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToStudent: (Long) -> Unit,
    onNavigateToGroup: (Long) -> Unit,
    onNavigateToExam: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "البحث الشامل في التطبيق",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("ابحث عن طالب، مجموعة، حصة، امتحان، دفعة...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NavyPrimary) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("universal_search_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (state.query.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.Search,
                    title = "ابدأ البحث السريع",
                    subtitle = "اكتب اسم طالب أو رقم هاتف أو اسم مجموعة أو تاريخ للوصول الفوري",
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else if (state.totalResultsCount == 0) {
                EmptyStateCard(
                    icon = Icons.Filled.SearchOff,
                    title = "لا توجد نتائج مطابقة",
                    subtitle = "لم نتمكن من العثور على أي نتائج لكلمة البحث: \"${state.query}\"",
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Students
                    if (state.filteredStudents.isNotEmpty()) {
                        item {
                            SectionHeader(title = "الطلاب (${state.filteredStudents.size})")
                        }
                        items(state.filteredStudents, key = { "student_${it.id}" }) { s ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToStudent(s.id) }
                                    .testTag("search_student_${s.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = NavyPrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${s.grade} | هاتف: ${s.phone.ifEmpty { "غير متوفر" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Groups
                    if (state.filteredGroups.isNotEmpty()) {
                        item {
                            SectionHeader(title = "المجموعات (${state.filteredGroups.size})")
                        }
                        items(state.filteredGroups, key = { "group_${it.id}" }) { g ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToGroup(g.id) }
                                    .testTag("search_group_${g.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Class, contentDescription = null, tint = EmeraldGreen)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(g.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${g.grade} | المواعيد: ${g.sessionDays} ${g.sessionTime}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Exams
                    if (state.filteredExams.isNotEmpty()) {
                        item {
                            SectionHeader(title = "الامتحانات (${state.filteredExams.size})")
                        }
                        items(state.filteredExams, key = { "exam_${it.id}" }) { ex ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToExam(ex.id) }
                                    .testTag("search_exam_${ex.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = AmberGoldDark)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("الدرجة العظمى: ${ex.maxScore} | التاريخ: ${ex.date}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Payments
                    if (state.filteredPayments.isNotEmpty()) {
                        item {
                            SectionHeader(title = "المدفوعات المالية (${state.filteredPayments.size})")
                        }
                        items(state.filteredPayments, key = { "pay_${it.id}" }) { p ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = EmeraldGreen)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${p.amount} ج.م - ${p.monthName.ifEmpty { p.type }}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("التاريخ: ${p.date} | ${p.note}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
