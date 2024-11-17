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

import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.mode.data.repository.DarkModeRepository
import com.android.customization.picker.mode.domain.interactor.DarkModeInteractor
import com.android.wallpaper.testing.FakePowerManager
import com.android.wallpaper.testing.FakeUiModeManager
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
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
    @Inject lateinit var darkModeRepository: DarkModeRepository
    @Inject lateinit var darkModeInteractor: DarkModeInteractor
    @Inject lateinit var logger: TestThemesUserEventLogger
    lateinit var darkModeViewModel: DarkModeViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        darkModeViewModel = DarkModeViewModel(darkModeInteractor, logger)
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
            darkModeRepository.refreshIsDarkModeActivated()
            val getOverridingIsDarkMode = collectLastValue(darkModeViewModel.overridingIsDarkMode)
            val getPreviewingIsDarkMode = collectLastValue(darkModeViewModel.previewingIsDarkMode)
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)
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
    fun onApply_shouldLogDarkTheme() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkModeActivated()
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)
            val onApply = collectLastValue(darkModeViewModel.onApply)

            getToggleDarkMode()?.invoke()
            onApply()?.invoke()

            assertThat(logger.useDarkTheme).isTrue()
        }
    }

    @Test
    fun onApply_shouldApplyDarkTheme() {
        testScope.runTest {
            uiModeManager.setNightModeActivated(false)
            darkModeRepository.refreshIsDarkModeActivated()
            val getToggleDarkMode = collectLastValue(darkModeViewModel.toggleDarkMode)
            val onApply = collectLastValue(darkModeViewModel.onApply)

            getToggleDarkMode()?.invoke()
            onApply()?.invoke()

            assertThat(uiModeManager.getIsNightModeActivated()).isTrue()
        }
    }
}
