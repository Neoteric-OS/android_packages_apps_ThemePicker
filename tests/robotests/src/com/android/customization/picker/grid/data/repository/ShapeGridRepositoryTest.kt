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

package com.android.customization.picker.grid.data.repository

import androidx.test.filters.SmallTest
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
class ShapeGridRepositoryTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var gridOptionsManager: FakeShapeGridManager
    @Inject lateinit var testScope: TestScope
    @BackgroundDispatcher @Inject lateinit var bgScope: CoroutineScope
    @BackgroundDispatcher @Inject lateinit var bgDispatcher: CoroutineDispatcher

    private lateinit var underTest: ShapeGridRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        underTest =
            ShapeGridRepository(
                manager = gridOptionsManager,
                bgScope = bgScope,
                bgDispatcher = bgDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shapeOptions_default() =
        testScope.runTest {
            val gridOptions = collectLastValue(underTest.shapeOptions)

            assertThat(gridOptions()).isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST)
        }

    @Test
    fun shapeOptions_shouldUpdateAfterApplyShapeGridOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)

            underTest.applySelectedOption("circle", "practical")

            assertThat(shapeOptions())
                .isEqualTo(
                    FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST.map {
                        it.copy(isCurrent = (it.key == "circle"))
                    }
                )
        }

    @Test
    fun selectedShapeOption_default() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedShapeOption)

            assertThat(selectedGridOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[0])
        }

    @Test
    fun selectedShapeOption_shouldUpdateAfterApplyShapeGridOption() =
        testScope.runTest {
            val selectedShapeOption = collectLastValue(underTest.selectedShapeOption)

            underTest.applySelectedOption("circle", "practical")

            assertThat(selectedShapeOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[4].copy(isCurrent = true))
        }

    @Test
    fun gridOptions_default() =
        testScope.runTest {
            val gridOptions = collectLastValue(underTest.gridOptions)

            assertThat(gridOptions()).isEqualTo(FakeShapeGridManager.DEFAULT_GRID_OPTION_LIST)
        }

    @Test
    fun gridOptions_shouldUpdateAfterApplyShapeGridOption() =
        testScope.runTest {
            val gridOptions = collectLastValue(underTest.gridOptions)

            underTest.applySelectedOption("circle", "practical")

            assertThat(gridOptions())
                .isEqualTo(
                    FakeShapeGridManager.DEFAULT_GRID_OPTION_LIST.map {
                        it.copy(isCurrent = (it.key == "practical"))
                    }
                )
        }

    @Test
    fun selectedGridOption_default() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedGridOption)

            assertThat(selectedGridOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_GRID_OPTION_LIST[0])
        }

    @Test
    fun selectedGridOption_shouldUpdateAfterApplyShapeGridOption() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedGridOption)

            underTest.applySelectedOption("circle", "practical")

            assertThat(selectedGridOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_GRID_OPTION_LIST[1].copy(isCurrent = true))
        }
}
