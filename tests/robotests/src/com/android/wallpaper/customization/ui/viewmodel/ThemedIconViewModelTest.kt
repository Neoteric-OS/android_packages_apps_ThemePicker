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

import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.themedicon.domain.interactor.ThemedIconInteractor
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
class ThemedIconViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var logger: TestThemesUserEventLogger
    @Inject lateinit var themedIconInteractor: ThemedIconInteractor
    private lateinit var themedIconViewModel: ThemedIconViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        themedIconViewModel = ThemedIconViewModel(themedIconInteractor, logger)
    }

    @Test
    fun toggleThemedIcon() {
        testScope.runTest {
            val isActivated = collectLastValue(themedIconViewModel.isActivated)
            val toggleThemedIcon = collectLastValue(themedIconViewModel.toggleThemedIcon)
            assertThat(isActivated()).isFalse()

            toggleThemedIcon()?.invoke()

            assertThat(isActivated()).isTrue()

            toggleThemedIcon()?.invoke()

            assertThat(isActivated()).isFalse()
        }
    }

    @Test
    fun toggleThemedIcon_shouldLogThemedIcon() {
        testScope.runTest {
            val toggleThemedIcon = collectLastValue(themedIconViewModel.toggleThemedIcon)
            assertThat(logger.useThemedIcon).isFalse()

            toggleThemedIcon()?.invoke()

            assertThat(logger.useThemedIcon).isTrue()
        }
    }
}
