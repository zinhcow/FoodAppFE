package com.se114.foodapp.ui.screen.warehouse.material

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.dto.ApiResponse

import com.example.foodapp.data.model.Ingredient
import com.example.foodapp.data.model.Menu
import com.example.foodapp.data.model.Unit
import com.se114.foodapp.domain.repository.IngredientRepository
import com.se114.foodapp.domain.use_case.ingredient.CreateIngredientUseCase
import com.se114.foodapp.domain.use_case.ingredient.CreateUnitUseCase
import com.se114.foodapp.domain.use_case.ingredient.DeleteIngredientUseCase
import com.se114.foodapp.domain.use_case.ingredient.DeleteUnitUseCase
import com.se114.foodapp.domain.use_case.ingredient.GetActiveIngredientsUseCase
import com.se114.foodapp.domain.use_case.ingredient.GetActiveUnitsUseCase
import com.se114.foodapp.domain.use_case.ingredient.GetHiddenIngredientsUseCase
import com.se114.foodapp.domain.use_case.ingredient.GetHiddenUnitsUseCase
import com.se114.foodapp.domain.use_case.ingredient.RecoverUnitUseCase
import com.se114.foodapp.domain.use_case.ingredient.SetActiveIngredientUseCase
import com.se114.foodapp.domain.use_case.ingredient.UpdateIngredientUseCase
import com.se114.foodapp.domain.use_case.ingredient.UpdateUnitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    private val getActiveIngredientsUseCase: GetActiveIngredientsUseCase,
    private val getHiddenIngredientsUseCase: GetHiddenIngredientsUseCase,
    private val getActiveUnitsUseCase: GetActiveUnitsUseCase,
    private val getHiddenUnitsUseCase: GetHiddenUnitsUseCase,
    private val createUnitUseCase: CreateUnitUseCase,
    private val createIngredientUseCase: CreateIngredientUseCase,
    private val updateUnitUseCase: UpdateUnitUseCase,
    private val updateIngredientUseCase: UpdateIngredientUseCase,
    private val deleteUnitUseCase: DeleteUnitUseCase,
    private val deleteIngredientUseCase: DeleteIngredientUseCase,
    private val recoverUnitUseCase: RecoverUnitUseCase,
    private val setActiveIngredientUseCase: SetActiveIngredientUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MaterialState.UiState())
    val uiState get() = _uiState.asStateFlow()

    private val _event = Channel<MaterialState.Event>()
    val event get() = _event.receiveAsFlow()



    fun getAllUnits() {
        viewModelScope.launch {
            combine(
                getActiveUnitsUseCase.invoke(),
                getHiddenUnitsUseCase.invoke()
            ) { activeResponse, hiddenResponse ->
                Pair(activeResponse, hiddenResponse)
            }.collect { (activeResult, hiddenResult) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        activeUnits = (activeResult as? ApiResponse.Success)?.data
                            ?: currentState.activeUnits,
                        hiddenUnits = (hiddenResult as? ApiResponse.Success)?.data
                            ?: currentState.hiddenUnits,
                        activeUnitsState = when (activeResult) {
                            is ApiResponse.Success -> MaterialState.ActiveUnits.Success
                            is ApiResponse.Failure -> MaterialState.ActiveUnits.Error(activeResult.errorMessage)
                            is ApiResponse.Loading -> MaterialState.ActiveUnits.Loading
                        },
                        hiddenUnitsState = when (hiddenResult) {
                            is ApiResponse.Success -> MaterialState.HiddenUnits.Success
                            is ApiResponse.Failure -> MaterialState.HiddenUnits.Error(hiddenResult.errorMessage)
                            is ApiResponse.Loading -> MaterialState.HiddenUnits.Loading
                        }
                    )
                }
            }
        }
    }

    fun getAllIngredients() {
        viewModelScope.launch {
            combine(
                getActiveIngredientsUseCase.invoke(),
                getHiddenIngredientsUseCase.invoke()
            ) { activeResponse, hiddenResponse ->
                Pair(activeResponse, hiddenResponse)
            }.collect { (activeResponse, hiddenResponse) ->
                _uiState.update { currentState ->
                    currentState.copy(

                        activeIngredients = (activeResponse as? ApiResponse.Success)?.data
                            ?: currentState.activeIngredients,
                        activeIngredientsState = when (activeResponse) {
                            is ApiResponse.Success -> MaterialState.ActiveIngredients.Success
                            is ApiResponse.Failure -> MaterialState.ActiveIngredients.Error(
                                activeResponse.errorMessage
                            )

                            is ApiResponse.Loading -> MaterialState.ActiveIngredients.Loading
                        },

                        hiddenIngredients = (hiddenResponse as? ApiResponse.Success)?.data
                            ?: currentState.hiddenIngredients,
                        hiddenIngredientsState = when (hiddenResponse) {
                            is ApiResponse.Success -> MaterialState.HiddenIngredients.Success
                            is ApiResponse.Failure -> MaterialState.HiddenIngredients.Error(
                                hiddenResponse.errorMessage
                            )

                            is ApiResponse.Loading -> MaterialState.HiddenIngredients.Loading
                        }
                    )
                }
            }
        }
    }


    private fun createUnit() {
        viewModelScope.launch {
            createUnitUseCase.invoke(_uiState.value.unitSelected.name).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeUnits = it.activeUnits + response.data,
                            )
                        }
                        _event.send(MaterialState.Event.ShowSuccessToast("Thêm đơn vị thành công"))

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.errorMessage
                            )
                        }
                        _event.send(MaterialState.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createIngredient() {
        viewModelScope.launch {
            createIngredientUseCase.invoke(_uiState.value.ingredientSelected).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeIngredients = it.activeIngredients + response.data,
                            )
                        }
                        _event.send(MaterialState.Event.ShowSuccessToast("Thêm nguyên liệu thành công"))

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.errorMessage
                            )
                        }
                        _event.send(MaterialState.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }
                }

            }
        }
    }

    private fun updateUnit() {
        viewModelScope.launch {
            updateUnitUseCase.invoke(
                _uiState.value.unitSelected.id!!,
                _uiState.value.unitSelected.name
            ).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeUnits = it.activeUnits.map {
                                    if (it.id == response.data.id) response.data else it
                                }
                            )
                        }
                        _event.send(MaterialState.Event.ShowSuccessToast("Cập nhật đơn vị thành công"))

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.errorMessage
                            )
                        }
                        _event.send(MaterialState.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }

                }

            }
        }
    }

    private fun updateIngredient() {
        viewModelScope.launch {
            updateIngredientUseCase.invoke(
                _uiState.value.ingredientSelected
            ).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeIngredients = it.activeIngredients.map {
                                    if (it.id == response.data.id) response.data else it
                                }
                            )
                        }
                        _event.send(MaterialState.Event.ShowSuccessToast("Cập nhật nguyên liệu thành công"))

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.errorMessage
                            )
                        }
                        _event.send(MaterialState.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }

                }

            }
        }
    }

    private fun deleteUnit() {
        viewModelScope.launch {
            deleteUnitUseCase.invoke(_uiState.value.unitSelected.id!!).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activeUnits = it.activeUnits.filter { it.id != _uiState.value.unitSelected.id }
                            )
                        }
                        _event.send(MaterialState.Event.ShowSuccessToast("Xóa đơn vị thành công"))

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = response.errorMessage
                            )
                        }
                        _event.send(MaterialState.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update {
                            it.copy(
                                isLoading = true
                            )
                        }
                    }


                }
            }
        }
    }

    private fun deleteIngredient() {
        viewModelScope.launch {
            deleteIngredientUseCase.invoke(_uiState.value.ingredientSelected.id!!)
                .collect { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    activeIngredients = it.activeIngredients.filter { it.id != _uiState.value.ingredientSelected.id }
                                )
                            }
                            _event.send(MaterialState.Event.ShowSuccessToast("Xóa nguyên liệu thành công"))

                        }

                        is ApiResponse.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.errorMessage
                                )
                            }
                            _event.send(MaterialState.Event.ShowError)
                        }

                        is ApiResponse.Loading -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = true
                                )
                            }

                        }
                    }
                }

        }
    }

    private fun recoverUnit(isActive: Boolean) {
        viewModelScope.launch {
            recoverUnitUseCase.invoke(_uiState.value.unitSelected.id!!, isActive)
                .collect { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                )
                            }
                            _event.send(MaterialState.Event.ShowSuccessToast("Cập nhật trạng thái đơn vị thành công"))
                            getAllUnits()
                        }

                        is ApiResponse.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.errorMessage
                                )
                            }
                            _event.send(MaterialState.Event.ShowError)
                        }

                        is ApiResponse.Loading -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = true
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun setActiveIngredient(isActive: Boolean) {
        viewModelScope.launch {
            setActiveIngredientUseCase.invoke(_uiState.value.ingredientSelected.id!!, isActive)
                .collect { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                )
                            }
                            _event.send(MaterialState.Event.ShowSuccessToast("Cập nhật trạng thái nguyên liệu thành công"))
                            getAllIngredients()
                        }

                        is ApiResponse.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.errorMessage
                                )
                            }
                            _event.send(MaterialState.Event.ShowError)
                        }

                        is ApiResponse.Loading -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = true
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onAction(action: MaterialState.Action) {
        when (action) {
            is MaterialState.Action.OnBack -> {
                viewModelScope.launch {
                    _event.send(MaterialState.Event.OnBack)
                }
            }

            is MaterialState.Action.OnAddIngredient -> {
                createIngredient()
            }

            is MaterialState.Action.OnAddUnit -> {
                createUnit()
            }

            is MaterialState.Action.OnUpdateIngredient -> {
                updateIngredient()
            }

            is MaterialState.Action.OnUpdateUnit -> {
                updateUnit()
            }

            is MaterialState.Action.DeleteIngredient -> {
                deleteIngredient()
            }

            is MaterialState.Action.DeleteUnit -> {
                deleteUnit()
            }

            is MaterialState.Action.SetActiveIngredient -> {
                setActiveIngredient(action.isActive)
            }

            is MaterialState.Action.OnRecoverUnit -> {
                recoverUnit(action.isActive)
            }

            is MaterialState.Action.OnChangeIngredientName -> {
                _uiState.update { it.copy(ingredientSelected = it.ingredientSelected.copy(name = action.ingredientName)) }
            }

            is MaterialState.Action.OnChangeIngredientUnitId -> {
                _uiState.update { it.copy(ingredientSelected = it.ingredientSelected.copy(unitId = action.unitId)) }
            }

            is MaterialState.Action.OnChangeUnitName -> {
                _uiState.update { it.copy(unitSelected = it.unitSelected.copy(name = action.unitName)) }
            }

            is MaterialState.Action.OnUnitSelected -> {
                _uiState.update { it.copy(unitSelected = action.unit) }
            }

            is MaterialState.Action.OnIngredientSelected -> {
                _uiState.update { it.copy(ingredientSelected = action.ingredient) }
            }

            is MaterialState.Action.OnEditState -> {
                _uiState.update {
                    it.copy(
                        editState = it.editState.copy(
                            isUpdating = action.isUpdating,
                            isActive = action.isActive,
                            isUnit = action.isUnit
                        )
                    )
                }
            }


        }
    }
}


