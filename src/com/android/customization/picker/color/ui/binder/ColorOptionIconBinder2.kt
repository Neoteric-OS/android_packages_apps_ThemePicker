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
 *
 */

package com.android.customization.picker.color.ui.binder

import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel

object ColorOptionIconBinder2 {

    interface Binding {
        /** Destroys the color update binding, in spite of lifecycle state. */
        fun destroy()
    }

    fun bind(
        view: ColorOptionIconView2,
        viewModel: ColorOptionIconViewModel,
        darkTheme: Boolean,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): Binding {
        val binding =
            ColorUpdateBinder.bind(
                setColor = { color -> view.bindStrokeColor(color) },
                color = colorUpdateViewModel.colorPrimary,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        if (darkTheme) {
            view.bindColor(
                viewModel.darkThemeColor0,
                viewModel.darkThemeColor1,
                viewModel.darkThemeColor2,
                viewModel.darkThemeColor3,
            )
        } else {
            view.bindColor(
                viewModel.lightThemeColor0,
                viewModel.lightThemeColor1,
                viewModel.lightThemeColor2,
                viewModel.lightThemeColor3,
            )
        }
        return object : Binding {
            override fun destroy() {
                binding.destroy()
            }
        }
    }
}
