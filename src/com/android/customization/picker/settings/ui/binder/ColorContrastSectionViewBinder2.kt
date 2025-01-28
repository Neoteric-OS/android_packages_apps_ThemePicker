/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.customization.picker.settings.ui.binder

import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.android.themepicker.R
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel

object ColorContrastSectionViewBinder2 {

    private const val TAG = "ColorContrastSectionViewBinder2"

    interface Binding {
        /** Destroys the binding in spite of lifecycle state. */
        fun destroy()
    }

    fun bind(
        view: View,
        contrast: Int,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): Binding {

        val descriptionView: TextView = view.requireViewById(R.id.option_entry_description)
        val iconInner: ImageView = view.requireViewById(R.id.option_entry_icon_inner_part)
        val iconOuter: ImageView = view.requireViewById(R.id.option_entry_icon_outer_part)

        // Bind outer and inner parts of the contrast icon separately. Use the same material color
        // tokens despite contrast level because the tokens adjust according to contrast thanks to
        // dynamic color magic.
        val bindingOuter: ColorUpdateBinder.Binding =
            ColorUpdateBinder.bind(
                setColor = { color -> iconOuter.setColorFilter(color) },
                color = colorUpdateViewModel.colorPrimaryContainer,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val bindingInner: ColorUpdateBinder.Binding =
            ColorUpdateBinder.bind(
                setColor = { color -> iconInner.setColorFilter(color) },
                color = colorUpdateViewModel.colorPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )

        TextViewBinder.bind(
            view = descriptionView,
            viewModel =
                when (contrast) {
                    CONTRAST_LEVEL_STANDARD -> Text.Resource(R.string.color_contrast_default_title)
                    CONTRAST_LEVEL_MEDIUM -> Text.Resource(R.string.color_contrast_medium_title)
                    CONTRAST_LEVEL_HIGH -> Text.Resource(R.string.color_contrast_high_title)
                    else -> {
                        iconInner.isVisible = false
                        iconOuter.isVisible = false
                        Log.e(TAG, "Invalid contrast value: $contrast")
                        throw IllegalArgumentException("Invalid contrast value: $contrast")
                    }
                },
        )

        return object : Binding {
            override fun destroy() {
                bindingInner.destroy()
                bindingOuter.destroy()
            }
        }
    }
}
