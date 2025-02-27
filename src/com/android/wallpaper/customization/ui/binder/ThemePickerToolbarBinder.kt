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

package com.android.wallpaper.customization.ui.binder

import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.themepicker.R as ThemePickerR
import com.android.wallpaper.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_SHAPE_GRID
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.SHORTCUTS
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultToolbarBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Singleton
class ThemePickerToolbarBinder
@Inject
constructor(private val defaultToolbarBinder: DefaultToolbarBinder) : ToolbarBinder {

    override fun bind(
        navButton: FrameLayout,
        toolbar: Toolbar,
        applyButton: Button,
        viewModel: CustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        onNavBack: () -> Unit,
    ) {
        defaultToolbarBinder.bind(
            navButton,
            toolbar,
            applyButton,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            onNavBack,
        )

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(applyButton.background), color)
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = { true },
            lifecycleOwner = lifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color -> applyButton.setTextColor(color) },
            color =
                combine(
                    viewModel.isApplyButtonEnabled,
                    colorUpdateViewModel.colorOnPrimary,
                    colorUpdateViewModel.colorOnSurface,
                ) { enabled, onPrimary, onSurface ->
                    if (enabled) {
                        onPrimary
                    } else {
                        ColorUtils.setAlphaComponent(onSurface, 97) // 97 for 38% transparent
                    }
                },
            shouldAnimate = { false },
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onApplyButtonClicked.collect { onApplyButtonClicked ->
                        applyButton.setOnClickListener { onApplyButtonClicked?.invoke(onNavBack) }
                    }
                }

                launch { viewModel.isApplyButtonVisible.collect { applyButton.isInvisible = !it } }

                launch {
                    viewModel.isApplyButtonEnabled.collect {
                        applyButton.isEnabled = it
                        applyButton.background.alpha =
                            if (it) 255 else 31 // 255 for 100%, 31 for 12% transparent
                    }
                }

                launch {
                    viewModel.selectedOption.collect {
                        val stringResId =
                            when (it) {
                                COLORS -> ThemePickerR.string.system_colors_title
                                APP_SHAPE_GRID -> ThemePickerR.string.shape_and_grid_title
                                CLOCK -> ThemePickerR.string.clock_title
                                SHORTCUTS ->
                                    ThemePickerR.string.keyguard_quick_affordance_section_title
                                else -> R.string.app_name
                            }
                        toolbar.title = toolbar.resources.getString(stringResId)
                    }
                }
            }
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
    }
}
