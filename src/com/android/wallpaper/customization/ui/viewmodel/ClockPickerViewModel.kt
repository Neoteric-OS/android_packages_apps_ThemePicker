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
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.systemui.plugins.clocks.AxisPresetConfig
import com.android.systemui.plugins.clocks.AxisPresetConfig.IndexedStyle
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlin.math.roundToInt
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/** View model for the clock customization screen. */
class ClockPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    resources: Resources,
    private val clockPickerInteractor: ClockPickerInteractor,
    colorPickerInteractor: ColorPickerInteractor2,
    private val logger: ThemesUserEventLogger,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    enum class Tab {
        STYLE,
        COLOR,
        SIZE,
    }

    private val colorMap = ClockColorViewModel.getPresetColorMap(context.resources)

    // Tabs
    private val _selectedTab = MutableStateFlow(Tab.STYLE)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        selectedTab.map {
            listOf(
                FloatingToolbarTabViewModel(
                    icon =
                        Icon.Resource(
                            res = R.drawable.ic_clock_filled_24px,
                            contentDescription = Text.Resource(R.string.clock_style),
                        ),
                    text = context.getString(R.string.clock_style),
                    isSelected = it == Tab.STYLE,
                    onClick =
                        if (it == Tab.STYLE) null
                        else {
                            { _selectedTab.value = Tab.STYLE }
                        },
                ),
                FloatingToolbarTabViewModel(
                    icon =
                        Icon.Resource(
                            res = R.drawable.ic_palette_filled_24px,
                            contentDescription = Text.Resource(R.string.clock_color),
                        ),
                    text = context.getString(R.string.clock_color),
                    isSelected = it == Tab.COLOR,
                    onClick =
                        if (it == Tab.COLOR) null
                        else {
                            { _selectedTab.value = Tab.COLOR }
                        },
                ),
                FloatingToolbarTabViewModel(
                    icon =
                        Icon.Resource(
                            res = R.drawable.ic_font_size_filled_24px,
                            contentDescription = Text.Resource(R.string.clock_size),
                        ),
                    text = context.getString(R.string.clock_size),
                    isSelected = it == Tab.SIZE,
                    onClick =
                        if (it == Tab.SIZE) null
                        else {
                            { _selectedTab.value = Tab.SIZE }
                        },
                ),
            )
        }

    // Clock style
    private val overridingClock = MutableStateFlow<ClockMetadataModel?>(null)
    val selectedClock = clockPickerInteractor.selectedClock
    val previewingClock =
        combine(overridingClock, selectedClock) { overridingClock, selectedClock ->
                (overridingClock ?: selectedClock)
            }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    private val isClockEdited =
        combine(overridingClock, selectedClock) { overridingClock, selectedClock ->
            overridingClock != null && overridingClock.clockId != selectedClock.clockId
        }

    suspend fun getIsShadeLayoutWide() = clockPickerInteractor.getIsShadeLayoutWide()

    suspend fun getUdfpsLocation() = clockPickerInteractor.getUdfpsLocation()

    data class ClockStyleModel(val thumbnail: Drawable)

    @OptIn(ExperimentalCoroutinesApi::class)
    val clockStyleOptions: StateFlow<List<OptionItemViewModel2<ClockStyleModel>>> =
        clockPickerInteractor.allClocks
            .mapLatest { allClocks ->
                // Delay to avoid the case that the full list of clocks is not initiated.
                delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
                val allClockMap = allClocks.groupBy { it.axisPresetConfig != null }
                buildList {
                    allClockMap[true]?.map { add(it.toOption(resources)) }
                    allClockMap[false]?.map { add(it.toOption(resources)) }
                }
            }
            // makes sure that the operations above this statement are executed on I/O dispatcher
            // while parallelism limits the number of threads this can run on which makes sure that
            // the flows run sequentially
            .flowOn(backgroundDispatcher.limitedParallelism(1))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Clock font presets
    private val overridingClockPresetIndexedStyle: MutableStateFlow<IndexedStyle?> =
        MutableStateFlow(null)
    private val selectedClockPresetIndexedStyle: Flow<IndexedStyle?> =
        previewingClock
            .map { it.axisPresetConfig?.current }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val previewingClockPresetIndexedStyle: Flow<IndexedStyle?> =
        combine(overridingClockPresetIndexedStyle, selectedClockPresetIndexedStyle) {
            overridingClockPresetIndexedStyle,
            selectedClockPresetIndexedStyle ->
            overridingClockPresetIndexedStyle ?: selectedClockPresetIndexedStyle
        }
    private val isClockAxisStyleEdited: Flow<Boolean> =
        combine(overridingClockPresetIndexedStyle, selectedClockPresetIndexedStyle) {
            overridingClockPresetIndexedStyle,
            selectedClockPresetIndexedStyle ->
            overridingClockPresetIndexedStyle != null &&
                (overridingClockPresetIndexedStyle.style != selectedClockPresetIndexedStyle?.style)
        }

    private val groups: Flow<List<AxisPresetConfig.Group>?> =
        previewingClock.map { it.axisPresetConfig?.groups }
    private val previewingClockPresetGroupIndex: Flow<Int> =
        previewingClockPresetIndexedStyle.map { it?.groupIndex ?: 0 }.distinctUntilChanged()
    val shouldShowPresetSlider: Flow<Boolean> = previewingClock.map { it.axisPresetConfig != null }
    val axisPresetsSliderViewModel: Flow<ClockAxisPresetSliderViewModel?> =
        combine(groups, previewingClockPresetGroupIndex) { groups, previewingClockPresetGroupIndex
            ->
            if (groups.isNullOrEmpty()) {
                null
            } else {
                val group = groups[previewingClockPresetGroupIndex]
                ClockAxisPresetSliderViewModel(
                    valueFrom = 0F,
                    valueTo = (group.presets.size - 1).toFloat(),
                    stepSize = 1F,
                    onSliderStopTrackingTouch = { value ->
                        val presetIndex = value.roundToInt()
                        overridingClockPresetIndexedStyle.value =
                            IndexedStyle(
                                groupIndex = previewingClockPresetGroupIndex,
                                presetIndex = presetIndex,
                                style = group.presets[presetIndex],
                            )
                    },
                )
            }
        }
    val axisPresetsSliderSelectedValue: Flow<Float> =
        previewingClockPresetIndexedStyle.map { it?.presetIndex?.toFloat() }.filterNotNull()
    val onClockFaceClicked: Flow<() -> Unit> =
        combine(groups, previewingClockPresetIndexedStyle) { groups, previewingIndexedStyle ->
            if (groups.isNullOrEmpty()) {
                {}
            } else {
                val groupCount = groups.size
                if (groupCount == 1) {
                    {}
                } else {
                    val currentGroupIndex = previewingIndexedStyle?.groupIndex ?: 0
                    val nextGroupIndex = (currentGroupIndex + 1) % groupCount
                    val nextPresetIndex = previewingIndexedStyle?.presetIndex ?: (groupCount / 2)
                    val nextGroup = groups[nextGroupIndex]
                    {
                        overridingClockPresetIndexedStyle.value =
                            IndexedStyle(
                                groupIndex = nextGroupIndex,
                                presetIndex = nextPresetIndex,
                                style = nextGroup.presets[nextPresetIndex],
                            )
                    }
                }
            }
        }

    private suspend fun ClockMetadataModel.toOption(
        resources: Resources
    ): OptionItemViewModel2<ClockStyleModel> {
        val isSelectedFlow = previewingClock.map { it.clockId == clockId }.stateIn(viewModelScope)
        val contentDescription =
            resources.getString(R.string.select_clock_action_description, description)
        return OptionItemViewModel2<ClockStyleModel>(
            key = MutableStateFlow(clockId) as StateFlow<String>,
            payload = ClockStyleModel(thumbnail = thumbnail),
            text = Text.Loaded(contentDescription),
            isTextUserVisible = false,
            isSelected = isSelectedFlow,
            onClicked =
                isSelectedFlow.map { isSelected ->
                    if (isSelected) {
                        null
                    } else {
                        fun() {
                            overridingClock.value = this
                        }
                    }
                },
        )
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

    // Clock color slider progress. Range is 0 - 100.
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

    val clockColorOptions: Flow<List<OptionItemViewModel2<ColorOptionIconViewModel>>> =
        colorPickerInteractor.selectedColorOption.map { selectedColorOption ->
            // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
            // events from ClockRegistry upstream, caused by sliding the saturation level bar.
            delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
            buildList {
                selectedColorOption?.let { add(it.toOptionItemViewModel(context)) }

                colorMap.values.forEachIndexed { index, colorModel ->
                    val isSelectedFlow =
                        previewingClockColorId
                            .map { colorMap.keys.indexOf(it) == index }
                            .stateIn(viewModelScope)
                    add(
                        OptionItemViewModel2<ColorOptionIconViewModel>(
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

    private suspend fun ColorOption.toOptionItemViewModel(
        context: Context
    ): OptionItemViewModel2<ColorOptionIconViewModel> {
        val lightThemeColors =
            (this as ColorOptionImpl)
                .previewInfo
                .resolveColors(
                    /** darkTheme= */
                    false
                )
        val darkThemeColors =
            this.previewInfo.resolveColors(
                /** darkTheme= */
                true
            )
        val isSelectedFlow =
            previewingClockColorId.map { it == DEFAULT_CLOCK_COLOR_ID }.stateIn(viewModelScope)
        val key = "${this.type}::${this.style}::${this.serializedPackages}"
        return OptionItemViewModel2<ColorOptionIconViewModel>(
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
            isClockAxisStyleEdited,
            isClockSizeEdited,
            isClockColorIdEdited,
            isSliderProgressEdited,
        ) {
            isClockEdited,
            isClockAxisStyleEdited,
            isClockSizeEdited,
            isClockColorEdited,
            isSliderProgressEdited ->
            isClockEdited ||
                isClockAxisStyleEdited ||
                isClockSizeEdited ||
                isClockColorEdited ||
                isSliderProgressEdited
        }
    private val onApplyClicked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onApply: Flow<(suspend () -> Unit)?> =
        combine(
            onApplyClicked,
            isEdited,
            previewingClock,
            previewingClockSize,
            previewingClockColorId,
            previewingSliderProgress,
            previewingClockPresetIndexedStyle,
        ) { array ->
            val onApplyClicked: Boolean = array[0] as Boolean
            val isEdited: Boolean = array[1] as Boolean
            val clock: ClockMetadataModel = array[2] as ClockMetadataModel
            val size: ClockSize = array[3] as ClockSize
            val previewingColorId: String = array[4] as String
            val previewProgress: Int = array[5] as Int
            val clockAxisStyle: ClockAxisStyle =
                (array[6] as? IndexedStyle)?.style ?: ClockAxisStyle()
            if (isEdited && !onApplyClicked) {
                {
                    this.onApplyClicked.value = true
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
                        axisSettings = clockAxisStyle,
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
        overridingClockPresetIndexedStyle.value = null
        _selectedTab.value = Tab.STYLE
        onApplyClicked.value = false
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
