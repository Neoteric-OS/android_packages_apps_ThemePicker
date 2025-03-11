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
import com.google.android.material.slider.Slider

object SliderColorBinder {

    interface Binding {
        /** Destroys the binding in spite of lifecycle state. */
        fun destroy()
    }

    /** Binds the color of a [Slider] using [ColorUpdateBinder] according to Material 3 specs. */
    fun bind(
        slider: Slider,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        val bindingPrimary =
            ColorUpdateBinder.bind(
                setColor = { color ->
                    slider.apply {
                        trackActiveTintList = ColorStateList.valueOf(color)
                        thumbTintList = ColorStateList.valueOf(color)
                        tickInactiveTintList = ColorStateList.valueOf(color)
                    }
                },
                color = colorUpdateViewModel.colorPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val bindingSurfaceContainerHighest =
            ColorUpdateBinder.bind(
                setColor = { color ->
                    slider.apply {
                        trackInactiveTintList = ColorStateList.valueOf(color)
                        tickActiveTintList = ColorStateList.valueOf(color)
                    }
                },
                color = colorUpdateViewModel.colorSurfaceContainerHighest,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        return object : Binding {
            override fun destroy() {
                bindingPrimary.destroy()
                bindingSurfaceContainerHighest.destroy()
            }
        }
    }
}
