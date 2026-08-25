package com.example.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentWithInfo(
    val payment: PaymentEntity,
    val studentName: String,
    val groupName: String
)

data class StudentBalanceInfo(
    val student: StudentEntity,
    val groupName: String,
    val totalPaid: Double,
    val totalRequired: Double,
    val remainingBalance: Double
)

data class FinanceUiState(
    val teacher: TeacherEntity? = null,
    val payments: List<PaymentWithInfo> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val studentBalances: List<StudentBalanceInfo> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalPendingBalance: Double = 0.0,
    val isLoading: Boolean = false
)

class FinanceViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        loadFinanceData()
    }

    private fun loadFinanceData() {
        viewModelScope.launch {
            combine(
                repository.teacher,
                repository.allPayments,
                repository.allExpenses,
                repository.allStudents,
                repository.allGroups
            ) { teacher, payments, expenses, students, groups ->
                val studentMap = students.associateBy { it.id }
                val groupMap = groups.associateBy { it.id }

                val paymentList = payments.map { p ->
                    PaymentWithInfo(
                        payment = p,
                        studentName = studentMap[p.studentId]?.name ?: "طالب #${p.studentId}",
                        groupName = groupMap[p.groupId]?.name ?: "غير محدد"
                    )
                }

                val totalRev = payments.sumOf { it.amount }
                val totalExp = expenses.sumOf { it.amount }
                val net = totalRev - totalExp

                val balances = students.map { s ->
                    val sGroup = groupMap[s.groupId]
                    val sPayments = payments.filter { it.studentId == s.id }
                    val sPaid = sPayments.sumOf { it.amount }
                    val monthlyPrice = sGroup?.monthlyPrice ?: 0.0
                    val sReq = if (s.isExempt) 0.0 else monthlyPrice * (1.0 - (s.discountPercent / 100.0))
                    val sRem = maxOf(0.0, sReq - sPaid)

                    StudentBalanceInfo(
                        student = s,
                        groupName = sGroup?.name ?: "غير محدد",
                        totalPaid = sPaid,
                        totalRequired = sReq,
                        remainingBalance = sRem
                    )
                }

                val totalPending = balances.sumOf { it.remainingBalance }

                _uiState.value = _uiState.value.copy(
                    teacher = teacher,
                    payments = paymentList,
                    expenses = expenses,
                    students = students,
                    groups = groups,
                    studentBalances = balances,
                    totalRevenue = totalRev,
                    totalExpenses = totalExp,
                    netProfit = net,
                    totalPendingBalance = totalPending,
                    isLoading = false
                )
            }.collect {
                // state updated
            }
        }
    }

    fun addPayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.insertPayment(payment)
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
