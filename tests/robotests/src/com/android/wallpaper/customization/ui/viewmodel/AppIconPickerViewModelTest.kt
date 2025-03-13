/*
 * Copyright (C) 2025 The Android Open Source Project
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
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.customization.picker.grid.domain.interactor.AppIconInteractor
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
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
class AppIconPickerViewModelTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var gridOptionsManager: FakeShapeGridManager
    @Inject lateinit var interactor: AppIconInteractor
    @Inject @ApplicationContext lateinit var appContext: Context

    private lateinit var underTest: AppIconPickerViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        underTest = AppIconPickerViewModel(interactor, testScope.backgroundScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectedShapeKey() =
        testScope.runTest {
            val selectedShapeKey = collectLastValue(underTest.selectedShapeKey)

            assertThat(selectedShapeKey()).isEqualTo("arch")
        }

    @Test
    fun shapeOptions() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)

            for (i in 0 until FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST.size) {
                val (expectedKey, expectedPath, expectedTitle) =
                    with(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i]) {
                        arrayOf(key, path, title)
                    }
                assertShapeItem(
                    optionItem = shapeOptions()?.get(i),
                    key = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].key,
                    payload = ShapeIconViewModel(expectedKey, expectedPath),
                    text = Text.Loaded(expectedTitle),
                    isTextUserVisible = true,
                    isSelected = expectedKey == "arch",
                    isEnabled = true,
                )
            }
        }

    @Test
    fun shapeOptions_whenClickOnCircleOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val previewingShapeKey = collectLastValue(underTest.previewingShapeKey)
            val circleOption = shapeOptions()?.firstOrNull { it.key.value == "circle" }
            val onCircleOptionClicked = circleOption?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onCircleOptionClicked)

            onCircleOptionClicked()?.invoke()

            assertThat(previewingShapeKey()).isEqualTo("circle")
            for (i in 0 until FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST.size) {
                val expectedKey = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].key
                val expectedPath = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].path
                val expectedTitle = FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[i].title
                assertShapeItem(
                    optionItem = shapeOptions()?.get(i),
                    key = expectedKey,
                    payload = ShapeIconViewModel(expectedKey, expectedPath),
                    text = Text.Loaded(expectedTitle),
                    isTextUserVisible = true,
                    isSelected = expectedKey == "circle",
                    isEnabled = true,
                )
            }
        }

    @Test
    fun onApple_shouldBeNonnull_whenClickOnCircleOption() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)
            val circleOption = shapeOptions()?.firstOrNull { it.key.value == "circle" }
            val onCircleOptionClicked = circleOption?.onClicked?.let { collectLastValue(it) }
            val onApply = collectLastValue(underTest.onApply)
            checkNotNull(onCircleOptionClicked)

            assertThat(onApply()).isNull()

            onCircleOptionClicked()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    @Test
    fun isThemeIconEnabled_shouldBeFalseByDefault() =
        testScope.runTest {
            val isThemeIconEnabled = collectLastValue(underTest.isThemedIconEnabled)

            assertThat(isThemeIconEnabled()).isFalse()
        }

    @Test
    fun previewingIsThemeIconEnabled_shouldBeFalseByDefault() =
        testScope.runTest {
            val previewingIsThemeIconEnabled =
                collectLastValue(underTest.previewingIsThemeIconEnabled)

            assertThat(previewingIsThemeIconEnabled()).isFalse()
        }

    @Test
    fun previewingIsThemeIconEnabled_shouldBeTrue_whenToggle() =
        testScope.runTest {
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            val previewingIsThemeIconEnabled =
                collectLastValue(underTest.previewingIsThemeIconEnabled)

            assertThat(previewingIsThemeIconEnabled()).isFalse()

            toggleThemedIcon()?.invoke()

            assertThat(previewingIsThemeIconEnabled()).isTrue()
        }

    @Test
    fun onApple_shouldBeNonnull_whenToggle() =
        testScope.runTest {
            val toggleThemedIcon = collectLastValue(underTest.toggleThemedIcon)
            val onApply = collectLastValue(underTest.onApply)

            assertThat(onApply()).isNull()

            toggleThemedIcon()?.invoke()

            assertThat(onApply()).isNotNull()
        }

    private fun TestScope.assertShapeItem(
        optionItem: OptionItemViewModel2<ShapeIconViewModel>?,
        key: String,
        payload: ShapeIconViewModel?,
        text: Text,
        isTextUserVisible: Boolean,
        isSelected: Boolean,
        isEnabled: Boolean,
    ) {
        checkNotNull(optionItem)
        assertThat(collectLastValue(optionItem.key)()).isEqualTo(key)
        assertThat(optionItem.text).isEqualTo(text)
        assertThat(optionItem.payload).isEqualTo(payload)
        assertThat(optionItem.isTextUserVisible).isEqualTo(isTextUserVisible)
        assertThat(collectLastValue(optionItem.isSelected)()).isEqualTo(isSelected)
        assertThat(optionItem.isEnabled).isEqualTo(isEnabled)
    }
}
