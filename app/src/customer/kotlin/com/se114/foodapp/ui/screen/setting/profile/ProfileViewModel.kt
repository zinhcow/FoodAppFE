package com.se114.foodapp.ui.screen.setting.profile


import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute

import com.example.foodapp.data.model.Account

import com.example.foodapp.domain.use_case.auth.FirebaseResult
import com.example.foodapp.domain.use_case.auth.LogOutUseCase
import com.example.foodapp.ui.screen.components.validateField
import com.se114.foodapp.domain.use_case.user.LoadProfileUseCase
import com.se114.foodapp.domain.use_case.user.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val loadProfileUseCase: LoadProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logOutUseCase: LogOutUseCase,
    val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
    private val isUpdatingArgument=  savedStateHandle.toRoute<com.example.foodapp.navigation.Profile>().isUpdating
    private val _uiState =
        MutableStateFlow(Profile.UiState(isUpdating = isUpdatingArgument))
    val uiState: StateFlow<Profile.UiState> get() = _uiState.asStateFlow()

    private val _event = Channel<Profile.Event>()
    val event = _event.receiveAsFlow()

    init {

        if (isUpdatingArgument) {
            loadProfile()
        }
    }

    fun onAction(action: Profile.Action) {
        when (action) {

            Profile.Action.OnUpdateProfile -> {
                updateProfile()
            }

            is Profile.Action.OnDisplayNameChanged -> {
                _uiState.update { it.copy(profile = it.profile.copy(displayName = action.displayName)) }
            }

            is Profile.Action.OnDateOfBirthChanged -> {
                _uiState.update { it.copy(profile = it.profile.copy(dob = action.dateOfBirth)) }
            }

            is Profile.Action.OnGenderChanged -> {
                _uiState.update { it.copy(profile = it.profile.copy(gender = action.gender)) }
            }

            is Profile.Action.OnAvatarChanged -> {
                _uiState.update { it.copy(profile = it.profile.copy(avatar = action.avatar?.toString() ?: "")) }
            }

            is Profile.Action.OnPhoneNumberChanged -> {
                _uiState.update { it.copy(profile = it.profile.copy(phoneNumber = action.phoneNumber)) }
            }


            Profile.Action.OnBackAuth -> {
                logOut()
                viewModelScope.launch {
                    _event.send(Profile.Event.NavigateAuth)
                }
            }

            Profile.Action.OnBackLogin -> {
                logOut()
                viewModelScope.launch {
                    _event.send(Profile.Event.NavigateLogin)
                }
            }

            Profile.Action.OnBackSetting -> {
                viewModelScope.launch {
                    _event.send(Profile.Event.BackToSetting)
                }
            }

            Profile.Action.OnBack -> {
                viewModelScope.launch {
                    _event.send(Profile.Event.BackToPrevious)
                }
            }
        }
    }

    fun validate(type: String) {
        val current = _uiState.value
        var displayNameError: String? = current.displayNameError
        var phoneNumberError: String? = current.displayNameError

        when (type) {
            "displayName" -> {
                displayNameError = validateField(
                    current.profile.displayName.trim(),
                    "Tên không hợp lệ"
                ) { it.matches(Regex("^[\\p{L}][\\p{L} .'-]{1,39}$")) }


            }

            "phoneNumber" -> {
                phoneNumberError = validateField(
                    current.profile.phoneNumber.trim(),
                    "Số điện thoại không hợp lệ"
                ) { it.matches(Regex("^0\\d{9}$")) }

            }



        }
        val isValid = current.profile.displayName.isNotBlank() && current.profile.phoneNumber.isNotBlank()  && displayNameError == null && phoneNumberError == null
        _uiState.update {
            it.copy(
                phoneNumberError = phoneNumberError,
                displayNameError = displayNameError,
                isValid = isValid
            )
        }
    }
    private fun logOut(){
        viewModelScope.launch {
            logOutUseCase.invoke().collect{result->
                when(result){
                    is FirebaseResult.Loading -> {

                    }
                    is FirebaseResult.Success -> {

                    }
                    is FirebaseResult.Failure -> {

                    }
                }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            loadProfileUseCase.invoke().collect { result ->
                when (result) {
                    is FirebaseResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is FirebaseResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, profile = result.data) }
                    }

                    is FirebaseResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error) }
                        _event.send(Profile.Event.ShowError)
                    }
                }
            }
        }
    }

    fun updateProfile() {
        viewModelScope.launch {
            updateProfileUseCase.invoke(_uiState.value.profile).collect { result ->
                when (result) {
                    is FirebaseResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is FirebaseResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        if (isUpdatingArgument) {
                            _event.send(Profile.Event.BackToSetting)
                        } else {
                            _event.send(Profile.Event.ShowDialog)
                        }
                    }

                    is FirebaseResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error) }
                        _event.send(Profile.Event.ShowError)
                    }
                }

            }
        }
    }


}

object Profile {
    data class UiState(
        val isUpdating: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val profile: Account = Account(),
        val displayNameError: String?=null,
        val phoneNumberError: String?=null,
        val isValid: Boolean = false,
    )

    sealed interface Event {
        object NavigateAuth : Event
        object NavigateLogin : Event
        object BackToSetting : Event
        object BackToPrevious : Event
        object ShowDialog : Event
        object ShowError : Event

    }

    sealed interface Action {
        data class OnDisplayNameChanged(val displayName: String) : Action
        data class OnDateOfBirthChanged(val dateOfBirth: LocalDate) : Action
        data class OnGenderChanged(val gender: String) : Action
        data class OnPhoneNumberChanged(val phoneNumber: String) : Action
        data class OnAvatarChanged(val avatar: Uri?) : Action
        object OnUpdateProfile : Action
        data object OnBackSetting : Action
        data object OnBackAuth : Action
        data object OnBackLogin : Action
        data object OnBack : Action

    }


}