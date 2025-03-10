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

package com.android.wallpaper.customization.ui.binder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder2
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.viewmodel.ClockFloatingSheetHeightsViewModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.ClockStyleModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.Tab
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import java.lang.ref.WeakReference
import kotlin.math.roundToInt
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object ClockFloatingSheetBinder {
    private const val SLIDER_ENABLED_ALPHA = 1f
    private const val SLIDER_DISABLED_ALPHA = .3f
    private const val ANIMATION_DURATION = 200L

    private val _clockFloatingSheetHeights: MutableStateFlow<ClockFloatingSheetHeightsViewModel> =
        MutableStateFlow(ClockFloatingSheetHeightsViewModel())
    private val clockFloatingSheetHeights: Flow<ClockFloatingSheetHeightsViewModel> =
        _clockFloatingSheetHeights.asStateFlow().filterNotNull()

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.clockPickerViewModel
        val appContext = view.context.applicationContext
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == CLOCK }

        val tabs: FloatingToolbar = view.requireViewById(R.id.floating_toolbar)
        val tabContainer =
            tabs.findViewById<ViewGroup>(com.android.wallpaper.R.id.floating_toolbar_tab_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(tabContainer.background), color)
            },
            color = colorUpdateViewModel.floatingToolbarBackground,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )
        val tabAdapter =
            FloatingToolbarTabAdapter(
                    colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                    shouldAnimateColor = isFloatingSheetActive,
                )
                .also { tabs.setAdapter(it) }

        val floatingSheetContainer: ViewGroup =
            view.requireViewById(R.id.floating_sheet_content_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(
                    DrawableCompat.wrap(floatingSheetContainer.background),
                    color,
                )
            },
            color = colorUpdateViewModel.colorSurfaceBright,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        // Clock style
        val clockStyleContent: View = view.requireViewById(R.id.clock_floating_sheet_style_content)
        val isClockStyleActive = {
            isFloatingSheetActive() && viewModel.selectedTab.value == Tab.STYLE
        }
        val clockStyleAdapter =
            createClockStyleOptionItemAdapter(
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isClockStyleActive,
                lifecycleOwner = lifecycleOwner,
            )
        val clockStyleList: RecyclerView = view.requireViewById(R.id.clock_style_list)
        clockStyleList.initStyleList(appContext, clockStyleAdapter)
        val axisPresetSlider: Slider =
            clockStyleContent.requireViewById(R.id.clock_axis_preset_slider)

        // Clock color
        val clockColorContent: View = view.requireViewById(R.id.clock_floating_sheet_color_content)

        val clockColorAdapter =
            createClockColorOptionItemAdapter(
                uiMode = view.resources.configuration.uiMode,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
            )
        val clockColorList: RecyclerView = view.requireViewById(R.id.clock_color_list)
        clockColorList.adapter = clockColorAdapter
        clockColorList.layoutManager =
            LinearLayoutManager(appContext, LinearLayoutManager.HORIZONTAL, false)

        val clockColorSlider: Slider = view.requireViewById(R.id.clock_color_slider)
        SliderColorBinder.bind(
            slider = clockColorSlider,
            colorUpdateViewModel = colorUpdateViewModel,
            shouldAnimateColor = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        clockColorSlider.apply {
            valueFrom = ClockMetadataModel.MIN_COLOR_TONE_PROGRESS.toFloat()
            valueTo = ClockMetadataModel.MAX_COLOR_TONE_PROGRESS.toFloat()
            labelBehavior = LabelFormatter.LABEL_GONE
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.onSliderProgressChanged(value.roundToInt())
                }
            }
        }
        val isClockColorActive = {
            isFloatingSheetActive() && viewModel.selectedTab.value == Tab.COLOR
        }
        ColorUpdateBinder.bind(
            setColor = { color ->
                clockColorContent
                    .requireViewById<TextView>(R.id.clock_color_title)
                    .setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isClockColorActive,
            lifecycleOwner = lifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color ->
                clockColorContent
                    .requireViewById<TextView>(R.id.clock_color_description)
                    .setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isClockColorActive,
            lifecycleOwner = lifecycleOwner,
        )

        // Clock size
        val clockSizeContent: View = view.requireViewById(R.id.clock_floating_sheet_size_content)
        val clockSizeSwitch: MaterialSwitch =
            clockSizeContent.requireViewById(R.id.clock_style_clock_size_switch)
        ColorUpdateBinder.bind(
            setColor = { color ->
                clockSizeContent
                    .requireViewById<TextView>(R.id.clock_style_clock_size_title)
                    .setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isClockStyleActive,
            lifecycleOwner = lifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color ->
                clockSizeContent
                    .requireViewById<TextView>(R.id.clock_style_clock_size_description)
                    .setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isClockStyleActive,
            lifecycleOwner = lifecycleOwner,
        )

        clockStyleContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (
                        clockStyleContent.height != 0 &&
                            axisPresetSlider.height != 0 &&
                            _clockFloatingSheetHeights.value.axisPresetSliderHeight == null &&
                            _clockFloatingSheetHeights.value.clockStyleContentHeight == null
                    ) {
                        _clockFloatingSheetHeights.value =
                            _clockFloatingSheetHeights.value.copy(
                                clockStyleContentHeight = clockStyleContent.height,
                                axisPresetSliderHeight = axisPresetSlider.height,
                            )
                        clockStyleContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        )

        clockColorContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (
                        clockColorContent.height != 0 &&
                            _clockFloatingSheetHeights.value.clockColorContentHeight == null
                    ) {
                        _clockFloatingSheetHeights.value =
                            _clockFloatingSheetHeights.value.copy(
                                clockColorContentHeight = clockColorContent.height
                            )
                        clockColorContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        )

        clockSizeContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (
                        clockSizeContent.height != 0 &&
                            _clockFloatingSheetHeights.value.clockSizeContentHeight == null
                    ) {
                        _clockFloatingSheetHeights.value =
                            _clockFloatingSheetHeights.value.copy(
                                clockSizeContentHeight = clockSizeContent.height
                            )
                        clockSizeContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            var currentTab: Tab = Tab.STYLE
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect { tabAdapter.submitList(it) } }

                launch {
                    combine(
                            clockFloatingSheetHeights,
                            viewModel.selectedTab,
                            viewModel.shouldShowPresetSlider,
                            ::Triple,
                        )
                        .collect { (heights, selectedTab, shouldShowPresetSlider) ->
                            val (
                                clockStyleContentHeight,
                                clockColorContentHeight,
                                clockSizeContentHeight,
                                axisPresetSliderHeight) =
                                heights
                            clockStyleContentHeight ?: return@collect
                            clockColorContentHeight ?: return@collect
                            clockSizeContentHeight ?: return@collect
                            axisPresetSliderHeight ?: return@collect

                            val fromHeight = floatingSheetContainer.height
                            val toHeight =
                                when (selectedTab) {
                                    Tab.STYLE ->
                                        if (shouldShowPresetSlider) clockStyleContentHeight
                                        else clockStyleContentHeight - axisPresetSliderHeight
                                    Tab.COLOR -> clockColorContentHeight
                                    Tab.SIZE -> clockSizeContentHeight
                                }
                            val currentContent: View =
                                when (currentTab) {
                                    Tab.STYLE -> clockStyleContent
                                    Tab.COLOR -> clockColorContent
                                    Tab.SIZE -> clockSizeContent
                                }
                            val shouldCurrentContentFadeOut = currentTab != selectedTab
                            // Start to animate the content height
                            ValueAnimator.ofInt(fromHeight, toHeight)
                                .apply {
                                    addUpdateListener { valueAnimator ->
                                        val value = valueAnimator.animatedValue as Int
                                        floatingSheetContainer.layoutParams =
                                            floatingSheetContainer.layoutParams.apply {
                                                height = value
                                            }
                                        if (shouldCurrentContentFadeOut) {
                                            currentContent.alpha =
                                                getAlpha(fromHeight, toHeight, value)
                                        }
                                    }
                                    duration = ANIMATION_DURATION
                                    addListener(
                                        object : AnimatorListenerAdapter() {
                                            override fun onAnimationEnd(animation: Animator) {
                                                clockStyleContent.isVisible =
                                                    selectedTab == Tab.STYLE
                                                clockStyleContent.alpha = 1f
                                                clockColorContent.isVisible =
                                                    selectedTab == Tab.COLOR
                                                clockColorContent.alpha = 1f
                                                clockSizeContent.isVisible = selectedTab == Tab.SIZE
                                                clockSizeContent.alpha = 1f
                                            }
                                        }
                                    )
                                }
                                .start()
                            currentTab = selectedTab
                        }
                }

                launch {
                    viewModel.shouldShowPresetSlider.collect { axisPresetSlider.isVisible = it }
                }

                launch {
                    viewModel.clockStyleOptions.collect { styleOptions ->
                        clockStyleAdapter.setItems(styleOptions) {
                            var indexToFocus = styleOptions.indexOfFirst { it.isSelected.value }
                            indexToFocus = if (indexToFocus < 0) 0 else indexToFocus
                            (clockStyleList.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(indexToFocus, 0)
                        }
                    }
                }

                launch {
                    viewModel.clockColorOptions.collect { colorOptions ->
                        clockColorAdapter.setItems(colorOptions) {
                            var indexToFocus = colorOptions.indexOfFirst { it.isSelected.value }
                            indexToFocus = if (indexToFocus < 0) 0 else indexToFocus
                            (clockColorList.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(indexToFocus, 0)
                        }
                    }
                }

                launch {
                    viewModel.previewingSliderProgress.collect { progress ->
                        clockColorSlider.value = progress.toFloat()
                    }
                }

                launch {
                    viewModel.isSliderEnabled.collect { isEnabled ->
                        clockColorSlider.isEnabled = isEnabled
                        clockColorSlider.alpha =
                            if (isEnabled) SLIDER_ENABLED_ALPHA else SLIDER_DISABLED_ALPHA
                    }
                }

                launch {
                    var binding: SwitchColorBinder.Binding? = null
                    viewModel.previewingClockSize.collect { size ->
                        when (size) {
                            ClockSize.DYNAMIC -> clockSizeSwitch.isChecked = true
                            ClockSize.SMALL -> clockSizeSwitch.isChecked = false
                        }
                        binding?.destroy()
                        binding =
                            SwitchColorBinder.bind(
                                switch = clockSizeSwitch,
                                isChecked =
                                    when (size) {
                                        ClockSize.DYNAMIC -> true
                                        ClockSize.SMALL -> false
                                    },
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isClockStyleActive,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    viewModel.onClockSizeSwitchCheckedChange.collect { onCheckedChange ->
                        clockSizeSwitch.setOnCheckedChangeListener { _, _ ->
                            onCheckedChange.invoke()
                        }
                    }
                }
            }
        }
    }

    private fun createClockStyleOptionItemAdapter(
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): OptionItemAdapter2<ClockStyleModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.clock_style_option,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { view: View, styleModel: ClockStyleModel ->
                view
                    .findViewById<ImageView>(R.id.foreground)
                    ?.setImageDrawable(styleModel.thumbnail)
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    private fun RecyclerView.initStyleList(
        context: Context,
        adapter: OptionItemAdapter2<ClockStyleModel>,
    ) {
        this.adapter = adapter
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        addItemDecoration(
            SingleRowListItemSpacing(
                context.resources.getDimensionPixelSize(
                    R.dimen.floating_sheet_content_horizontal_padding
                ),
                context.resources.getDimensionPixelSize(
                    R.dimen.floating_sheet_list_item_horizontal_space
                ),
            )
        )
    }

    private fun createClockColorOptionItemAdapter(
        uiMode: Int,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): OptionItemAdapter2<ColorOptionIconViewModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.color_option2,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { itemView: View, colorIcon: ColorOptionIconViewModel ->
                val colorOptionIconView: ColorOptionIconView2 =
                    itemView.requireViewById(com.android.wallpaper.R.id.background)
                val night = uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
                val binding =
                    ColorOptionIconBinder2.bind(
                        view = colorOptionIconView,
                        viewModel = colorIcon,
                        darkTheme = night,
                        colorUpdateViewModel = colorUpdateViewModel,
                        shouldAnimateColor = shouldAnimateColor,
                        lifecycleOwner = lifecycleOwner,
                    )
                return@OptionItemAdapter2 DisposableHandle { binding.destroy() }
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    // Alpha is 1 when current height is from height, and 0 when current height is to height.
    private fun getAlpha(fromHeight: Int, toHeight: Int, currentHeight: Int): Float =
        (1 - (currentHeight - fromHeight).toFloat() / (toHeight - fromHeight).toFloat())
}
