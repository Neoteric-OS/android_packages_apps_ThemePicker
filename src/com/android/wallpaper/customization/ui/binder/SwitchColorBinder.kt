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

package com.android.wallpaper.customization.ui.binder

import android.content.res.ColorStateList
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.google.android.material.materialswitch.MaterialSwitch

object SwitchColorBinder {

    private const val COLOR_TRANSPARENT = 0

    interface Binding {
        /** Destroys the binding in spite of lifecycle state. */
        fun destroy()
    }

    /**
     * Binds the color of a [MaterialSwitch] using [ColorUpdateBinder] according to Material 3
     * specs.
     */
    fun bind(
        switch: MaterialSwitch,
        isChecked: Boolean,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        val bindingThumb: ColorUpdateBinder.Binding
        val bindingTrack: ColorUpdateBinder.Binding
        if (isChecked) {
            switch.trackDecorationTintList = ColorStateList.valueOf(COLOR_TRANSPARENT)
            bindingThumb =
                ColorUpdateBinder.bind(
                    setColor = { color -> switch.thumbTintList = ColorStateList.valueOf(color) },
                    color = colorUpdateViewModel.colorOnPrimary,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            bindingTrack =
                ColorUpdateBinder.bind(
                    setColor = { color -> switch.trackTintList = ColorStateList.valueOf(color) },
                    color = colorUpdateViewModel.colorPrimary,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
        } else {
            bindingThumb =
                ColorUpdateBinder.bind(
                    setColor = { color ->
                        switch.thumbTintList = ColorStateList.valueOf(color)
                        switch.trackDecorationTintList = ColorStateList.valueOf(color)
                    },
                    color = colorUpdateViewModel.colorOutline,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            bindingTrack =
                ColorUpdateBinder.bind(
                    setColor = { color -> switch.trackTintList = ColorStateList.valueOf(color) },
                    color = colorUpdateViewModel.colorSurfaceContainerHighest,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
        }
        return object : Binding {
            override fun destroy() {
                bindingThumb.destroy()
                bindingTrack.destroy()
            }
        }
    }
}
