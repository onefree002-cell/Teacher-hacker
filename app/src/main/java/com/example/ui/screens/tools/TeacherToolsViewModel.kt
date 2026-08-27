package com.example.ui.screens.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.*
import com.example.data.repository.LeaderboardItem
import com.example.data.repository.TeacherPlannerRepository
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecordManager
import com.example.util.MediaCaptureHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class TeacherToolsUiState(
    val teacher: TeacherEntity = TeacherEntity(),
    val groups: List<GroupEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val deliveries: List<MaterialDeliveryEntity> = emptyList(),
    val leaderboard: List<LeaderboardItem> = emptyList(),
    val voiceNotes: List<VoiceNoteEntity> = emptyList(),
    val homeworkSubmissions: List<HomeworkSubmissionEntity> = emptyList(),

    // 1. Lucky Student Picker
    val pickerGroupId: Long = 0L,
    val selectedLuckyStudent: StudentEntity? = null,
    val isPickingStudent: Boolean = false,

    // 2. Classroom Countdown Timer
    val timerTotalSeconds: Int = 300, // default 5 min
    val timerRemainingSeconds: Int = 300,
    val isTimerRunning: Boolean = false,
    val isTimerFinished: Boolean = false,

    // 3. Quick Grade Calculator
    val calcRawScore: String = "",
    val calcMaxScore: String = "50",
    val calcPercentage: Double? = null,
    val calcRating: String? = null,

    // 4. Material Tracker
    val materialSelectedGroupId: Long = 0L,
    val materialTitle: String = "مذكرة الفصل الأول",
    val materialPrice: String = "50",

    // 5. Voice Notes Studio
    val isRecordingAudio: Boolean = false,
    val recordingElapsedSeconds: Int = 0,
    val recordingAmplitude: Int = 0,
    val voiceNoteTitle: String = "",
    val voiceCategory: String = "شرح درس",
    val voiceGroupId: Long = 0L,
    val voiceStudentId: Long = 0L,
    val lastRecordedFilePath: String? = null,

    // Audio Playback
    val playingAudioPath: String? = null,
    val isPlayingAudio: Boolean = false,
    val playbackPositionMs: Int = 0,
    val playbackTotalDurationMs: Int = 0,

    // 6. Homework & Assignment Scanner
    val hwTitle: String = "واجب الحصة",
    val hwGroupId: Long = 0L,
    val hwStudentId: Long = 0L,
    val hwLessonDate: String = "",
    val hwPhotoPath: String? = null,
    val hwPhotoPaths: List<String> = emptyList(),
    val lastGeneratedHomeworkPdf: File? = null,
    val hwScore: String = "10",
    val hwMaxScore: String = "10",
    val hwRating: String = "ممتاز ⭐⭐⭐",
    val hwFeedbackNote: String = "",
    val hwAudioFeedbackPath: String? = null,
    val isSavingHomework: Boolean = false,

    // 7. Smart Educational Translator (المترجم التعليمي الذكي)
    val translatorInputText: String = "",
    val translatorTranslatedText: String = "",
    val translatorSourceLang: com.example.util.AppLanguage = com.example.util.AppLanguage.ARABIC,
    val translatorTargetLang: com.example.util.AppLanguage = com.example.util.AppLanguage.ENGLISH,
    val translatorSelectedSubjectFilter: String = "الكل",

    // PDF Exporting & Status
    val isExportingPdf: Boolean = false,
    val statusMessage: String? = null
)

