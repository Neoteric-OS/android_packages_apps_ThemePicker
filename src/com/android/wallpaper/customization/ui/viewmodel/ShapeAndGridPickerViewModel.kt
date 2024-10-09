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

import android.content.Context
import android.content.res.Resources
import com.android.customization.model.ResourceConstants
import com.android.customization.model.grid.GridOptionModel
import com.android.customization.picker.grid.domain.interactor.GridInteractor2
import com.android.customization.picker.grid.ui.viewmodel.GridIconViewModel
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class ShapeAndGridPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val context: Context,
    interactor: GridInteractor2,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    // The currently-set system grid option
    val selectedGridOption =
        interactor.selectedGridOption
            .filterNotNull()
            .map { toOptionItemViewModel(it) }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingGridOptionKey = MutableStateFlow<String?>(null)
    // If the previewing key is null, use the currently-set system grid option
    val previewingGridOptionKey =
        combine(overridingGridOptionKey, selectedGridOption) {
            overridingGridOptionKey,
            selectedGridOption ->
            overridingGridOptionKey ?: selectedGridOption.key.value
        }

    fun resetPreview() {
        overridingGridOptionKey.value = null
    }

    val optionItems: Flow<List<OptionItemViewModel<GridIconViewModel>>> =
        interactor.gridOptions
            .filterNotNull()
            .map { gridOptions -> gridOptions.map { toOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(previewingGridOptionKey, selectedGridOption) {
            previewingGridOptionKey,
            selectedGridOption ->
            if (previewingGridOptionKey == selectedGridOption.key.value) {
                null
            } else {
                { interactor.applySelectedOption(previewingGridOptionKey) }
            }
        }

    private fun toOptionItemViewModel(
        option: GridOptionModel
    ): OptionItemViewModel<GridIconViewModel> {
        val iconShapePath =
            context.resources.getString(
                Resources.getSystem()
                    .getIdentifier(
                        ResourceConstants.CONFIG_ICON_MASK,
                        "string",
                        ResourceConstants.ANDROID_PACKAGE,
                    )
            )
        val isSelected =
            previewingGridOptionKey
                .map { it == option.key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )

        return OptionItemViewModel(
            key = MutableStateFlow(option.key),
            payload =
                GridIconViewModel(columns = option.cols, rows = option.rows, path = iconShapePath),
            text = Text.Loaded(option.title),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingGridOptionKey.value = option.key }
                    } else {
                        null
                    }
                },
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ShapeAndGridPickerViewModel
    }
}
