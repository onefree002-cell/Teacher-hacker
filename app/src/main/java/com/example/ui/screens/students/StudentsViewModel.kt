package com.example.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentsUiState(
    val students: List<StudentEntity> = emptyList(),
    val filteredStudents: List<StudentEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedGroupId: Long = 0, // 0 = all
    val selectedGrade: String = "all", // "all" or specific grade
    val selectedStatus: String = "all", // "all", "active", "inactive"
    val selectedTag: String = "all", // "all" or specific tag like "متميز", "يحتاج متابعة", "متأخر بالمصروفات", "منحة"
    val isLoading: Boolean = false,
    val selectedStudentDetails: StudentWithDetails? = null,
    val studentGrades: List<StudentGradeItem> = emptyList(),
    val studentAttendance: List<AttendanceEntity> = emptyList(),
    val studentPayments: List<PaymentEntity> = emptyList(),
    val studentDeliveries: List<MaterialDeliveryEntity> = emptyList(),
    val studentHomeworks: List<HomeworkSubmissionEntity> = emptyList(),
    val teacher: TeacherEntity? = null
)

class StudentsViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentsUiState())
    val uiState: StateFlow<StudentsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.allStudents,
                repository.allGroups,
                repository.teacher
            ) { students, groups, teacher ->
                val current = _uiState.value
                val filtered = filterStudentsList(
                    students = students,
                    query = current.searchQuery,
                    groupId = current.selectedGroupId,
                    grade = current.selectedGrade,
                    status = current.selectedStatus,
                    tag = current.selectedTag
                )
                current.copy(
                    students = students,
                    filteredStudents = filtered,
                    groups = groups,
                    teacher = teacher,
                    isLoading = false
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        val filtered = filterStudentsList(
            students = current.students,
            query = query,
            groupId = current.selectedGroupId,
            grade = current.selectedGrade,
            status = current.selectedStatus,
            tag = current.selectedTag
        )
        _uiState.value = current.copy(searchQuery = query, filteredStudents = filtered)
    }

    fun onGroupFilterSelected(groupId: Long) {
        val current = _uiState.value
        val filtered = filterStudentsList(
            students = current.students,
            query = current.searchQuery,
            groupId = groupId,
            grade = current.selectedGrade,
            status = current.selectedStatus,
            tag = current.selectedTag
        )
        _uiState.value = current.copy(selectedGroupId = groupId, filteredStudents = filtered)
    }

    fun onGradeFilterSelected(grade: String) {
        val current = _uiState.value
        val filtered = filterStudentsList(
            students = current.students,
            query = current.searchQuery,
            groupId = current.selectedGroupId,
            grade = grade,
            status = current.selectedStatus,
            tag = current.selectedTag
        )
        _uiState.value = current.copy(selectedGrade = grade, filteredStudents = filtered)
    }

    fun onStatusFilterSelected(status: String) {
        val current = _uiState.value
        val filtered = filterStudentsList(
            students = current.students,
            query = current.searchQuery,
            groupId = current.selectedGroupId,
            grade = current.selectedGrade,
            status = status,
            tag = current.selectedTag
        )
        _uiState.value = current.copy(selectedStatus = status, filteredStudents = filtered)
    }

    fun onTagFilterSelected(tag: String) {
        val current = _uiState.value
        val filtered = filterStudentsList(
            students = current.students,
            query = current.searchQuery,
            groupId = current.selectedGroupId,
            grade = current.selectedGrade,
            status = current.selectedStatus,
            tag = tag
        )
        _uiState.value = current.copy(selectedTag = tag, filteredStudents = filtered)
    }

    private fun filterStudentsList(
        students: List<StudentEntity>,
        query: String,
        groupId: Long,
        grade: String,
        status: String,
        tag: String
    ): List<StudentEntity> {
        return students.filter { s ->
            val matchQuery = query.isEmpty() ||
                    s.name.contains(query, ignoreCase = true) ||
                    s.phone.contains(query) ||
                    s.parentPhone.contains(query) ||
                    s.barcodeCode.contains(query)
            val matchGroup = groupId == 0L || s.groupId == groupId
            val matchGrade = grade == "all" || s.grade == grade
            val matchStatus = status == "all" || s.status == status
            val matchTag = tag == "all" || s.notes.contains(tag) || (tag == "منحة" && s.isExempt)
            matchQuery && matchGroup && matchGrade && matchStatus && matchTag
        }
    }

    fun transferStudent(studentId: Long, newGroupId: Long) {
        viewModelScope.launch {
            repository.transferStudent(studentId, newGroupId)
            if (_uiState.value.selectedStudentDetails?.student?.id == studentId) {
                loadStudentDetails(studentId)
            }
        }
    }

    fun addOrUpdateStudent(student: StudentEntity) {
        viewModelScope.launch {
            if (student.id == 0L) {
                repository.insertStudent(student)
            } else {
                repository.updateStudent(student)
                if (_uiState.value.selectedStudentDetails?.student?.id == student.id) {
                    loadStudentDetails(student.id)
                }
            }
        }
    }

    fun importStudentsBatch(students: List<StudentEntity>) {
        viewModelScope.launch {
            repository.insertAllStudents(students)
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun recordMaterialDelivery(delivery: MaterialDeliveryEntity) {
        viewModelScope.launch {
            if (delivery.id == 0L) {
                repository.insertMaterialDelivery(delivery)
            } else {
                repository.updateMaterialDelivery(delivery)
            }
            _uiState.value.selectedStudentDetails?.student?.id?.let {
                loadStudentDetails(it)
            }
        }
    }

    fun deleteMaterialDelivery(delivery: MaterialDeliveryEntity) {
        viewModelScope.launch {
            repository.deleteMaterialDelivery(delivery)
            _uiState.value.selectedStudentDetails?.student?.id?.let {
                loadStudentDetails(it)
            }
        }
    }

    fun deleteHomework(homework: HomeworkSubmissionEntity) {
        viewModelScope.launch {
            repository.deleteHomework(homework)
            _uiState.value.selectedStudentDetails?.student?.id?.let {
                loadStudentDetails(it)
            }
        }
    }

    fun updateHomework(homework: HomeworkSubmissionEntity) {
        viewModelScope.launch {
            repository.updateHomework(homework)
            _uiState.value.selectedStudentDetails?.student?.id?.let {
                loadStudentDetails(it)
            }
        }
    }

    fun saveHomework(homework: HomeworkSubmissionEntity) {
        viewModelScope.launch {
            if (homework.id == 0L) {
                repository.insertHomework(homework)
            } else {
                repository.updateHomework(homework)
            }
            _uiState.value.selectedStudentDetails?.student?.id?.let {
                loadStudentDetails(it)
            }
        }
    }

    fun loadStudentDetails(studentId: Long) {
        viewModelScope.launch {
            val details = repository.getStudentDetails(studentId)
            val attList = repository.getAttendanceByStudent(studentId).first()
            val payList = repository.getPaymentsByStudent(studentId).first()
            val delList = repository.getDeliveriesByStudent(studentId).first()
            val hwList = repository.getHomeworkByStudent(studentId).first()

            _uiState.value = _uiState.value.copy(
                selectedStudentDetails = details,
                studentAttendance = attList,
                studentPayments = payList,
                studentDeliveries = delList,
                studentHomeworks = hwList
            )
        }
    }
}
