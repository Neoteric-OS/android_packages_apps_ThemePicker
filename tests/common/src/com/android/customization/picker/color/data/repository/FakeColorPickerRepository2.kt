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
package com.android.customization.picker.color.data.repository

import android.graphics.Color
import com.android.customization.model.ResourceConstants
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.model.color.ColorOptionsProvider
import com.android.customization.model.color.ColorUtils.toColorString
import com.android.customization.picker.color.shared.model.ColorType
import com.android.systemui.monet.Style
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeColorPickerRepository2 @Inject constructor() : ColorPickerRepository2 {

    private val _selectedColorOption = MutableStateFlow<ColorOption?>(null)
    override val selectedColorOption = _selectedColorOption.asStateFlow()

    private val _colorOptions =
        MutableStateFlow(
            mapOf<ColorType, List<ColorOption>>(
                ColorType.WALLPAPER_COLOR to listOf(),
                ColorType.PRESET_COLOR to listOf(),
            )
        )
    override val colorOptions: StateFlow<Map<ColorType, List<ColorOption>>> =
        _colorOptions.asStateFlow()

    init {
        setOptions(4, 4, ColorType.WALLPAPER_COLOR, 0)
    }

    fun setOptions(
        wallpaperOptions: List<ColorOptionImpl>,
        presetOptions: List<ColorOptionImpl>,
        selectedColorOptionType: ColorType,
        selectedColorOptionIndex: Int,
    ) {
        _colorOptions.value =
            mapOf(
                ColorType.WALLPAPER_COLOR to
                    buildList {
                        for ((index, colorOption) in wallpaperOptions.withIndex()) {
                            val isSelected =
                                selectedColorOptionType == ColorType.WALLPAPER_COLOR &&
                                    selectedColorOptionIndex == index
                            if (isSelected) {
                                _selectedColorOption.value = colorOption
                            }
                            add(colorOption)
                        }
                    },
                ColorType.PRESET_COLOR to
                    buildList {
                        for ((index, colorOption) in presetOptions.withIndex()) {
                            val isSelected =
                                selectedColorOptionType == ColorType.PRESET_COLOR &&
                                    selectedColorOptionIndex == index
                            if (isSelected) {
                                _selectedColorOption.value = colorOption
                            }
                            add(colorOption)
                        }
                    },
            )
    }

    fun setOptions(
        numWallpaperOptions: Int,
        numPresetOptions: Int,
        selectedColorOptionType: ColorType,
        selectedColorOptionIndex: Int,
    ) {
        _colorOptions.value =
            mapOf(
                ColorType.WALLPAPER_COLOR to
                    buildList {
                        repeat(times = numWallpaperOptions) { index ->
                            val isSelected =
                                selectedColorOptionType == ColorType.WALLPAPER_COLOR &&
                                    selectedColorOptionIndex == index
                            val colorOption = buildWallpaperOption(index)
                            if (isSelected) {
                                _selectedColorOption.value = colorOption
                            }
                            add(colorOption)
                        }
                    },
                ColorType.PRESET_COLOR to
                    buildList {
                        repeat(times = numPresetOptions) { index ->
                            val isSelected =
                                selectedColorOptionType == ColorType.PRESET_COLOR &&
                                    selectedColorOptionIndex == index
                            val colorOption = buildPresetOption(index)
                            if (isSelected) {
                                _selectedColorOption.value = colorOption
                            }
                            add(colorOption)
                        }
                    },
            )
    }

    private fun buildPresetOption(index: Int): ColorOptionImpl {
        val builder = ColorOptionImpl.Builder()
        builder.lightColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.darkColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.index = index
        builder.type = ColorType.PRESET_COLOR
        builder.source = ColorOptionsProvider.COLOR_SOURCE_PRESET
        builder.title = "Preset"
        builder
            .addOverlayPackage("TEST_PACKAGE_TYPE", "preset_color")
            .addOverlayPackage("TEST_PACKAGE_INDEX", "$index")
        return builder.build()
    }

    fun buildPresetOption(@Style.Type style: Int, seedColor: Int): ColorOptionImpl {
        val builder = ColorOptionImpl.Builder()
        builder.lightColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.darkColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.type = ColorType.PRESET_COLOR
        builder.source = ColorOptionsProvider.COLOR_SOURCE_PRESET
        builder.style = style
        builder.title = "Preset"
        builder.seedColor = seedColor
        builder
            .addOverlayPackage("TEST_PACKAGE_TYPE", "preset_color")
            .addOverlayPackage(
                ResourceConstants.OVERLAY_CATEGORY_SYSTEM_PALETTE,
                toColorString(seedColor),
            )
        return builder.build()
    }

    private fun buildWallpaperOption(index: Int): ColorOptionImpl {
        val builder = ColorOptionImpl.Builder()
        builder.lightColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.darkColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.index = index
        builder.type = ColorType.WALLPAPER_COLOR
        builder.source = ColorOptionsProvider.COLOR_SOURCE_HOME
        builder.title = "Dynamic"
        builder
            .addOverlayPackage("TEST_PACKAGE_TYPE", "wallpaper_color")
            .addOverlayPackage("TEST_PACKAGE_INDEX", "$index")
        return builder.build()
    }

    fun buildWallpaperOption(
        source: String,
        @Style.Type style: Int,
        seedColor: Int,
    ): ColorOptionImpl {
        val builder = ColorOptionImpl.Builder()
        builder.lightColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.darkColors =
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        builder.type = ColorType.WALLPAPER_COLOR
        builder.source = source
        builder.style = style
        builder.title = "Dynamic"
        builder.seedColor = seedColor
        builder
            .addOverlayPackage("TEST_PACKAGE_TYPE", "wallpaper_color")
            .addOverlayPackage(
                ResourceConstants.OVERLAY_CATEGORY_SYSTEM_PALETTE,
                toColorString(seedColor),
            )
        return builder.build()
    }

    override suspend fun select(colorOption: ColorOption) {
        _selectedColorOption.value = colorOption
    }
}
