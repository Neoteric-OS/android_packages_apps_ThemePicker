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

package com.android.wallpaper.customization.ui.view

import android.widget.TextView
import androidx.core.view.isInvisible
import com.android.systemui.plugins.clocks.ClockFontAxis
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider

class ClockFontSliderViewHolder(val name: TextView, val slider: Slider) {

    fun setIsVisible(isVisible: Boolean) {
        name.isInvisible = !isVisible
        slider.isInvisible = !isVisible
    }

    fun initView(clockFontAxis: ClockFontAxis, onFontAxisValueUpdated: (value: Float) -> Unit) {
        name.text = clockFontAxis.name
        slider.apply {
            valueFrom = clockFontAxis.minValue
            valueTo = clockFontAxis.maxValue
            value = clockFontAxis.currentValue
            labelBehavior = LabelFormatter.LABEL_GONE
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    onFontAxisValueUpdated.invoke(value)
                }
            }
        }
    }

    fun setValue(value: Float) {
        slider.value = value
    }
}
