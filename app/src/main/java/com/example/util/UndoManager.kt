package com.example.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UndoableAction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val undoBlock: suspend () -> Unit
)

object UndoManager {
    private val _history = MutableStateFlow<List<UndoableAction>>(emptyList())
    val history: StateFlow<List<UndoableAction>> = _history.asStateFlow()

    private val _lastAction = MutableStateFlow<UndoableAction?>(null)
    val lastAction: StateFlow<UndoableAction?> = _lastAction.asStateFlow()

    private val _undoNotification = MutableStateFlow<String?>(null)
    val undoNotification: StateFlow<String?> = _undoNotification.asStateFlow()

    fun recordAction(title: String, description: String = "", undoBlock: suspend () -> Unit) {
        val action = UndoableAction(
            title = title,
            description = description,
            undoBlock = undoBlock
        )
        val currentList = _history.value.toMutableList()
        currentList.add(0, action)
        if (currentList.size > 25) {
            currentList.removeAt(currentList.lastIndex)
        }
        _history.value = currentList
        _lastAction.value = action
    }

    fun undoLast(scope: CoroutineScope, onCompleted: (String) -> Unit = {}) {
        val list = _history.value
        if (list.isEmpty()) return
        val actionToUndo = list.first()
        val remaining = list.drop(1)
        _history.value = remaining
        _lastAction.value = remaining.firstOrNull()

        scope.launch(Dispatchers.IO) {
            try {
                actionToUndo.undoBlock()
                _undoNotification.value = "تم التراجع عن: ${actionToUndo.title}"
                onCompleted(actionToUndo.title)
            } catch (e: Exception) {
                _undoNotification.value = "تعذر التراجع: ${e.localizedMessage}"
            }
        }
    }

    fun undoSpecific(actionId: String, scope: CoroutineScope, onCompleted: (String) -> Unit = {}) {
        val list = _history.value
        val action = list.find { it.id == actionId } ?: return
        val remaining = list.filter { it.id != actionId }
        _history.value = remaining
        _lastAction.value = remaining.firstOrNull()

        scope.launch(Dispatchers.IO) {
            try {
                action.undoBlock()
                _undoNotification.value = "تم التراجع عن: ${action.title}"
                onCompleted(action.title)
            } catch (e: Exception) {
                _undoNotification.value = "تعذر التراجع: ${e.localizedMessage}"
            }
        }
    }

    fun clearNotification() {
        _undoNotification.value = null
    }

    fun clearHistory() {
        _history.value = emptyList()
        _lastAction.value = null
    }
}
