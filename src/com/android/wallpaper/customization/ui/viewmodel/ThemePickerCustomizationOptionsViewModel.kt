/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.customization.ui.viewmodel

import com.android.customization.picker.mode.ui.viewmodel.DarkModeViewModel
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModelFactory
import com.android.wallpaper.picker.customization.ui.viewmodel.DefaultCustomizationOptionsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemePickerCustomizationOptionsViewModel
@AssistedInject
constructor(
    defaultCustomizationOptionsViewModelFactory: DefaultCustomizationOptionsViewModel.Factory,
    keyguardQuickAffordancePickerViewModel2Factory: KeyguardQuickAffordancePickerViewModel2.Factory,
    colorPickerViewModel2Factory: ColorPickerViewModel2.Factory,
    clockPickerViewModelFactory: ClockPickerViewModel.Factory,
    shapeGridPickerViewModelFactory: ShapeGridPickerViewModel.Factory,
    val colorContrastSectionViewModel: ColorContrastSectionViewModel2,
    val darkModeViewModel: DarkModeViewModel,
    val themedIconViewModel: ThemedIconViewModel,
    @Assisted private val viewModelScope: CoroutineScope,
) : CustomizationOptionsViewModel {

    private val defaultCustomizationOptionsViewModel =
        defaultCustomizationOptionsViewModelFactory.create(viewModelScope)

    val clockPickerViewModel = clockPickerViewModelFactory.create(viewModelScope = viewModelScope)
    val keyguardQuickAffordancePickerViewModel2 =
        keyguardQuickAffordancePickerViewModel2Factory.create(viewModelScope = viewModelScope)
    val colorPickerViewModel2 = colorPickerViewModel2Factory.create(viewModelScope = viewModelScope)
    val shapeGridPickerViewModel =
        shapeGridPickerViewModelFactory.create(viewModelScope = viewModelScope)

    private var onApplyJob: Job? = null

    override val selectedOption = defaultCustomizationOptionsViewModel.selectedOption

    override fun handleBackPressed(): Boolean {

        if (
            defaultCustomizationOptionsViewModel.selectedOption.value ==
                ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK &&
                clockPickerViewModel.selectedTab.value == ClockPickerViewModel.Tab.FONT
        ) {
            clockPickerViewModel.cancelFontAxes()
            return true
        }

        return defaultCustomizationOptionsViewModel.handleBackPressed()
    }

    override fun resetPreview() {
        defaultCustomizationOptionsViewModel.resetPreview()

        keyguardQuickAffordancePickerViewModel2.resetPreview()
        shapeGridPickerViewModel.resetPreview()
        clockPickerViewModel.resetPreview()
        colorPickerViewModel2.resetPreview()
        darkModeViewModel.resetPreview()
    }

    val onCustomizeClockClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeShortcutClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
                            .SHORTCUTS
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeColorsClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeShapeGridClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
                            .APP_SHAPE_GRID
                    )
                }
            } else {
                null
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val onApplyButtonClicked: Flow<((onComplete: () -> Unit) -> Unit)?> =
        selectedOption
            .flatMapLatest {
                when (it) {
                    ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK ->
                        clockPickerViewModel.onApply
                    ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
                        .SHORTCUTS -> keyguardQuickAffordancePickerViewModel2.onApply
                    ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
                        .APP_SHAPE_GRID -> shapeGridPickerViewModel.onApply
                    ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS ->
                        combine(colorPickerViewModel2.onApply, darkModeViewModel.onApply) {
                            colorOnApply,
                            darkModeOnApply ->
                            if (colorOnApply == null && darkModeOnApply == null) {
                                null
                            } else {
                                {
                                    colorOnApply?.invoke()
                                    darkModeOnApply?.invoke()
                                }
                            }
                        }
                    else -> flow { emit(null) }
                }
            }
            .map { onApply ->
                if (onApply != null) {
                    fun(onComplete: () -> Unit) {
                        // Prevent double apply
                        if (onApplyJob?.isActive != true) {
                            onApplyJob =
                                viewModelScope.launch {
                                    onApply()
                                    onComplete()
                                    onApplyJob = null
                                }
                        }
                    }
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isApplyButtonEnabled: Flow<Boolean> = onApplyButtonClicked.map { it != null }

    val isApplyButtonVisible: Flow<Boolean> = selectedOption.map { it != null }

    val isToolbarCollapsed: Flow<Boolean> =
        combine(selectedOption, clockPickerViewModel.selectedTab) { selectedOption, selectedTab ->
                selectedOption ==
                    ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK &&
                    selectedTab == ClockPickerViewModel.Tab.FONT
            }
            .distinctUntilChanged()

    @ViewModelScoped
    @AssistedFactory
    interface Factory : CustomizationOptionsViewModelFactory {
        override fun create(
            viewModelScope: CoroutineScope
        ): ThemePickerCustomizationOptionsViewModel
    }
}
