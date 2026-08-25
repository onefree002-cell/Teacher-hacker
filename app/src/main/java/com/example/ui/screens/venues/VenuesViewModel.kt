package com.example.ui.screens.venues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.VenueEntity
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VenueWithStats(
    val venue: VenueEntity,
    val groupCount: Int = 0,
    val weeklySessionCount: Int = 0
)

data class VenuesUiState(
    val venues: List<VenueWithStats> = emptyList(),
    val allGroups: List<GroupEntity> = emptyList(),
    val allSessions: List<SessionEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class VenuesViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(VenuesUiState())
    val uiState: StateFlow<VenuesUiState> = _uiState.asStateFlow()

    init {
        loadVenues()
    }

    private fun loadVenues() {
        viewModelScope.launch {
            combine(
                repository.allVenues,
                repository.allGroups,
                repository.allSessions
            ) { venues, groups, sessions ->
                val list = venues.map { venue ->
                    val linkedGroups = groups.filter { it.location.equals(venue.name, ignoreCase = true) || it.location.contains(venue.name, ignoreCase = true) }
                    val groupIds = linkedGroups.map { it.id }.toSet()
                    val linkedSessions = sessions.filter { it.groupId in groupIds || it.location.contains(venue.name, ignoreCase = true) }
                    VenueWithStats(
                        venue = venue,
                        groupCount = linkedGroups.size,
                        weeklySessionCount = linkedSessions.size
                    )
                }
                _uiState.value = _uiState.value.copy(
                    venues = list,
                    allGroups = groups,
                    allSessions = sessions,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addOrUpdateVenue(venue: VenueEntity) {
        viewModelScope.launch {
            if (venue.id == 0L) {
                repository.insertVenue(venue)
            } else {
                repository.updateVenue(venue)
            }
        }
    }

    fun deleteVenue(venue: VenueEntity) {
        viewModelScope.launch {
            repository.deleteVenue(venue)
        }
    }
}
