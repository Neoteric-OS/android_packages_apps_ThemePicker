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
package com.android.customization.model.picker.color.domain.interactor

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository2
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor2
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.testing.FakeSnapshotStore
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorPickerInteractor2Test {
    private lateinit var underTest: ColorPickerInteractor2
    private lateinit var repository: FakeColorPickerRepository2
    private lateinit var store: FakeSnapshotStore

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = FakeColorPickerRepository2()
        store = FakeSnapshotStore()
        underTest = ColorPickerInteractor2(repository = repository)
        repository.setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
    }

    @Test
    fun select() = runTest {
        val colorOptions = collectLastValue(underTest.colorOptions)
        val selectedColorOption = collectLastValue(underTest.selectedColorOption)

        val wallpaperColorOption = colorOptions()?.get(ColorType.WALLPAPER_COLOR)?.get(2)
        assertThat(selectedColorOption()).isNotEqualTo(wallpaperColorOption)

        wallpaperColorOption?.let { underTest.select(colorOption = it) }
        assertThat(selectedColorOption()).isEqualTo(wallpaperColorOption)

        val presetColorOption = colorOptions()?.get(ColorType.PRESET_COLOR)?.get(1)
        assertThat(selectedColorOption()).isNotEqualTo(presetColorOption)

        presetColorOption?.let { underTest.select(colorOption = it) }
        assertThat(selectedColorOption()).isEqualTo(presetColorOption)
    }
}