class TeacherToolsViewModel(
    private val repository: TeacherPlannerRepository,
    private val pdfExporter: PdfReportExporter,
    private val audioRecordManager: AudioRecordManager? = null,
    private val audioPlayerManager: AudioPlayerManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherToolsUiState())
    val uiState: StateFlow<TeacherToolsUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadData()
        observeAudioManagers()
    }

    private fun observeAudioManagers() {
        audioRecordManager?.let { arm ->
            viewModelScope.launch {
                arm.isRecording.collect { rec ->
                    _uiState.update { it.copy(isRecordingAudio = rec) }
                }
            }
            viewModelScope.launch {
                arm.elapsedSeconds.collect { sec ->
                    _uiState.update { it.copy(recordingElapsedSeconds = sec) }
                }
            }
            viewModelScope.launch {
                arm.amplitude.collect { amp ->
                    _uiState.update { it.copy(recordingAmplitude = amp) }
                }
            }
        }

        audioPlayerManager?.let { apm ->
            viewModelScope.launch {
                apm.isPlaying.collect { playing ->
                    _uiState.update { it.copy(isPlayingAudio = playing) }
                }
            }
            viewModelScope.launch {
                apm.currentPath.collect { path ->
                    _uiState.update { it.copy(playingAudioPath = path) }
                }
            }
            viewModelScope.launch {
                apm.currentPositionMs.collect { pos ->
                    _uiState.update { it.copy(playbackPositionMs = pos) }
                }
            }
            viewModelScope.launch {
                apm.totalDurationMs.collect { dur ->
                    _uiState.update { it.copy(playbackTotalDurationMs = dur) }
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.teacher,
                repository.allGroups,
                repository.allStudents,
                repository.allMaterialDeliveries
            ) { teacher, groups, students, deliveries ->
                _uiState.update { current ->
                    current.copy(
                        teacher = teacher ?: TeacherEntity(name = "عبده أيمن", phone = "01206150946"),
                        groups = groups,
                        students = students,
                        deliveries = deliveries,
                        pickerGroupId = if (current.pickerGroupId == 0L && groups.isNotEmpty()) groups.first().id else current.pickerGroupId,
                        materialSelectedGroupId = if (current.materialSelectedGroupId == 0L && groups.isNotEmpty()) groups.first().id else current.materialSelectedGroupId,
                        voiceGroupId = if (current.voiceGroupId == 0L && groups.isNotEmpty()) groups.first().id else current.voiceGroupId,
                        hwGroupId = if (current.hwGroupId == 0L && groups.isNotEmpty()) groups.first().id else current.hwGroupId
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            repository.allVoiceNotes.collect { voiceNotes ->
                _uiState.update { it.copy(voiceNotes = voiceNotes) }
            }
        }

        viewModelScope.launch {
            repository.allHomework.collect { homework ->
                _uiState.update { it.copy(homeworkSubmissions = homework) }
            }
        }

        refreshLeaderboard()
    }

    private fun refreshLeaderboard() {
        viewModelScope.launch {
            try {
                val board = repository.getLeaderboard(0L)
                _uiState.update { it.copy(leaderboard = board) }
            } catch (_: Exception) {}
        }
    }

    // ==========================================
    // 1. LUCKY STUDENT PICKER
    // ==========================================
    fun setPickerGroup(groupId: Long) {
        _uiState.update { it.copy(pickerGroupId = groupId, selectedLuckyStudent = null) }
    }

    fun pickLuckyStudent() {
        val currentGroupStudents = if (_uiState.value.pickerGroupId == 0L) {
            _uiState.value.students.filter { it.status == "active" }
        } else {
            _uiState.value.students.filter { it.groupId == _uiState.value.pickerGroupId && it.status == "active" }
        }

        if (currentGroupStudents.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "لا يوجد طلاب نشطين في هذه المجموعة للقرعة") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPickingStudent = true) }
            for (i in 0 until 12) {
                val randomStudent = currentGroupStudents[Random.nextInt(currentGroupStudents.size)]
                _uiState.update { it.copy(selectedLuckyStudent = randomStudent) }
                delay(80L + (i * 20L))
            }
            val finalWinner = currentGroupStudents[Random.nextInt(currentGroupStudents.size)]
            _uiState.update { it.copy(selectedLuckyStudent = finalWinner, isPickingStudent = false) }
        }
    }

    // ==========================================
    // 2. CLASSROOM COUNTDOWN TIMER
    // ==========================================
    fun setTimerDuration(seconds: Int) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerTotalSeconds = seconds,
                timerRemainingSeconds = seconds,
                isTimerRunning = false,
                isTimerFinished = false
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.isTimerRunning) return
        if (_uiState.value.timerRemainingSeconds <= 0) {
            _uiState.update { it.copy(timerRemainingSeconds = it.timerTotalSeconds, isTimerFinished = false) }
        }

        _uiState.update { it.copy(isTimerRunning = true, isTimerFinished = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerRemainingSeconds > 0 && _uiState.value.isTimerRunning) {
                delay(1000L)
                val remaining = _uiState.value.timerRemainingSeconds - 1
                if (remaining <= 0) {
                    _uiState.update {
                        it.copy(
                            timerRemainingSeconds = 0,
                            isTimerRunning = false,
                            isTimerFinished = true,
                            statusMessage = "🔔 انتهى وقت الحصة / الاختبار المحدد!"
                        )
                    }
                    break
                } else {
                    _uiState.update { it.copy(timerRemainingSeconds = remaining) }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerRemainingSeconds = it.timerTotalSeconds,
                isTimerRunning = false,
                isTimerFinished = false
            )
        }
    }

    // ==========================================
    // 3. QUICK GRADE CALCULATOR
    // ==========================================
    fun setCalcRawScore(raw: String) {
        _uiState.update { it.copy(calcRawScore = raw) }
        recalculateGrade()
    }

    fun setCalcMaxScore(max: String) {
        _uiState.update { it.copy(calcMaxScore = max) }
        recalculateGrade()
    }

    private fun recalculateGrade() {
        val raw = _uiState.value.calcRawScore.toDoubleOrNull()
        val max = _uiState.value.calcMaxScore.toDoubleOrNull()
        if (raw != null && max != null && max > 0) {
            val percentage = (raw / max) * 100.0
            val rating = when {
                percentage >= 90.0 -> "ممتاز مع مرتبة الشرف 🌟"
                percentage >= 80.0 -> "جيد جداً مرتفع 👏"
                percentage >= 70.0 -> "جيد جداً 👍"
                percentage >= 60.0 -> "جيد ومقبول 👌"
                percentage >= 50.0 -> "على حافة النجاح ⚠️"
                else -> "يحتاج تكثيف ومتابعة ❗"
            }
            _uiState.update { it.copy(calcPercentage = percentage, calcRating = rating) }
        } else {
            _uiState.update { it.copy(calcPercentage = null, calcRating = null) }
        }
    }

    // ==========================================
    // 4. MATERIAL & BOOKLET DELIVERY TRACKER
    // ==========================================
    fun setMaterialGroup(groupId: Long) {
        _uiState.update { it.copy(materialSelectedGroupId = groupId) }
    }

    fun setMaterialTitle(title: String) {
        _uiState.update { it.copy(materialTitle = title) }
    }

    fun setMaterialPrice(price: String) {
        _uiState.update { it.copy(materialPrice = price) }
    }

    fun toggleDelivery(studentId: Long, materialName: String) {
        viewModelScope.launch {
            val existing = _uiState.value.deliveries.find { it.studentId == studentId && it.materialName == materialName }
            if (existing != null) {
                val newReceived = !existing.isDelivered
                repository.updateMaterialDelivery(existing.copy(isDelivered = newReceived))
            } else {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val newEntry = MaterialDeliveryEntity(
                    studentId = studentId,
                    materialName = materialName,
                    price = _uiState.value.materialPrice.toDoubleOrNull() ?: 0.0,
                    isDelivered = true,
                    isPaid = false,
                    deliveryDate = today
                )
                repository.insertMaterialDelivery(newEntry)
            }
        }
    }

    fun togglePayment(studentId: Long, materialName: String) {
        viewModelScope.launch {
            val existing = _uiState.value.deliveries.find { it.studentId == studentId && it.materialName == materialName }
            if (existing != null) {
                val newPaid = !existing.isPaid
                repository.updateMaterialDelivery(existing.copy(isPaid = newPaid))
            } else {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val newEntry = MaterialDeliveryEntity(
                    studentId = studentId,
                    materialName = materialName,
                    price = _uiState.value.materialPrice.toDoubleOrNull() ?: 0.0,
                    isDelivered = false,
                    isPaid = true,
                    deliveryDate = today
                )
                repository.insertMaterialDelivery(newEntry)
            }
        }
    }

    // ==========================================
    // 5. VOICE NOTES & AUDIO STUDIO
    // ==========================================
    fun setVoiceTitle(title: String) {
        _uiState.update { it.copy(voiceNoteTitle = title) }
    }

    fun setVoiceCategory(category: String) {
        _uiState.update { it.copy(voiceCategory = category) }
    }

    fun setVoiceGroup(groupId: Long) {
        _uiState.update { it.copy(voiceGroupId = groupId) }
    }

    fun setVoiceStudent(studentId: Long) {
        _uiState.update { it.copy(voiceStudentId = studentId) }
    }

    fun startAudioRecording(context: Context) {
        val title = _uiState.value.voiceNoteTitle.ifEmpty { "تسجيل_صوتي" }
        val arm = audioRecordManager ?: AudioRecordManager(context)
        val file = arm.startRecording(title)
        if (file != null) {
            _uiState.update { it.copy(lastRecordedFilePath = file.absolutePath) }
        }
    }

    fun stopAudioRecording(saveToDb: Boolean = true) {
        val file = audioRecordManager?.stopRecording(discard = !saveToDb)
        if (saveToDb && file != null && file.exists()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val title = _uiState.value.voiceNoteTitle.ifEmpty { "تسجيل صوتي - $today" }
            val note = VoiceNoteEntity(
                title = title,
                groupId = _uiState.value.voiceGroupId,
                studentId = _uiState.value.voiceStudentId,
                filePath = file.absolutePath,
                durationSeconds = _uiState.value.recordingElapsedSeconds,
                category = _uiState.value.voiceCategory,
                date = today
            )
            viewModelScope.launch {
                repository.insertVoiceNote(note)
                _uiState.update {
                    it.copy(
                        voiceNoteTitle = "",
                        lastRecordedFilePath = file.absolutePath,
                        statusMessage = "تم حفظ التسجيل الصوتي بنجاح 🎙️"
                    )
                }
            }
        }
    }

    fun playAudio(filePath: String, context: Context) {
        val apm = audioPlayerManager ?: AudioPlayerManager(context)
        apm.playOrToggle(filePath)
    }

    fun seekAudio(positionMs: Int) {
        audioPlayerManager?.seekTo(positionMs)
    }

    fun deleteVoiceNote(note: VoiceNoteEntity) {
        viewModelScope.launch {
            repository.deleteVoiceNote(note)
            try {
                val f = File(note.filePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
            _uiState.update { it.copy(statusMessage = "تم حذف التسجيل الصوتي") }
        }
    }

    fun shareVoiceNote(context: Context, note: VoiceNoteEntity, targetPhone: String? = null) {
        val teacherName = _uiState.value.teacher.name.ifEmpty { "معلم المادة" }
        val text = "🎙️ تسجيل صوتي وملاحظة هامة من أستاذ المادة: *$teacherName*\nعنوان التسجيل: *${note.title}*\nالتصنيف: ${note.category}"
        MediaCaptureHelper.shareMediaWithWhatsApp(context, targetPhone, text, note.filePath)
    }

    // ==========================================
    // 6. HOMEWORK & ASSIGNMENT SCANNER
    // ==========================================
    fun setHwTitle(title: String) {
        _uiState.update { it.copy(hwTitle = title) }
    }

    fun setHwGroup(groupId: Long) {
        _uiState.update { it.copy(hwGroupId = groupId) }
    }

    fun setHwStudent(studentId: Long) {
        _uiState.update { it.copy(hwStudentId = studentId) }
    }

    fun setHwLessonDate(date: String) {
        _uiState.update { it.copy(hwLessonDate = date) }
    }

    fun setHwPhotoPath(path: String?) {
        _uiState.update { current ->
            val list = if (path != null) {
                if (current.hwPhotoPaths.contains(path)) current.hwPhotoPaths else current.hwPhotoPaths + path
            } else {
                emptyList()
            }
            current.copy(hwPhotoPath = path, hwPhotoPaths = list)
        }
    }

    fun addHwPhotoPath(path: String) {
        _uiState.update { current ->
            val updatedList = current.hwPhotoPaths + path
            current.copy(hwPhotoPaths = updatedList, hwPhotoPath = updatedList.firstOrNull())
        }
    }

    fun removeHwPhotoPath(index: Int) {
        _uiState.update { current ->
            val updatedList = current.hwPhotoPaths.toMutableList().apply {
                if (index in indices) removeAt(index)
            }
            current.copy(hwPhotoPaths = updatedList, hwPhotoPath = updatedList.firstOrNull())
        }
    }

    fun clearHwPhotos() {
        _uiState.update { it.copy(hwPhotoPaths = emptyList(), hwPhotoPath = null) }
    }

    fun setHwScore(score: String) {
        _uiState.update { it.copy(hwScore = score) }
    }

    fun setHwMaxScore(maxScore: String) {
        _uiState.update { it.copy(hwMaxScore = maxScore) }
    }

    fun setHwRating(rating: String) {
        _uiState.update { it.copy(hwRating = rating) }
    }

    fun setHwFeedbackNote(note: String) {
        _uiState.update { it.copy(hwFeedbackNote = note) }
    }

    fun saveAndGenerateHomeworkPdf(
        context: Context,
        onComplete: ((File) -> Unit)? = null
    ) {
        val studentId = _uiState.value.hwStudentId
        val student = _uiState.value.students.find { it.id == studentId }

        if (student == null) {
            _uiState.update { it.copy(statusMessage = "يرجى اختيار اسم الطالب أولاً") }
            return
        }

        val allPhotos = if (_uiState.value.hwPhotoPaths.isNotEmpty()) {
            _uiState.value.hwPhotoPaths
        } else if (!_uiState.value.hwPhotoPath.isNullOrEmpty()) {
            listOf(_uiState.value.hwPhotoPath!!)
        } else {
            emptyList()
        }

        if (allPhotos.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "يرجى التقاط صورة واحدة على الأقل لواجب الطالب") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingHomework = true) }
            try {
                val group = _uiState.value.groups.find { it.id == _uiState.value.hwGroupId || it.id == student.groupId }
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lessonDate = _uiState.value.hwLessonDate.ifBlank { today }
                val scoreVal = _uiState.value.hwScore.toDoubleOrNull() ?: 10.0
                val maxScoreVal = _uiState.value.hwMaxScore.toDoubleOrNull() ?: 10.0
                val title = _uiState.value.hwTitle.ifBlank { "واجب الحصة - $lessonDate" }

                // 1. Generate the PDF with the exact requested naming convention: "واجب - اسم الطالب - تاريخ الحصة.pdf"
                val pdfFile = pdfExporter.generateStudentHomeworkPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    student = student,
                    group = group,
                    lessonDate = lessonDate,
                    homeworkTitle = title,
                    imagePaths = allPhotos,
                    score = scoreVal,
                    maxScore = maxScoreVal,
                    rating = _uiState.value.hwRating,
                    teacherFeedback = _uiState.value.hwFeedbackNote
                )

                // 2. Save submission entity to DB
                val submission = HomeworkSubmissionEntity(
                    studentId = student.id,
                    groupId = group?.id ?: 0L,
                    title = title,
                    assignedDate = lessonDate,
                    photoUri = pdfFile.absolutePath, // Store PDF file path
                    audioFeedbackUri = _uiState.value.lastRecordedFilePath ?: "",
                    score = scoreVal,
                    maxScore = maxScoreVal,
                    rating = _uiState.value.hwRating,
                    feedbackNote = _uiState.value.hwFeedbackNote,
                    status = "corrected"
                )
                repository.insertHomework(submission)

                // 3. Register as StudyFile so it is also indexed in study files / notes
                try {
                    val studyFile = StudyFileEntity(
                        title = "واجب - ${student.name} - $lessonDate",
                        grade = student.grade.ifBlank { "جميع الصفوف" },
                        category = "واجبات مصححة",
                        subject = _uiState.value.teacher.subject.ifBlank { "المادة" },
                        localFilePath = pdfFile.absolutePath,
                        originalFileName = pdfFile.name,
                        fileExtension = "pdf",
                        fileSizeBytes = pdfFile.length(),
                        notes = "تم تصحيح الواجب بنجاح • الدرجة: ${scoreVal.toInt()}/${maxScoreVal.toInt()}",
                        dateAdded = lessonDate
                    )
                    repository.insertStudyFile(studyFile)
                } catch (_: Exception) {}

                _uiState.update {
                    it.copy(
                        isSavingHomework = false,
                        lastGeneratedHomeworkPdf = pdfFile,
                        hwPhotoPaths = emptyList(),
                        hwPhotoPath = null,
                        hwFeedbackNote = "",
                        statusMessage = "تم حفظ وتوليد ملف PDF باسم '${pdfFile.name}' بنجاح 🎉"
                    )
                }

                onComplete?.invoke(pdfFile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingHomework = false,
                        statusMessage = "حدث خطأ أثناء حفظ الواجب: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deleteHomework(hw: HomeworkSubmissionEntity) {
        viewModelScope.launch {
            repository.deleteHomework(hw)
            if (hw.photoUri.isNotEmpty()) {
                try {
                    val f = File(hw.photoUri)
                    if (f.exists()) f.delete()
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(statusMessage = "تم حذف سجل الواجب") }
        }
    }

    fun shareHomeworkFeedback(context: Context, hw: HomeworkSubmissionEntity) {
        val student = _uiState.value.students.find { it.id == hw.studentId }
        val teacherName = _uiState.value.teacher.name.ifEmpty { "معلم المادة" }
        val phone = student?.parentPhone?.ifEmpty { student.phone }

        val msg = """
📝 *تقرير تقييم وتصحيح الواجب الدراسي*
الطالب: *${student?.name ?: "البطل"}*
عنوان الواجب: *${hw.title}*
حالة الواجب والتقييم: *${hw.rating}*
ملاحظات المعلم: ${hw.feedbackNote.ifEmpty { "مجهود رائع وبارك الله فيك!" }}

معلم المادة: *$teacherName*
        """.trimIndent()

        MediaCaptureHelper.shareMediaWithWhatsApp(context, phone, msg, hw.photoUri.ifEmpty { null })
    }

    // ==========================================
    // 7. PDF EXPORT ACTIONS
    // ==========================================
    fun printStudentIdCards(context: Context, groupId: Long = 0L) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExportingPdf = true) }
                val group = if (groupId > 0L) _uiState.value.groups.find { it.id == groupId } else null
                val targetStudents = _uiState.value.students.filter { it.status == "active" && (groupId == 0L || it.groupId == groupId) }
                if (targetStudents.isEmpty()) {
                    _uiState.update { it.copy(isExportingPdf = false, statusMessage = "لا يوجد طلاب لطباعة الكارنيهات") }
                    return@launch
                }
                val file = pdfExporter.generateStudentIdCardsPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    group = group,
                    students = targetStudents
                )
                _uiState.update { it.copy(isExportingPdf = false, statusMessage = "تم تجهيز كارنيهات الطلاب بنجاح") }
                pdfExporter.sharePdf(context, file, "كارنيهات الطلاب - ${group?.name ?: "الجميع"}")
            } catch (e: Exception) {
                _uiState.update { it.copy(isExportingPdf = false, statusMessage = "فشل تصدير الكارنيهات: ${e.localizedMessage}") }
            }
        }
    }

    fun printHonorRoll(context: Context, groupId: Long = 0L) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExportingPdf = true) }
                val group = if (groupId > 0L) _uiState.value.groups.find { it.id == groupId } else null
                val board = repository.getLeaderboard(groupId)
                if (board.isEmpty()) {
                    _uiState.update { it.copy(isExportingPdf = false, statusMessage = "لا توجد بيانات أو درجات كافية للوحة الشرف") }
                    return@launch
                }
                val file = pdfExporter.generateHonorRollPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    groupName = group?.name ?: "كافة المجموعات والصفوف",
                    leaderboardItems = board
                )
                _uiState.update { it.copy(isExportingPdf = false, statusMessage = "تم توليد بوستر لوحة الشرف بنجاح 👑") }
                pdfExporter.sharePdf(context, file, "لوحة شرف الأوائل - ${group?.name ?: "الجميع"}")
            } catch (e: Exception) {
                _uiState.update { it.copy(isExportingPdf = false, statusMessage = "فشل تصدير لوحة الشرف: ${e.localizedMessage}") }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Smart Educational Translator (المترجم التعليمي واللغوي الذكي)
    // ------------------------------------------------------------------------

    fun setTranslatorInput(text: String) {
        _uiState.update { it.copy(translatorInputText = text) }
        performTranslation(text, _uiState.value.translatorSourceLang, _uiState.value.translatorTargetLang)
    }

    fun setTranslatorLanguages(source: com.example.util.AppLanguage, target: com.example.util.AppLanguage) {
        _uiState.update { it.copy(translatorSourceLang = source, translatorTargetLang = target) }
        performTranslation(_uiState.value.translatorInputText, source, target)
    }

    fun swapTranslatorLanguages() {
        val currentSource = _uiState.value.translatorSourceLang
        val currentTarget = _uiState.value.translatorTargetLang
        val currentResult = _uiState.value.translatorTranslatedText
        _uiState.update {
            it.copy(
                translatorSourceLang = currentTarget,
                translatorTargetLang = currentSource,
                translatorInputText = currentResult
            )
        }
        performTranslation(currentResult, currentTarget, currentSource)
    }

    fun setTranslatorSubjectFilter(subject: String) {
        _uiState.update { it.copy(translatorSelectedSubjectFilter = subject) }
    }

    private fun performTranslation(text: String, source: com.example.util.AppLanguage, target: com.example.util.AppLanguage) {
        if (text.isBlank()) {
            _uiState.update { it.copy(translatorTranslatedText = "") }
            return
        }
        val result = com.example.util.SmartTranslatorHelper.translate(text, source, target)
        _uiState.update { it.copy(translatorTranslatedText = result) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager?.release()
        audioPlayerManager?.release()
    }
}
