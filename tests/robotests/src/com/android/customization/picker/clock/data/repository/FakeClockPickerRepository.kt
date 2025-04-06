/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.customization.picker.clock.data.repository

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import com.android.customization.picker.clock.data.repository.FakeClockPickerRepository.Companion.fakeClocks
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.systemui.plugins.clocks.AxisPresetConfig
import com.android.systemui.plugins.clocks.AxisPresetConfig.Group
import com.android.systemui.plugins.clocks.AxisType
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.systemui.plugins.clocks.ClockFontAxis
import com.android.systemui.plugins.clocks.ClockId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/** By default [FakeClockPickerRepository] uses [fakeClocks]. */
open class FakeClockPickerRepository(clocks: List<ClockMetadataModel> = fakeClocks) :
    ClockPickerRepository {
    override val allClocks: Flow<List<ClockMetadataModel>> = MutableStateFlow(clocks).asStateFlow()

    private val selectedClockId: MutableStateFlow<String> = MutableStateFlow(fakeClocks[0].clockId)
    @ColorInt private val selectedColorId: MutableStateFlow<String?> = MutableStateFlow(null)
    private val colorTone: MutableStateFlow<Int> =
        MutableStateFlow(ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS)
    @ColorInt private val seedColor: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val clockListFlow: MutableStateFlow<List<ClockMetadataModel>> =
        MutableStateFlow(fakeClocks)
    override val selectedClock: Flow<ClockMetadataModel> =
        combine(clockListFlow, selectedClockId, selectedColorId, colorTone, seedColor) {
            clockList,
            selectedClockId,
            selectedColor,
            colorTone,
            seedColor ->
            val selectedClock = clockList.find { clock -> clock.clockId == selectedClockId }
            checkNotNull(selectedClock)
            ClockMetadataModel(
                clockId = selectedClock.clockId,
                isSelected = true,
                description = "description",
                thumbnail = ColorDrawable(0),
                isReactiveToTone = selectedClock.isReactiveToTone,
                axisPresetConfig = selectedClock.axisPresetConfig,
                selectedColorId = selectedColor,
                colorToneProgress = colorTone,
                seedColor = seedColor,
            )
        }

    private val _selectedClockSize = MutableStateFlow(ClockSize.DYNAMIC)
    override val selectedClockSize: Flow<ClockSize> = _selectedClockSize.asStateFlow()

    override suspend fun setSelectedClock(clockId: String) {
        selectedClockId.value = clockId
    }

    override suspend fun setClockColor(
        selectedColorId: String?,
        @IntRange(from = 0, to = 100) colorToneProgress: Int,
        @ColorInt seedColor: Int?,
    ) {
        this.selectedColorId.value = selectedColorId
        this.colorTone.value = colorToneProgress
        this.seedColor.value = seedColor
    }

    override suspend fun setClockSize(size: ClockSize) {
        _selectedClockSize.value = size
    }

    override suspend fun setClockAxisStyle(axisStyle: ClockAxisStyle) {
        val clockList = clockListFlow.value
        val newClockList = clockList.toMutableList()
        for ((index, clock) in clockList.withIndex()) {
            val presetConfig = clock.axisPresetConfig
            val style = presetConfig?.findStyle(axisStyle)
            if (presetConfig != null && style != null) {
                newClockList[index] =
                    clock.copy(axisPresetConfig = presetConfig.copy(current = style))
            }
        }

        clockListFlow.value = newClockList.toList()
    }

    override fun isReactiveToTone(clockId: ClockId): Boolean? = true

    companion object {
        fun buildFakeClockAxisStyle(i: Int): ClockAxisStyle {
            return ClockAxisStyle(listOf(buildFakeAxis(i)))
        }

        private fun buildFakeAxis(i: Int): ClockFontAxis {
            return ClockFontAxis(
                key = "key#$i",
                type = AxisType.Float,
                maxValue = 0f,
                minValue = 1000f,
                currentValue = 50f * (i + 1),
                name = "FakeAxis",
                description = "Axis Description",
            )
        }

        private val fakeClockAxisStyle0 = ClockAxisStyle(listOf(buildFakeAxis(0)))
        val fakeClockAxisStyle1 = ClockAxisStyle(listOf(buildFakeAxis(1)))
        private val fakeClockAxisStyle2 = ClockAxisStyle(listOf(buildFakeAxis(2)))
        private val fakeClockAxisStyle3 = ClockAxisStyle(listOf(buildFakeAxis(3)))
        private val fakeAxisPresetConfig: AxisPresetConfig =
            AxisPresetConfig(
                groups =
                    listOf(
                        AxisPresetConfig.Group(
                            presets = listOf(fakeClockAxisStyle0, fakeClockAxisStyle1),
                            icon = ColorDrawable(Color.BLUE),
                        ),
                        AxisPresetConfig.Group(
                            presets = listOf(fakeClockAxisStyle2, fakeClockAxisStyle3),
                            icon = ColorDrawable(Color.YELLOW),
                        ),
                    ),
                current =
                    AxisPresetConfig.IndexedStyle(
                        groupIndex = 0,
                        presetIndex = 0,
                        style = fakeClockAxisStyle0,
                    ),
            )

        const val CLOCK_ID_0 = "clock0"
        const val CLOCK_ID_1 = "clock1"
        const val CLOCK_ID_2 = "clock2"
        const val CLOCK_ID_3 = "clock3"

        val fakeClocks =
            listOf(
                ClockMetadataModel(
                    CLOCK_ID_0,
                    true,
                    "description0",
                    ColorDrawable(0),
                    true,
                    fakeAxisPresetConfig,
                    null,
                    50,
                    null,
                ),
                ClockMetadataModel(
                    CLOCK_ID_1,
                    false,
                    "description1",
                    ColorDrawable(0),
                    true,
                    null,
                    null,
                    50,
                    null,
                ),
                ClockMetadataModel(
                    CLOCK_ID_2,
                    false,
                    "description2",
                    ColorDrawable(0),
                    true,
                    null,
                    null,
                    50,
                    null,
                ),
                ClockMetadataModel(
                    CLOCK_ID_3,
                    false,
                    "description3",
                    ColorDrawable(0),
                    false,
                    null,
                    null,
                    50,
                    null,
                ),
            )
        const val CLOCK_COLOR_ID = "RED"
        const val CLOCK_COLOR_TONE_PROGRESS = 87
        const val SEED_COLOR = Color.RED
    }
}
