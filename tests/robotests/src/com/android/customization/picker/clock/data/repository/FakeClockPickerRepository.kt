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
import com.android.systemui.plugins.clocks.AxisType
import com.android.systemui.plugins.clocks.ClockFontAxis
import com.android.systemui.plugins.clocks.ClockFontAxisSetting
import com.android.systemui.plugins.clocks.ClockId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** By default [FakeClockPickerRepository] uses [fakeClocks]. */
open class FakeClockPickerRepository(clocks: List<ClockMetadataModel> = fakeClocks) :
    ClockPickerRepository {
    override val allClocks: Flow<List<ClockMetadataModel>> = MutableStateFlow(clocks).asStateFlow()

    private val selectedClockId: MutableStateFlow<String> = MutableStateFlow(fakeClocks[0].clockId)
    @ColorInt private val selectedColorId: MutableStateFlow<String?> = MutableStateFlow(null)
    private val colorTone: MutableStateFlow<Int> =
        MutableStateFlow(ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS)
    @ColorInt private val seedColor: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val fontAxes: MutableStateFlow<List<ClockFontAxis>> =
        MutableStateFlow(listOf(buildFakeAxis(0)))
    override val selectedClock: Flow<ClockMetadataModel> =
        combine(selectedClockId, selectedColorId, colorTone, seedColor, fontAxes) {
            selectedClockId,
            selectedColor,
            colorTone,
            seedColor,
            fontAxes ->
            val selectedClock = fakeClocks.find { clock -> clock.clockId == selectedClockId }
            checkNotNull(selectedClock)
            ClockMetadataModel(
                clockId = selectedClock.clockId,
                isSelected = true,
                description = "description",
                thumbnail = ColorDrawable(0),
                isReactiveToTone = selectedClock.isReactiveToTone,
                fontAxes = fontAxes,
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

    override suspend fun setClockFontAxes(axisSettings: List<ClockFontAxisSetting>) {
        fontAxes.update { fontAxes -> ClockFontAxis.merge(fontAxes, axisSettings) }
    }

    override fun isReactiveToTone(clockId: ClockId): Boolean? = true

    companion object {
        fun buildFakeAxis(i: Int): ClockFontAxis {
            return ClockFontAxis(
                key = "key",
                type = AxisType.Float,
                maxValue = 0f,
                minValue = 1000f,
                currentValue = 50f * (i + 1),
                name = "FakeAxis",
                description = "Axis Description",
            )
        }

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
                    listOf(buildFakeAxis(0)),
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
                    listOf(buildFakeAxis(1)),
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
                    listOf(buildFakeAxis(2)),
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
                    listOf(buildFakeAxis(3)),
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
