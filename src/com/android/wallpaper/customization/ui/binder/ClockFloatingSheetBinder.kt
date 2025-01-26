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
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder
import com.android.customization.picker.color.ui.view.ColorOptionIconView
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.systemui.plugins.clocks.AxisType
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.view.ClockFontSliderViewHolder
import com.android.wallpaper.customization.ui.view.ClockFontSwitchViewHolder
import com.android.wallpaper.customization.ui.viewmodel.ClockFloatingSheetHeightsViewModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.ClockStyleModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.Tab
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import java.lang.ref.WeakReference
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

        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
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

        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.floating_sheet_content_container)
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
        val clockStyleContent = view.requireViewById<View>(R.id.clock_floating_sheet_style_content)
        val clockStyleAdapter = createClockStyleOptionItemAdapter(lifecycleOwner)
        val clockStyleList =
            view.requireViewById<RecyclerView>(R.id.clock_style_list).apply {
                initStyleList(appContext, clockStyleAdapter)
            }

        // Clock font editor
        val clockFontContent =
            view.requireViewById<ViewGroup>(R.id.clock_floating_sheet_font_content)
        val clockFontToolbar = view.requireViewById<ViewGroup>(R.id.clock_font_toolbar)
        clockFontToolbar.requireViewById<View>(R.id.clock_font_revert).setOnClickListener {
            viewModel.cancelFontAxes()
        }
        clockFontToolbar.requireViewById<View>(R.id.clock_font_apply).setOnClickListener {
            viewModel.confirmFontAxes()
        }

        // Clock color
        val clockColorContent = view.requireViewById<View>(R.id.clock_floating_sheet_color_content)
        val clockColorAdapter =
            createClockColorOptionItemAdapter(view.resources.configuration.uiMode, lifecycleOwner)
        val clockColorList =
            view.requireViewById<RecyclerView>(R.id.clock_color_list).apply {
                initColorList(appContext, clockColorAdapter)
            }
        val clockColorSlider: SeekBar = view.requireViewById(R.id.clock_color_slider)
        clockColorSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        viewModel.onSliderProgressChanged(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            }
        )

        // Clock size switch
        val clockSizeSwitch = view.requireViewById<Switch>(R.id.clock_style_clock_size_switch)

        clockStyleContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (
                        clockStyleContent.height != 0 &&
                            _clockFloatingSheetHeights.value.clockStyleContentHeight == null
                    ) {
                        _clockFloatingSheetHeights.value =
                            _clockFloatingSheetHeights.value.copy(
                                clockStyleContentHeight = clockStyleContent.height
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

        clockFontContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (
                        clockFontContent.height != 0 &&
                            _clockFloatingSheetHeights.value.clockFontContentHeight == null
                    ) {
                        _clockFloatingSheetHeights.value =
                            _clockFloatingSheetHeights.value.copy(
                                clockFontContentHeight = clockFontContent.height
                            )
                        clockColorContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            var currentContent: View = clockStyleContent
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect { tabAdapter.submitList(it) } }

                launch {
                    combine(clockFloatingSheetHeights, viewModel.selectedTab, ::Pair).collect {
                        (heights, selectedTab) ->
                        val (
                            clockStyleContentHeight,
                            clockColorContentHeight,
                            clockFontContentHeight) =
                            heights
                        clockStyleContentHeight ?: return@collect
                        clockColorContentHeight ?: return@collect
                        clockFontContentHeight ?: return@collect

                        val fromHeight = floatingSheetContainer.height
                        val toHeight =
                            when (selectedTab) {
                                Tab.STYLE -> clockStyleContentHeight
                                Tab.COLOR -> clockColorContentHeight
                                Tab.FONT -> clockFontContentHeight
                            }
                        // Start to animate the content height
                        ValueAnimator.ofInt(fromHeight, toHeight)
                            .apply {
                                addUpdateListener { valueAnimator ->
                                    val value = valueAnimator.animatedValue as Int
                                    floatingSheetContainer.layoutParams =
                                        floatingSheetContainer.layoutParams.apply { height = value }
                                    currentContent.alpha = getAlpha(fromHeight, toHeight, value)
                                }
                                duration = ANIMATION_DURATION
                                addListener(
                                    object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            clockStyleContent.isVisible = selectedTab == Tab.STYLE
                                            clockStyleContent.alpha = 1f
                                            clockColorContent.isVisible = selectedTab == Tab.COLOR
                                            clockColorContent.alpha = 1f
                                            clockFontContent.isVisible = selectedTab == Tab.FONT
                                            clockFontContent.alpha = 1f
                                            currentContent =
                                                when (selectedTab) {
                                                    Tab.STYLE -> clockStyleContent
                                                    Tab.COLOR -> clockColorContent
                                                    Tab.FONT -> clockFontContent
                                                }
                                            // Also update the floating toolbar when the height
                                            // animation ends.
                                            tabs.isVisible = selectedTab != Tab.FONT
                                            clockFontToolbar.isVisible = selectedTab == Tab.FONT
                                        }
                                    }
                                )
                            }
                            .start()
                    }
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
                        clockColorSlider.setProgress(progress, true)
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
                    viewModel.previewingClockSize.collect { size ->
                        when (size) {
                            ClockSize.DYNAMIC -> clockSizeSwitch.isChecked = true
                            ClockSize.SMALL -> clockSizeSwitch.isChecked = false
                        }
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

        bindClockFontContent(
            clockFontContent = clockFontContent,
            viewModel = viewModel,
            lifecycleOwner = lifecycleOwner,
        )
    }

    private fun bindClockFontContent(
        clockFontContent: View,
        viewModel: ClockPickerViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val sliderViewList =
            listOf(
                ClockFontSliderViewHolder(
                    name = clockFontContent.requireViewById(R.id.clock_axis_slider_name1),
                    slider = clockFontContent.requireViewById(R.id.clock_axis_slider1),
                ),
                ClockFontSliderViewHolder(
                    name = clockFontContent.requireViewById(R.id.clock_axis_slider_name2),
                    slider = clockFontContent.requireViewById(R.id.clock_axis_slider2),
                ),
            )
        val switchViewList =
            listOf(
                ClockFontSwitchViewHolder(
                    name = clockFontContent.requireViewById(R.id.clock_axis_switch_name1),
                    switch = clockFontContent.requireViewById(R.id.clock_axis_switch1),
                ),
                ClockFontSwitchViewHolder(
                    name = clockFontContent.requireViewById(R.id.clock_axis_switch_name2),
                    switch = clockFontContent.requireViewById(R.id.clock_axis_switch2),
                ),
            )
        val sliderViewMap: MutableMap<String, ClockFontSliderViewHolder> = mutableMapOf()
        val switchViewMap: MutableMap<String, ClockFontSwitchViewHolder> = mutableMapOf()

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedClockFontAxes.filterNotNull().collect { fontAxes ->
                        // This data flow updates only when a new clock style is selected. We
                        // initiate the clock font content with regard to that clock style.
                        sliderViewMap.clear()
                        switchViewMap.clear()

                        // Initiate the slider views
                        val floatAxisList = fontAxes.filter { it.type == AxisType.Float }
                        sliderViewList.forEachIndexed { i, viewHolder ->
                            val floatAxis = floatAxisList.getOrNull(i)
                            viewHolder.setIsVisible(floatAxis != null)
                            floatAxis?.let {
                                sliderViewMap[floatAxis.key] = viewHolder
                                viewHolder.initView(it) { value ->
                                    viewModel.updatePreviewFontAxis(floatAxis.key, value)
                                }
                            }
                        }

                        // Initiate the switch views
                        val booleanAxisList = fontAxes.filter { it.type == AxisType.Boolean }
                        switchViewList.forEachIndexed { i, viewHolder ->
                            val booleanAxis = booleanAxisList.getOrNull(i)
                            viewHolder.setIsVisible(booleanAxis != null)
                            booleanAxis?.let {
                                switchViewMap[it.key] = viewHolder
                                viewHolder.initView(booleanAxis) { value ->
                                    viewModel.updatePreviewFontAxis(booleanAxis.key, value)
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.previewingClockFontAxisMap.collect { axisMap ->
                        // This data flow updates when user configures the sliders and switches
                        // in the clock font content.
                        axisMap.forEach { (key, value) ->
                            sliderViewMap[key]?.setValue(value)
                            switchViewMap[key]?.setValue(value)
                        }
                    }
                }
            }
        }
    }

    private fun createClockStyleOptionItemAdapter(
        lifecycleOwner: LifecycleOwner
    ): OptionItemAdapter2<ClockStyleModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.clock_style_option,
            lifecycleOwner = lifecycleOwner,
            bindPayload = { view: View, styleModel: ClockStyleModel ->
                view
                    .findViewById<ImageView>(R.id.foreground)
                    ?.setImageDrawable(styleModel.thumbnail)
                val job =
                    lifecycleOwner.lifecycleScope.launch {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            styleModel.showEditButton.collect {
                                view.findViewById<ImageView>(R.id.edit_icon)?.isVisible = it
                            }
                        }
                    }
                return@OptionItemAdapter2 DisposableHandle { job.cancel() }
            },
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
        lifecycleOwner: LifecycleOwner,
    ): OptionItemAdapter<ColorOptionIconViewModel> =
        OptionItemAdapter(
            layoutResourceId = R.layout.color_option,
            lifecycleOwner = lifecycleOwner,
            bindIcon = { foregroundView: View, colorIcon: ColorOptionIconViewModel ->
                val colorOptionIconView = foregroundView as? ColorOptionIconView
                val night =
                    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                colorOptionIconView?.let { ColorOptionIconBinder.bind(it, colorIcon, night) }
            },
        )

    private fun RecyclerView.initColorList(
        context: Context,
        adapter: OptionItemAdapter<ColorOptionIconViewModel>,
    ) {
        apply {
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
    }

    // Alpha is 1 when current height is from height, and 0 when current height is to height.
    private fun getAlpha(fromHeight: Int, toHeight: Int, currentHeight: Int): Float =
        (1 - (currentHeight - fromHeight).toFloat() / (toHeight - fromHeight).toFloat())
}
