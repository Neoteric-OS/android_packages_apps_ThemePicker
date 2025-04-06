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

package com.android.customization.picker.mode.ui.viewmodel

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.mode.data.repository.DarkModeRepository
import com.android.customization.picker.mode.data.repository.DarkModeStateRepository
import com.android.customization.picker.mode.domain.interactor.DarkModeInteractor
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.testing.FakePowerManager
import com.android.wallpaper.testing.FakeUiModeManager
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.internal.lifecycle.RetainedLifecycleImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DarkModeViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var uiModeManager: FakeUiModeManager
    @Inject lateinit var powerManager: FakePowerManager
    @Inject lateinit var darkModeStateRepository: DarkModeStateRepository
    @Inject lateinit var darkModeRepository: DarkModeRepository
    @Inject lateinit var darkModeInteractor: DarkModeInteractor
    @Inject lateinit var logger: TestThemesUserEventLogger
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    private lateinit var context: Context
    private lateinit var colorUpdateViewModel: ColorUpdateViewModel
    private lateinit var darkModeViewModel: DarkModeViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        context = InstrumentationRegistry.getInstrumentation().targetContext
        colorUpdateViewModel =
            ColorUpdateViewModel(context, RetainedLifecycleImpl(), darkModeStateRepository)
        darkModeViewModel = DarkModeViewModel(colorUpdateViewModel, darkModeInteractor, logger)
    }

    @Test
    fun isEnabled_powerSaveModeOn() {
        testScope.runTest {
            powerManager.setIsPowerSaveMode(true)
            darkModeRepository.refreshIsPowerSaveModeActivated()

            val isEnabled = collectLastValue(darkModeViewModel.isEnabled)()

            assertThat(isEnabled).isFalse()
        }
    }

    @Test
    fun isEnabled_powerSaveModeOff() {
        testScope.runTest {
            powerManager.setIsPowerSaveMode(false)
            darkModeRepository.refreshIsPowerSaveModeActivated()

            val isEnabled = collectLastValue(darkModeViewModel.isEnabled)()

            assertThat(isEnabled).isTrue()
        }
    }

    @Test
    fun toggleDarkMode() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkMode()
            val getOverridingIsDarkMode = collectLastValue(darkModeViewModel.overridingIsDarkMode)
            val getPreviewingIsDarkMode = collectLastValue(darkModeViewModel.previewingIsDarkMode)
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)
            assertThat(getOverridingIsDarkMode()).isNull()
            assertThat(getPreviewingIsDarkMode()).isFalse()

            getToggleDarkMode()?.invoke()

            assertThat(getOverridingIsDarkMode()).isTrue()
            assertThat(getPreviewingIsDarkMode()).isTrue()

            getToggleDarkMode()?.invoke()

            assertThat(getOverridingIsDarkMode()).isNull()
            assertThat(getPreviewingIsDarkMode()).isFalse()
        }
    }

    @Test
    fun previewingIsDarkMode_systemChanges() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkMode()
            val getOverridingIsDarkMode = collectLastValue(darkModeViewModel.overridingIsDarkMode)
            val getPreviewingIsDarkMode = collectLastValue(darkModeViewModel.previewingIsDarkMode)
            assertThat(getOverridingIsDarkMode()).isNull()
            assertThat(getPreviewingIsDarkMode()).isFalse()

            // Turn on dark mode
            uiModeManager.setNightModeActivated(true)
            darkModeRepository.refreshIsDarkMode()

            assertThat(getOverridingIsDarkMode()).isNull()
            assertThat(getPreviewingIsDarkMode()).isTrue()

            // Turn off dark mode
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkMode()

            assertThat(getOverridingIsDarkMode()).isNull()
            assertThat(getPreviewingIsDarkMode()).isFalse()
        }
    }

    @Test
    fun onApply_shouldLogDarkTheme() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkMode()
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)

            getToggleDarkMode()?.invoke()
            applyDarkMode()

            assertThat(logger.useDarkTheme).isTrue()
        }
    }

    @Test
    fun onApply_shouldApplyDarkTheme() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkMode()
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)

            getToggleDarkMode()?.invoke()
            applyDarkMode()

            assertThat(uiModeManager.getIsNightModeActivated()).isTrue()
        }
    }

    /** Simulates a user applying the previewing dark mode, and the apply completes. */
    private fun TestScope.applyDarkMode() {
        val onApply = collectLastValue(darkModeViewModel.onApply)()
        testScope.launch { onApply?.invoke() }
        // Run coroutine launched in DarkModeViewModel#onApply
        runCurrent()
        // Simulate dark mode and color update config change
        colorUpdateViewModel.updateDarkModeAndColors()
        // Run coroutine launched in colorUpdateViewModel#updateColors
        runCurrent()
    }
}