object MaterialState {
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val activeUnits: List<Unit> = emptyList(),
        val activeUnitsState: ActiveUnits = ActiveUnits.Loading,
        val hiddenUnits: List<Unit> = emptyList(),
        val hiddenUnitsState: HiddenUnits = HiddenUnits.Loading,
        val activeIngredients: List<Ingredient> = emptyList(),
        val activeIngredientsState: ActiveIngredients = ActiveIngredients.Loading,
        val hiddenIngredients: List<Ingredient> = emptyList(),
        val hiddenIngredientsState: HiddenIngredients = HiddenIngredients.Loading,
        val unitSelected: Unit = Unit(),
        val ingredientSelected: Ingredient = Ingredient(),
        val editState: EditState = EditState(),
        
        )

    data class EditState(
        val isUpdating: Boolean = false,
        val isActive: Boolean = false,
        val isUnit: Boolean = true,
    )

    sealed interface ActiveUnits {
        object Success : ActiveUnits
        data class Error(val message: String) : ActiveUnits
        object Loading : ActiveUnits
    }

    sealed interface HiddenUnits {
        object Success : HiddenUnits
        data class Error(val message: String) : HiddenUnits
        object Loading : HiddenUnits
    }

    sealed interface ActiveIngredients {
        object Success : ActiveIngredients
        data class Error(val message: String) : ActiveIngredients
        object Loading : ActiveIngredients
    }

    sealed interface HiddenIngredients {
        object Success : HiddenIngredients
        data class Error(val message: String) : HiddenIngredients
        object Loading : HiddenIngredients
    }

    sealed interface Event {
        data object ShowError : Event
        data object OnBack : Event
        data class ShowSuccessToast(val message: String) : Event

    }

    sealed interface Action {
        object OnBack : Action
        object OnAddUnit : Action
        object OnAddIngredient : Action
        object OnUpdateUnit : Action
        object OnUpdateIngredient : Action
        data class OnRecoverUnit(val isActive: Boolean) : Action
        data class SetActiveIngredient(val isActive: Boolean) : Action
        object DeleteUnit : Action
        object DeleteIngredient : Action
        data class OnChangeUnitName(val unitName: String) : Action
        data class OnChangeIngredientName(val ingredientName: String) : Action
        data class OnChangeIngredientUnitId(val unitId: Long) : Action
        data class OnUnitSelected(val unit: Unit) : Action
        data class OnIngredientSelected(val ingredient: Ingredient) : Action
        data class OnEditState(
            val isUpdating: Boolean,
            val isActive: Boolean,
            val isUnit: Boolean,
        ) : Action



    }
}