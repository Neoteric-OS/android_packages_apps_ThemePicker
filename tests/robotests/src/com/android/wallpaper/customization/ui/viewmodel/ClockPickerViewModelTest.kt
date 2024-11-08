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

package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import androidx.test.filters.SmallTest
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.clock.data.repository.FakeClockPickerRepository
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.domain.interactor.ClockPickerSnapshotRestorer
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.domain.interactor.ColorPickerSnapshotRestorer
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.Tab
import com.android.wallpaper.testing.FakeSnapshotStore
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ClockPickerViewModelTest {

    private val logger = TestThemesUserEventLogger()

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    private lateinit var colorMap: Map<String, ClockColorViewModel>
    private lateinit var underTest: ClockPickerViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        val repository = FakeClockPickerRepository()
        val clockPickerInteractor =
            ClockPickerInteractor(
                repository = repository,
                snapshotRestorer =
                    ClockPickerSnapshotRestorer(repository = repository).apply {
                        runBlocking { setUpSnapshotRestorer(store = FakeSnapshotStore()) }
                    },
            )
        val colorPickerRepository = FakeColorPickerRepository(context = context)
        val colorPickerInteractor =
            ColorPickerInteractor(
                repository = colorPickerRepository,
                snapshotRestorer =
                    ColorPickerSnapshotRestorer(repository = colorPickerRepository).apply {
                        runBlocking { setUpSnapshotRestorer(store = FakeSnapshotStore()) }
                    },
            )
        colorMap = ClockColorViewModel.getPresetColorMap(context.resources)
        underTest =
            ClockPickerViewModel(
                context = context,
                resources = context.resources,
                clockPickerInteractor = clockPickerInteractor,
                colorPickerInteractor = colorPickerInteractor,
                logger = logger,
                backgroundDispatcher = testDispatcher,
                viewModelScope = testScope,
            )

        testScope.launch {
            clockPickerInteractor.setSelectedClock(FakeClockPickerRepository.CLOCK_ID_0)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectedTab_whenClickOnTabs() = runTest {
        val tabs = collectLastValue(underTest.tabs)
        val selectedTab = collectLastValue(underTest.selectedTab)

        assertThat(selectedTab()).isEqualTo(Tab.STYLE)

        tabs()?.get(1)?.onClick?.invoke()

        assertThat(selectedTab()).isEqualTo(Tab.COLOR)
    }

    @Test
    fun tabs_whenClickOnTabs() = runTest {
        val tabs = collectLastValue(underTest.tabs)

        assertThat(tabs()?.get(0)?.isSelected).isTrue()

        tabs()?.get(1)?.onClick?.invoke()

        assertThat(tabs()?.get(1)?.isSelected).isTrue()
    }

    @Test
    fun selectedTab_fontEditorWhenClickSelectedClock() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        val selectedTab = collectLastValue(underTest.selectedTab)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        assertThat(selectedTab()).isEqualTo(Tab.STYLE)

        val firstClock = clockStyleOptions()!![0]
        val onClicked = collectLastValue(firstClock.onClicked)
        if (!firstClock.isSelected.value) onClicked()?.invoke()
        onClicked()?.invoke()

        assertThat(selectedTab()).isEqualTo(Tab.FONT)
    }

    @Test
    fun previewingClock_whenClickOnStyleOptions() = runTest {
        val previewingClock = collectLastValue(underTest.previewingClock)
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(previewingClock()?.clockId).isEqualTo(FakeClockPickerRepository.CLOCK_ID_0)

        val option1OnClicked = collectLastValue(clockStyleOptions()!![1].onClicked)
        option1OnClicked()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockColorOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(previewingClock()?.clockId).isEqualTo(FakeClockPickerRepository.CLOCK_ID_1)
    }

    @Test
    fun clockStyleOptions_whenClickOnStyleOptions() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val option0IsSelected = collectLastValue(clockStyleOptions()!![0].isSelected)
        val option0OnClicked = collectLastValue(clockStyleOptions()!![0].onClicked)
        val option1IsSelected = collectLastValue(clockStyleOptions()!![1].isSelected)
        val option1OnClicked = collectLastValue(clockStyleOptions()!![1].onClicked)

        assertThat(option0IsSelected()).isTrue()
        assertThat(option0OnClicked()).isNotNull()

        option1OnClicked()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockColorOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(option0IsSelected()).isFalse()
        assertThat(option1IsSelected()).isTrue()
        assertThat(option1OnClicked()).isNotNull()
    }

    @Test
    fun previewingClockSize_whenCallingOnClockSizeSwitchChecked() = runTest {
        val previewingClockSize = collectLastValue(underTest.previewingClockSize)

        assertThat(previewingClockSize()).isEqualTo(ClockSize.DYNAMIC)

        val onClockSizeSwitchCheckedChange =
            collectLastValue(underTest.onClockSizeSwitchCheckedChange)
        onClockSizeSwitchCheckedChange()?.invoke()

        assertThat(previewingClockSize()).isEqualTo(ClockSize.SMALL)
    }

    @Test
    fun previewingFontAxes_defaultWhenNoOverrides() = runTest {
        val previewingFontAxes = collectLastValue(underTest.previewingFontAxes)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 50f))
    }

    @Test
    fun previewingFontAxes_updateAxisChangesSetting() = runTest {
        val previewingFontAxes = collectLastValue(underTest.previewingFontAxes)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 50f))

        underTest.updatePreviewFontAxis("key", 100f)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 100f))

        underTest.updatePreviewFontAxis("extra", 10f)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 100f, "extra" to 10f))
    }

    @Test
    fun previewingFontAxes_applyFontEditorExitsTab_keepsPreviewAxis() = runTest {
        val previewingFontAxes = collectLastValue(underTest.previewingFontAxes)
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        val selectedTab = collectLastValue(underTest.selectedTab)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 50f))
        assertThat(selectedTab()).isEqualTo(Tab.STYLE)

        val firstClock = clockStyleOptions()!![0]
        val onClicked = collectLastValue(firstClock.onClicked)
        if (!firstClock.isSelected.value) onClicked()?.invoke()
        onClicked()?.invoke()
        underTest.updatePreviewFontAxis("key", 100f)

        assertThat(selectedTab()).isEqualTo(Tab.FONT)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 100f))

        underTest.applyFontAxes()

        assertThat(selectedTab()).isEqualTo(Tab.STYLE)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 100f))
    }

    @Test
    fun previewingFontAxes_revertFontEditorExitsTab_revertsPreviewAxis() = runTest {
        val previewingFontAxes = collectLastValue(underTest.previewingFontAxes)
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        val selectedTab = collectLastValue(underTest.selectedTab)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 50f))
        assertThat(selectedTab()).isEqualTo(Tab.STYLE)

        val firstClock = clockStyleOptions()!![0]
        val onClicked = collectLastValue(firstClock.onClicked)
        if (!firstClock.isSelected.value) onClicked()?.invoke()
        onClicked()?.invoke()
        underTest.updatePreviewFontAxis("key", 100f)

        assertThat(selectedTab()).isEqualTo(Tab.FONT)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 100f))

        underTest.revertFontAxes()

        assertThat(selectedTab()).isEqualTo(Tab.STYLE)
        assertThat(previewingFontAxes()).isEqualTo(mapOf("key" to 50f))
    }

    @Test
    fun sliderProgress_whenOnSliderProgressChanged() = runTest {
        val sliderProgress = collectLastValue(underTest.previewingSliderProgress)

        assertThat(sliderProgress()).isEqualTo(ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS)

        underTest.onSliderProgressChanged(87)

        assertThat(sliderProgress()).isEqualTo(87)
    }

    @Test
    fun isSliderEnabledShouldBeTrue_whenTheClockIsReactiveToToneAndSolidColor() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val styleOption0OnClicked = collectLastValue(clockStyleOptions()!![0].onClicked)
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val colorOption1OnClicked = collectLastValue(clockColorOptions()!![1].onClicked)
        val isSliderEnabled = collectLastValue(underTest.isSliderEnabled)

        styleOption0OnClicked()?.invoke()
        colorOption1OnClicked()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(isSliderEnabled()).isTrue()
    }

    @Test
    fun isSliderEnabledShouldBeFalse_whenTheClockIsReactiveToToneAndDefaultColor() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val styleOption0OnClicked = collectLastValue(clockStyleOptions()!![0].onClicked)
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val colorOption0OnClicked = collectLastValue(clockColorOptions()!![0].onClicked)
        val isSliderEnabled = collectLastValue(underTest.isSliderEnabled)

        styleOption0OnClicked()?.invoke()
        colorOption0OnClicked()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(isSliderEnabled()).isFalse()
    }

    @Test
    fun isSliderEnabledShouldBeFalse_whenTheClockIsNotReactiveToTone() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val styleOption3OnClicked = collectLastValue(clockStyleOptions()!![3].onClicked)
        val isSliderEnabled = collectLastValue(underTest.isSliderEnabled)

        styleOption3OnClicked()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(isSliderEnabled()).isFalse()
    }

    @Test
    fun previewingSeedColor_whenChangeColorOptionAndToneProgress() = runTest {
        val previewingSeedColor = collectLastValue(underTest.previewingSeedColor)
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option1OnClicked = collectLastValue(clockColorOptions()!![1].onClicked)

        option1OnClicked()?.invoke()
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val targetProgress = 55
        underTest.onSliderProgressChanged(targetProgress)

        val expectedSelectedColorModel = colorMap.values.first() // RED
        assertThat(previewingSeedColor())
            .isEqualTo(
                ClockSettingsViewModel.blendColorWithTone(
                    expectedSelectedColorModel.color,
                    expectedSelectedColorModel.getColorTone(targetProgress),
                )
            )
    }

    @Test
    fun clockColorOptions_whenClickOnColorOptions() = runTest {
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option0IsSelected = collectLastValue(clockColorOptions()!![0].isSelected)
        val option0OnClicked = collectLastValue(clockColorOptions()!![0].onClicked)
        val option1IsSelected = collectLastValue(clockColorOptions()!![1].isSelected)
        val option1OnClicked = collectLastValue(clockColorOptions()!![1].onClicked)

        assertThat(option0IsSelected()).isTrue()
        assertThat(option0OnClicked()).isNull()

        option1OnClicked()?.invoke()
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)

        assertThat(option0IsSelected()).isFalse()
        assertThat(option1IsSelected()).isTrue()
        assertThat(option1OnClicked()).isNull()
    }
}
