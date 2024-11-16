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
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.systemui.plugins.clocks.ClockFontAxisSetting
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlin.collections.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** View model for the clock customization screen. */
class ClockPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    resources: Resources,
    private val clockPickerInteractor: ClockPickerInteractor,
    colorPickerInteractor: ColorPickerInteractor,
    private val logger: ThemesUserEventLogger,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    enum class Tab {
        STYLE,
        COLOR,
        FONT,
    }

    private val colorMap = ClockColorViewModel.getPresetColorMap(context.resources)

    // Tabs
    private val _selectedTab = MutableStateFlow(Tab.STYLE)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        _selectedTab.asStateFlow().map {
            listOf(
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_style_filled_24px,
                        contentDescription = Text.Resource(R.string.clock_style),
                    ),
                    context.getString(R.string.clock_style),
                    it == Tab.STYLE || it == Tab.FONT,
                ) {
                    _selectedTab.value = Tab.STYLE
                },
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_palette_filled_24px,
                        contentDescription = Text.Resource(R.string.clock_color),
                    ),
                    context.getString(R.string.clock_color),
                    it == Tab.COLOR,
                ) {
                    _selectedTab.value = Tab.COLOR
                },
            )
        }

    // Clock style
    private val overridingClock = MutableStateFlow<ClockMetadataModel?>(null)
    private val isClockEdited =
        combine(overridingClock, clockPickerInteractor.selectedClock) {
            overridingClock,
            selectedClock ->
            overridingClock != null && overridingClock.clockId != selectedClock.clockId
        }
    val previewingClock =
        combine(overridingClock, clockPickerInteractor.selectedClock) {
            overridingClock,
            selectedClock ->
            overridingClock ?: selectedClock
        }

    data class ClockStyleModel(val thumbnail: Drawable, val isEditable: Boolean)

    @OptIn(ExperimentalCoroutinesApi::class)
    val clockStyleOptions: StateFlow<List<OptionItemViewModel<ClockStyleModel>>> =
        clockPickerInteractor.allClocks
            .mapLatest { allClocks ->
                // Delay to avoid the case that the full list of clocks is not initiated.
                delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
                allClocks.map { clockModel ->
                    val isSelectedFlow =
                        previewingClock
                            .map { it.clockId == clockModel.clockId }
                            .stateIn(viewModelScope)
                    val contentDescription =
                        resources.getString(
                            R.string.select_clock_action_description,
                            clockModel.description,
                        )
                    OptionItemViewModel<ClockStyleModel>(
                        key = MutableStateFlow(clockModel.clockId) as StateFlow<String>,
                        payload =
                            ClockStyleModel(
                                clockModel.thumbnail,
                                isEditable = !clockModel.fontAxes.isEmpty(),
                            ),
                        text = Text.Loaded(contentDescription),
                        isTextUserVisible = false,
                        isSelected = isSelectedFlow,
                        onClicked =
                            isSelectedFlow.map { isSelected ->
                                if (isSelected) {
                                    fun() {
                                        _selectedTab.value = Tab.FONT
                                    }
                                } else {
                                    fun() {
                                        overridingClock.value = clockModel
                                        overrideFontAxisMap.value = null
                                    }
                                }
                            },
                    )
                }
            }
            // makes sure that the operations above this statement are executed on I/O dispatcher
            // while parallelism limits the number of threads this can run on which makes sure that
            // the flows run sequentially
            .flowOn(backgroundDispatcher.limitedParallelism(1))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Clock Font Axis Editor
    private val overrideFontAxisMap = MutableStateFlow<Map<String, Float>?>(null)
    val previewingFontAxisMap =
        combine(overrideFontAxisMap, previewingClock) { overrideAxes, previewingClock ->
                overrideAxes ?: previewingClock.fontAxes.associate { it.key to it.currentValue }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val isFontAxisMapEdited = overrideFontAxisMap.map { it != null }

    fun updatePreviewFontAxis(key: String, value: Float) {
        val axisMap = previewingFontAxisMap.value.toMutableMap()
        axisMap[key] = value
        overrideFontAxisMap.value = axisMap
    }

    fun applyFontAxes() {
        _selectedTab.value = Tab.STYLE
    }

    fun revertFontAxes() {
        overrideFontAxisMap.value = null
        _selectedTab.value = Tab.STYLE
    }

    // Clock size
    private val overridingClockSize = MutableStateFlow<ClockSize?>(null)
    private val isClockSizeEdited =
        combine(overridingClockSize, clockPickerInteractor.selectedClockSize) {
            overridingClockSize,
            selectedClockSize ->
            overridingClockSize != null && overridingClockSize != selectedClockSize
        }
    val previewingClockSize =
        combine(overridingClockSize, clockPickerInteractor.selectedClockSize) {
            overridingClockSize,
            selectedClockSize ->
            overridingClockSize ?: selectedClockSize
        }
    val onClockSizeSwitchCheckedChange: Flow<(() -> Unit)> =
        previewingClockSize.map {
            {
                when (it) {
                    ClockSize.DYNAMIC -> overridingClockSize.value = ClockSize.SMALL
                    ClockSize.SMALL -> overridingClockSize.value = ClockSize.DYNAMIC
                }
            }
        }

    // Clock color
    // 0 - 100
    private val overridingClockColorId = MutableStateFlow<String?>(null)
    private val isClockColorIdEdited =
        combine(overridingClockColorId, clockPickerInteractor.selectedColorId) {
            overridingClockColorId,
            selectedColorId ->
            overridingClockColorId != null && (overridingClockColorId != selectedColorId)
        }
    private val previewingClockColorId =
        combine(overridingClockColorId, clockPickerInteractor.selectedColorId) {
            overridingClockColorId,
            selectedColorId ->
            overridingClockColorId ?: selectedColorId ?: DEFAULT_CLOCK_COLOR_ID
        }

    private val overridingSliderProgress = MutableStateFlow<Int?>(null)
    private val isSliderProgressEdited =
        combine(overridingSliderProgress, clockPickerInteractor.colorToneProgress) {
            overridingSliderProgress,
            colorToneProgress ->
            overridingSliderProgress != null && (overridingSliderProgress != colorToneProgress)
        }
    val previewingSliderProgress: Flow<Int> =
        combine(overridingSliderProgress, clockPickerInteractor.colorToneProgress) {
            overridingSliderProgress,
            colorToneProgress ->
            overridingSliderProgress ?: colorToneProgress
        }
    val isSliderEnabled: Flow<Boolean> =
        combine(previewingClock, previewingClockColorId) { clock, clockColorId ->
                clock.isReactiveToTone && clockColorId != DEFAULT_CLOCK_COLOR_ID
            }
            .distinctUntilChanged()

    fun onSliderProgressChanged(progress: Int) {
        overridingSliderProgress.value = progress
    }

    val previewingSeedColor: Flow<Int?> =
        combine(previewingClockColorId, previewingSliderProgress) { clockColorId, sliderProgress ->
            val clockColorViewModel =
                if (clockColorId == DEFAULT_CLOCK_COLOR_ID) null else colorMap[clockColorId]
            if (clockColorViewModel == null) {
                null
            } else {
                blendColorWithTone(
                    color = clockColorViewModel.color,
                    colorTone = clockColorViewModel.getColorTone(sliderProgress),
                )
            }
        }

    val clockColorOptions: Flow<List<OptionItemViewModel<ColorOptionIconViewModel>>> =
        colorPickerInteractor.colorOptions.map { colorOptions ->
            // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
            // events from ClockRegistry upstream, caused by sliding the saturation level bar.
            delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
            buildList {
                val defaultThemeColorOptionViewModel =
                    (colorOptions[ColorType.WALLPAPER_COLOR]?.find { it.isSelected })
                        ?.toOptionItemViewModel(context)
                        ?: (colorOptions[ColorType.PRESET_COLOR]?.find { it.isSelected })
                            ?.toOptionItemViewModel(context)
                if (defaultThemeColorOptionViewModel != null) {
                    add(defaultThemeColorOptionViewModel)
                }

                colorMap.values.forEachIndexed { index, colorModel ->
                    val isSelectedFlow =
                        previewingClockColorId
                            .map { colorMap.keys.indexOf(it) == index }
                            .stateIn(viewModelScope)
                    add(
                        OptionItemViewModel<ColorOptionIconViewModel>(
                            key = MutableStateFlow(colorModel.colorId) as StateFlow<String>,
                            payload =
                                ColorOptionIconViewModel(
                                    lightThemeColor0 = colorModel.color,
                                    lightThemeColor1 = colorModel.color,
                                    lightThemeColor2 = colorModel.color,
                                    lightThemeColor3 = colorModel.color,
                                    darkThemeColor0 = colorModel.color,
                                    darkThemeColor1 = colorModel.color,
                                    darkThemeColor2 = colorModel.color,
                                    darkThemeColor3 = colorModel.color,
                                ),
                            text =
                                Text.Loaded(
                                    context.getString(
                                        R.string.content_description_color_option,
                                        index,
                                    )
                                ),
                            isTextUserVisible = false,
                            isSelected = isSelectedFlow,
                            onClicked =
                                isSelectedFlow.map { isSelected ->
                                    if (isSelected) {
                                        null
                                    } else {
                                        {
                                            overridingClockColorId.value = colorModel.colorId
                                            overridingSliderProgress.value =
                                                ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                                        }
                                    }
                                },
                        )
                    )
                }
            }
        }

    private suspend fun ColorOptionModel.toOptionItemViewModel(
        context: Context
    ): OptionItemViewModel<ColorOptionIconViewModel> {
        val lightThemeColors =
            (colorOption as ColorOptionImpl)
                .previewInfo
                .resolveColors(
                    /** darkTheme= */
                    false
                )
        val darkThemeColors =
            colorOption.previewInfo.resolveColors(
                /** darkTheme= */
                true
            )
        val isSelectedFlow =
            previewingClockColorId.map { it == DEFAULT_CLOCK_COLOR_ID }.stateIn(viewModelScope)
        return OptionItemViewModel<ColorOptionIconViewModel>(
            key = MutableStateFlow(key) as StateFlow<String>,
            payload =
                ColorOptionIconViewModel(
                    lightThemeColor0 = lightThemeColors[0],
                    lightThemeColor1 = lightThemeColors[1],
                    lightThemeColor2 = lightThemeColors[2],
                    lightThemeColor3 = lightThemeColors[3],
                    darkThemeColor0 = darkThemeColors[0],
                    darkThemeColor1 = darkThemeColors[1],
                    darkThemeColor2 = darkThemeColors[2],
                    darkThemeColor3 = darkThemeColors[3],
                ),
            text = Text.Loaded(context.getString(R.string.default_theme_title)),
            isTextUserVisible = true,
            isSelected = isSelectedFlow,
            onClicked =
                isSelectedFlow.map { isSelected ->
                    if (isSelected) {
                        null
                    } else {
                        {
                            overridingClockColorId.value = DEFAULT_CLOCK_COLOR_ID
                            overridingSliderProgress.value =
                                ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                        }
                    }
                },
        )
    }

    private val isEdited =
        combine(
            isClockEdited,
            isClockSizeEdited,
            isClockColorIdEdited,
            isSliderProgressEdited,
            isFontAxisMapEdited,
        ) {
            isClockEdited,
            isClockSizeEdited,
            isClockColorEdited,
            isSliderProgressEdited,
            isFontAxisMapEdited ->
            isClockEdited ||
                isClockSizeEdited ||
                isClockColorEdited ||
                isSliderProgressEdited ||
                isFontAxisMapEdited
        }

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(
            isEdited,
            previewingClock,
            previewingClockSize,
            previewingClockColorId,
            previewingSliderProgress,
            previewingFontAxisMap,
        ) { array ->
            val isEdited = array[0] as Boolean
            val clock = array[1] as ClockMetadataModel
            val size = array[2] as ClockSize
            val previewingColorId = array[3] as String
            val previewProgress = array[4] as Int
            val axisMap = array[5] as Map<String, Float>
            if (isEdited) {
                {
                    clockPickerInteractor.applyClock(
                        clockId = clock.clockId,
                        size = size,
                        selectedColorId = previewingColorId,
                        colorToneProgress = previewProgress,
                        seedColor =
                            colorMap[previewingColorId]?.let {
                                blendColorWithTone(
                                    color = it.color,
                                    colorTone = it.getColorTone(previewProgress),
                                )
                            },
                        axisSettings = axisMap.map { ClockFontAxisSetting(it.key, it.value) },
                    )
                }
            } else {
                null
            }
        }

    fun resetPreview() {
        overridingClock.value = null
        overridingClockSize.value = null
        overridingClockColorId.value = null
        overridingSliderProgress.value = null
        overrideFontAxisMap.value = null
        _selectedTab.value = Tab.STYLE
    }

    companion object {
        private const val DEFAULT_CLOCK_COLOR_ID = "DEFAULT"
        private val helperColorLab: DoubleArray by lazy { DoubleArray(3) }

        fun blendColorWithTone(color: Int, colorTone: Double): Int {
            ColorUtils.colorToLAB(color, helperColorLab)
            return ColorUtils.LABToColor(colorTone, helperColorLab[1], helperColorLab[2])
        }

        const val COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
        const val CLOCKS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ClockPickerViewModel
    }
}
