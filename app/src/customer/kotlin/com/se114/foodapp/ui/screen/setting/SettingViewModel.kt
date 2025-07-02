package com.se114.foodapp.ui.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.foodapp.data.model.Account
import com.example.foodapp.domain.use_case.auth.FirebaseResult
import com.se114.foodapp.domain.use_case.user.LoadProfileUseCase
import com.example.foodapp.domain.use_case.auth.LogOutUseCase
import com.se114.foodapp.domain.use_case.cart.ClearAllCartUseCase
import com.se114.foodapp.domain.use_case.cart.ClearCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val loadProfileUseCase: LoadProfileUseCase,
    private val logoutUseCase: LogOutUseCase,
    private val clearAllCartUseCase: ClearAllCartUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(Setting.UiState())
    val uiState: StateFlow<Setting.UiState> get() = _uiState.asStateFlow()

    private val _event = Channel<Setting.Event>()
    val event = _event.receiveAsFlow()


    fun getProfile() {
        viewModelScope.launch {
            loadProfileUseCase().collect { result ->
                when (result) {
                    is FirebaseResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is FirebaseResult.Success -> {
                        _uiState.update { it.copy(profile = result.data, isLoading = false) }
                    }

                    is FirebaseResult.Failure -> {
                        _uiState.update { it.copy(error = result.error, isLoading = false) }
                        _event.send(Setting.Event.ShowError)
                    }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch() {
            logoutUseCase().collect { result ->
                when (result) {
                    is FirebaseResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is FirebaseResult.Success -> {
                        try {
                            clearAllCartUseCase() // <-- sẽ throw nếu có lỗi
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    error = e.message ?: "Đã có lỗi xảy khi xóa giỏ hàng",
                                    isLoading = false
                                )
                            }
                            _event.send(Setting.Event.ShowError)
                            return@collect
                        }
                        _uiState.update { it.copy(isLoading = false) }
                    }

                    is FirebaseResult.Failure -> {
                        _uiState.update { it.copy(error = result.error, isLoading = false) }
                        _event.send(Setting.Event.ShowError)

                    }
                }
            }
        }
    }

    fun onAction(action: Setting.Action) {
        when (action) {
            is Setting.Action.OnLogout -> {
                logout()
            }

            is Setting.Action.OnProfileClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToProfile)
                }
            }

            is Setting.Action.OnAddressClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToAddress)
                }
            }

            is Setting.Action.OnVoucherClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToVoucher)
                }
            }

            is Setting.Action.OnHelpClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToHelp)
                }
            }

            is Setting.Action.OnContactClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToContact)
                }
            }

            is Setting.Action.OnPrivacyClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToPrivacy)
                }

            }

            Setting.Action.OnLoadProfile -> {
                getProfile()
            }

            Setting.Action.OnLogOutClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.ShowLogoutDialog)
                }
            }

            is Setting.Action.OnSecurityClicked -> {
                viewModelScope.launch {
                    _event.send(Setting.Event.NavigateToSecurity)
                }
            }
        }
    }
}

object Setting {
    data class UiState(
        val profile: Account = Account(),
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    sealed interface Event {
        data object NavigateToSecurity : Event
        data object NavigateToAddress : Event
        data object NavigateToVoucher : Event
        data object NavigateToHelp : Event
        data object NavigateToContact : Event
        data object NavigateToPrivacy : Event
        data object NavigateToProfile : Event
        data object ShowLogoutDialog : Event
        data object ShowError : Event

    }

    sealed interface Action {
        data object OnLogOutClicked : Action
        data object OnLogout : Action
        data object OnLoadProfile : Action
        data object OnProfileClicked : Action
        data object OnAddressClicked : Action
        data object OnVoucherClicked : Action
        data object OnHelpClicked : Action
        data object OnContactClicked : Action
        data object OnPrivacyClicked : Action
        data object OnSecurityClicked : Action

    }
}