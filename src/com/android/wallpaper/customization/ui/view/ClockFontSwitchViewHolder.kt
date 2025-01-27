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
import androidx.core.view.isVisible
import com.android.systemui.plugins.clocks.ClockFontAxis
import com.google.android.material.materialswitch.MaterialSwitch
import kotlin.math.abs

class ClockFontSwitchViewHolder(val name: TextView, val switch: MaterialSwitch) {

    private var switchMaxValue: Float? = null

    fun setIsVisible(isVisible: Boolean) {
        name.isVisible = isVisible
        switch.isVisible = isVisible
    }

    fun initView(clockFontAxis: ClockFontAxis, onFontAxisValueUpdated: (value: Float) -> Unit) {
        switchMaxValue = clockFontAxis.maxValue
        name.text = clockFontAxis.name
        switch.apply {
            isChecked = abs(clockFontAxis.currentValue - clockFontAxis.maxValue) < 0.01f
            setOnCheckedChangeListener { v, _ ->
                val value = if (v.isChecked) clockFontAxis.maxValue else clockFontAxis.minValue
                onFontAxisValueUpdated.invoke(value)
            }
        }
    }

    fun setValue(value: Float) {
        switchMaxValue?.let { switch.isChecked = abs(value - it) < 0.01f }
    }
}
