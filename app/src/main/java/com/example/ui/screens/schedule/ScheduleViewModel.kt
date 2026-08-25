package com.example.ui.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.*
import com.example.data.repository.*
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class ScheduleViewMode {
    CARDS,
    WEEKLY_TIMETABLE,
    MONTHLY_CALENDAR
}

data class ScheduleUiState(
    val sessionsWithGroups: List<SessionWithGroup> = emptyList(),
    val filteredSessions: List<SessionWithGroup> = emptyList(),
    val exams: List<ExamWithGroup> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val venues: List<VenueEntity> = emptyList(),
    val teacher: TeacherEntity = TeacherEntity(),
    val selectedDay: String = "all", // "all" or "السبت", "الأحد", etc.
    val selectedCalendarDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val viewMode: ScheduleViewMode = ScheduleViewMode.WEEKLY_TIMETABLE,
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportedPdfFile: File? = null,
    val statusMessage: String? = null,
    val conflictWarning: String? = null
)

class ScheduleViewModel(
    private val repository: TeacherPlannerRepository,
    private val pdfExporter: PdfReportExporter = PdfReportExporter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            combine(
                repository.allSessions,
                repository.allGroups,
                repository.allVenues,
                repository.teacher,
                repository.allExams
            ) { sessions, groups, venues, teacher, exams ->
                val groupMap = groups.associateBy { it.id }
                val fullList = sessions.map { sess ->
                    SessionWithGroup(
                        session = sess,
                        groupName = groupMap[sess.groupId]?.name ?: "غير محدد",
                        location = groupMap[sess.groupId]?.location ?: sess.location
                    )
                }.sortedWith(
                    compareBy<SessionWithGroup> { TimeUtils.dayOrderIndex(it.session.day) }
                        .thenBy { TimeUtils.timeToMinutes(it.session.time) }
                )
                val currentDay = _uiState.value.selectedDay
                val filtered = (if (currentDay == "all") fullList else fullList.filter { it.session.day.contains(currentDay) })
                    .sortedBy { TimeUtils.timeToMinutes(it.session.time) }

                val examsWithGroups = exams.map { ex ->
                    ExamWithGroup(
                        exam = ex,
                        groupName = groupMap[ex.groupId]?.name ?: "غير محدد"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    sessionsWithGroups = fullList,
                    filteredSessions = filtered,
                    exams = examsWithGroups,
                    groups = groups,
                    venues = venues,
                    teacher = teacher ?: TeacherEntity(),
                    isLoading = false
                )
            }.collect {
                // state updated in combine
            }
        }
    }

    fun setSelectedCalendarDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedCalendarDate = date)
    }

    fun addNewVenue(venue: VenueEntity, onSaved: (VenueEntity) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertVenue(venue)
            onSaved(venue.copy(id = id))
        }
    }

    fun setViewMode(mode: ScheduleViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun onDaySelected(day: String) {
        val fullList = _uiState.value.sessionsWithGroups
        val filtered = (if (day == "all") fullList else fullList.filter { it.session.day.contains(day) })
            .sortedBy { TimeUtils.timeToMinutes(it.session.time) }
        _uiState.value = _uiState.value.copy(selectedDay = day, filteredSessions = filtered)
    }

    fun printSchedulePdf(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingPdf = true)
            try {
                val sessionsToPrint = if (_uiState.value.selectedDay == "all") {
                    _uiState.value.sessionsWithGroups
                } else {
                    _uiState.value.filteredSessions
                }
                val file = pdfExporter.exportSchedulePdf(
                    context = context,
                    sessionsWithGroups = sessionsToPrint,
                    teacher = _uiState.value.teacher,
                    selectedDay = _uiState.value.selectedDay
                )
                _uiState.value = _uiState.value.copy(
                    isExportingPdf = false,
                    exportedPdfFile = file,
                    statusMessage = "تم إنشاء ملف جدول الحصص PDF بنجاح"
                )
                pdfExporter.sharePdf(context, file, "جدول الحصص والمواعيد - ${_uiState.value.teacher.name}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExportingPdf = false,
                    statusMessage = "حدث خطأ أثناء إنشاء PDF: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun addOrUpdateSession(session: SessionEntity, onConflict: (String) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val conflict = repository.checkSessionConflict(session.day, session.time, session.durationMinutes, session.id)
            if (conflict != null) {
                onConflict(conflict)
            } else {
                if (session.id == 0L) {
                    repository.insertSession(session)
                } else {
                    repository.updateSession(session)
                }
                onSuccess()
            }
        }
    }

    fun toggleSessionCompletion(session: SessionEntity) {
        viewModelScope.launch {
            repository.updateSession(session.copy(completed = !session.completed))
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }
}
