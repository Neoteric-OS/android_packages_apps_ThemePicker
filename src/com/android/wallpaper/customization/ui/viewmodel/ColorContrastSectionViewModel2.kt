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

import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD
import android.util.Log
import com.android.customization.picker.settings.domain.interactor.ColorContrastSectionInteractor
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionDataViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ViewModelScoped
class ColorContrastSectionViewModel2
@Inject
constructor(colorContrastSectionInteractor: ColorContrastSectionInteractor) {

    val summary: Flow<ColorContrastSectionDataViewModel> =
        colorContrastSectionInteractor.contrast.map { contrastValue ->
            when (contrastValue) {
                CONTRAST_LEVEL_STANDARD ->
                    ColorContrastSectionDataViewModel(
                        Text.Resource(R.string.color_contrast_default_title),
                        Icon.Resource(
                            res = R.drawable.ic_contrast_standard,
                            contentDescription = null,
                        ),
                    )
                CONTRAST_LEVEL_MEDIUM ->
                    ColorContrastSectionDataViewModel(
                        Text.Resource(R.string.color_contrast_medium_title),
                        Icon.Resource(
                            res = R.drawable.ic_contrast_medium,
                            contentDescription = null,
                        ),
                    )
                CONTRAST_LEVEL_HIGH ->
                    ColorContrastSectionDataViewModel(
                        Text.Resource(R.string.color_contrast_high_title),
                        Icon.Resource(res = R.drawable.ic_contrast_high, contentDescription = null),
                    )
                else -> {
                    Log.e(TAG, "Invalid contrast value: $contrastValue")
                    throw IllegalArgumentException("Invalid contrast value: $contrastValue")
                }
            }
        }

    companion object {
        private const val TAG = "ColorContrastSectionViewModel2"
    }
}
