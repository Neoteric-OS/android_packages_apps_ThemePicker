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

package com.android.customization.picker.grid.domain.interactor

import androidx.test.filters.SmallTest
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.customization.picker.grid.data.repository.ShapeGridRepository
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
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
class ShapeGridInteractorTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var gridOptionsManager: FakeShapeGridManager
    @Inject lateinit var repository: ShapeGridRepository
    @Inject lateinit var testScope: TestScope

    private lateinit var underTest: ShapeGridInteractor

    @Before
    fun setUp() {
        hiltRule.inject()
        underTest = ShapeGridInteractor(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shapeOptions_default() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)

            assertThat(shapeOptions()).isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST)
        }

    @Test
    fun shapeOptions_shouldUpdateAfterApplyGridOption() =
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
            val selectedShapeOption = collectLastValue(underTest.selectedShapeOption)

            assertThat(selectedShapeOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[0])
        }

    @Test
    fun selectedShapeOption_shouldUpdateAfterApplyGridOption() =
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
    fun gridOptions_shouldUpdateAfterApplyGridOption() =
        testScope.runTest {
            val gridOptions = collectLastValue(underTest.gridOptions)

            underTest.applySelectedOption("arch", "practical")

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
    fun selectedGridOption_shouldUpdateAfterApplyGridOption() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedGridOption)

            underTest.applySelectedOption("arch", "practical")

            assertThat(selectedGridOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_GRID_OPTION_LIST[1].copy(isCurrent = true))
        }
}
