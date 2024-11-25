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

import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isInvisible
import com.android.systemui.plugins.clocks.ClockFontAxis

class ClockFontSliderViewHolder(val name: TextView, val slider: SeekBar) {

    fun setIsVisible(isVisible: Boolean) {
        name.isInvisible = !isVisible
        slider.isInvisible = !isVisible
    }

    fun initView(clockFontAxis: ClockFontAxis, onFontAxisValueUpdated: (value: Float) -> Unit) {
        name.text = clockFontAxis.name
        slider.apply {
            max = clockFontAxis.maxValue.toInt()
            min = clockFontAxis.minValue.toInt()
            progress = clockFontAxis.currentValue.toInt()
            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean,
                    ) {
                        if (fromUser) {
                            onFontAxisValueUpdated.invoke(progress.toFloat())
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }
    }

    fun setValue(value: Float) {
        slider.progress = value.toInt()
    }
}
