package com.se114.foodapp.ui.screen.menu.food_details

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.foodapp.data.dto.ApiResponse
import com.example.foodapp.data.model.Food
import com.example.foodapp.data.model.Menu
import com.example.foodapp.domain.use_case.food.GetMenusUseCase
import com.example.foodapp.navigation.FoodDetailsAdmin
import com.example.foodapp.navigation.FoodNavType
import com.example.foodapp.ui.screen.components.validateField
import com.se114.foodapp.data.mapper.toFoodAddUi
import com.se114.foodapp.domain.use_case.food.CreateFoodUseCase
import com.se114.foodapp.domain.use_case.food.UpdateFoodUseCase
import com.se114.foodapp.ui.screen.menu.FoodListAdmin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.math.log
import kotlin.reflect.typeOf

@HiltViewModel
class FoodDetailsAdminViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val createFoodUseCase: CreateFoodUseCase,
    private val updateFoodUseCase: UpdateFoodUseCase,
    private val getMenusUseCase: GetMenusUseCase,
) : ViewModel() {
    private val foodArgument  = savedStateHandle.toRoute<FoodDetailsAdmin>(
        typeMap = mapOf(typeOf<Food>() to FoodNavType)
    )
    private val food = foodArgument.food
    private val isUpdating = foodArgument.isUpdating

    private val _uiState =
        MutableStateFlow(AddFood.UiState(foodAddUi = food.toFoodAddUi(), isUpdating = isUpdating))
    val uiState: StateFlow<AddFood.UiState> get() = _uiState.asStateFlow()

    private val _event = Channel<AddFood.Event>()
    val event = _event.receiveAsFlow()

    init {
        getMenus()
    }

    private fun getMenus() {
        viewModelScope.launch {
            getMenusUseCase.invoke().collect { result ->
                when (result) {
                    is ApiResponse.Loading -> {
                        _uiState.update { it.copy(menuState = FoodListAdmin.MenuSate.Loading) }
                    }

                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                menus = result.data,
                                menuState = FoodListAdmin.MenuSate.Success
                            )
                        }
                    }

                    is ApiResponse.Failure -> {
                        _uiState.update { it.copy(menuState = FoodListAdmin.MenuSate.Error(result.errorMessage)) }
                    }
                }
            }
        }
    }
    private fun createFood() {
        viewModelScope.launch {
            Log.d("FoodDetailsAdminViewModel", "createFood: ${_uiState.value.foodAddUi.images}")
            createFoodUseCase.invoke(uiState.value.foodAddUi).collect { result ->
                when (result) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                            )
                        }
                        _event.send(AddFood.Event.OnBackAfterUpdate)
                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Thêm món ăn thất bại: ${result.errorMessage}"
                            )
                        }
                        _event.send(AddFood.Event.ShowError)
                    }

                    is ApiResponse.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }

            }

        }
    }

    private fun updateFood() {

        viewModelScope.launch {
            updateFoodUseCase(uiState.value.foodAddUi).collect { result ->
                when (result) {
                    is ApiResponse.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                            )
                        }
                        _event.send(AddFood.Event.OnBackAfterUpdate)

                    }

                    is ApiResponse.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Cập nhật món ăn thất bại: ${result.errorMessage}"
                            )
                        }
                        _event.send(AddFood.Event.ShowError)
                    }
                    is ApiResponse.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }

        }
    }
    fun validate(type: String) {
        val current = _uiState.value
        var nameError: String? = current.nameError
        var priceError: String? = current.priceError

        var defaultQuantityError: String? = current.defaultQuantityError
        when (type) {
            "name" -> {
                nameError = validateField(
                    current.foodAddUi.name.trim(),
                    "Tên không hợp lệ"
                ) { it.matches(Regex("^[\\p{L}][\\p{L} .'-]{1,39}$")) }


            }

            "price" -> {
                priceError = validateField(
                    current.foodAddUi.price.toPlainString().trim(),
                    "Giá phải lớn hơn 0"
                ) { it.toBigDecimal() > BigDecimal.ZERO } }

            "defaultQuantity" -> {
                defaultQuantityError = validateField(
                    current.foodAddUi.defaultQuantity.toString().trim(),
                    "Số lượng phải lớn hơn 0"
                ) { it.toInt() > 0 }}

        }
        val isValid = current.foodAddUi.name.isNotBlank() && current.foodAddUi.price > BigDecimal.ZERO && current.foodAddUi.defaultQuantity > 0 && current.foodAddUi.description.isNotBlank() && nameError == null && priceError == null && defaultQuantityError == null
        _uiState.update {
            it.copy(
                nameError = nameError,
                priceError = priceError,

                defaultQuantityError = defaultQuantityError,
                isValid = isValid
            )
        }
    }

    fun onAction(action: AddFood.Action) {
        when (action) {
            is AddFood.Action.AddFood -> {
                createFood()
            }
            is AddFood.Action.UpdateFood -> {
                updateFood()
            }
            is AddFood.Action.OnNameChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(name = action.name)) }
            }
            is AddFood.Action.OnMenuChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(menuId = action.menuId, menuName = action.menuName)) }
            }
            is AddFood.Action.OnDescriptionChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(description = action.description)) }
            }
            is AddFood.Action.OnPriceChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(price = action.price?: BigDecimal.ZERO)) }
            }

            is AddFood.Action.OnDefaultQuantityChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(defaultQuantity = action.defaultQuantity?: 0)) }
            }
            is AddFood.Action.OnImagesChange -> {
                _uiState.update { it.copy(foodAddUi = it.foodAddUi.copy(images = action.images) ) }
            }
            is AddFood.Action.OnBack -> {
                viewModelScope.launch {
                    _event.send(AddFood.Event.OnBack)
                }
            }
        }
    }
}




object AddFood {
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val foodAddUi: FoodAddUi,
        val isUpdating: Boolean = false,
        val menus: List<Menu> = emptyList(),
        val menuState: FoodListAdmin.MenuSate = FoodListAdmin.MenuSate.Loading,
        val nameError: String? = null,
        val isValid: Boolean = false,
        val priceError: String? = null,

        val defaultQuantityError: String? = null,

    )
    sealed interface MenuSate{
        data object Loading: MenuSate
        data object Success: MenuSate
        data class Error(val message: String): MenuSate
    }

    sealed interface Event {
        data object OnBack : Event
        data object OnBackAfterUpdate : Event
        data object ShowError : Event
    }

    sealed interface Action {
        data object AddFood : Action
        data object UpdateFood : Action
        data class OnNameChange(val name: String) : Action
        data class OnMenuChange(val menuId: Int, val menuName: String) : Action
        data class OnDescriptionChange(val description: String) : Action
        data class OnPriceChange(val price: BigDecimal?) : Action
        data class OnDefaultQuantityChange(val defaultQuantity: Int?) : Action
        data class OnImagesChange(val images: List<Uri>) : Action
        data object OnBack : Action

    }
}

data class FoodAddUi(
    val id: Long ?= null,
    val menuName: String?=null,
    val name: String = "",
    val menuId: Int = 1,
    val description: String = "",
    val images: List<Uri>? = emptyList(),
    val price: BigDecimal = BigDecimal.ZERO,
    val defaultQuantity: Int = 1,
)