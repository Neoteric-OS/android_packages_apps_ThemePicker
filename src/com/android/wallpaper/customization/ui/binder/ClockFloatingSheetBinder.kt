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

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.doOnLayout
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
import com.android.systemui.plugins.clocks.ClockFontAxis
import com.android.systemui.plugins.clocks.ClockId
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.viewmodel.ClockFloatingSheetHeightsViewModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.ClockStyleModel
import com.android.wallpaper.customization.ui.viewmodel.ClockPickerViewModel.Tab
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object ClockFloatingSheetBinder {
    private const val SLIDER_ENABLED_ALPHA = 1f
    private const val SLIDER_DISABLED_ALPHA = .3f
    private const val ANIMATION_DURATION = 200L

    private val _clockFloatingSheetHeights: MutableStateFlow<ClockFloatingSheetHeightsViewModel?> =
        MutableStateFlow(null)
    private val clockFloatingSheetHeights: Flow<ClockFloatingSheetHeightsViewModel?> =
        _clockFloatingSheetHeights.asStateFlow()

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val viewModel = optionsViewModel.clockPickerViewModel

        val appContext = view.context.applicationContext

        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
        val tabAdapter =
            FloatingToolbarTabAdapter(
                    colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                    shouldAnimateColor = { optionsViewModel.selectedOption.value == CLOCK },
                )
                .also { tabs.setAdapter(it) }

        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.clock_floating_sheet_content_container)

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
            viewModel.revertFontAxes()
        }
        clockFontToolbar.requireViewById<View>(R.id.clock_font_apply).setOnClickListener {
            viewModel.applyFontAxes()
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

        view.doOnLayout {
            if (_clockFloatingSheetHeights.value == null) {
                _clockFloatingSheetHeights.value =
                    ClockFloatingSheetHeightsViewModel(
                        clockStyleContentHeight = clockStyleContent.height,
                        clockColorContentHeight = clockColorContent.height,
                        clockFontContentHeight = clockFontContent.height,
                    )
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect { tabAdapter.submitList(it) } }

                launch {
                    combine(clockFloatingSheetHeights, viewModel.selectedTab, ::Pair).collect {
                        (heights, selectedTab) ->
                        heights ?: return@collect

                        val targetHeight =
                            when (selectedTab) {
                                Tab.STYLE -> heights.clockStyleContentHeight
                                Tab.COLOR -> heights.clockColorContentHeight
                                Tab.FONT -> heights.clockFontContentHeight
                            } +
                                view.resources.getDimensionPixelSize(
                                    R.dimen.floating_sheet_content_vertical_padding
                                ) * 2

                        val animationFloatingSheet =
                            ValueAnimator.ofInt(floatingSheetContainer.height, targetHeight)
                        animationFloatingSheet.addUpdateListener { valueAnimator ->
                            val value = valueAnimator.animatedValue as Int
                            floatingSheetContainer.layoutParams =
                                floatingSheetContainer.layoutParams.apply { height = value }
                        }
                        animationFloatingSheet.setDuration(ANIMATION_DURATION)
                        animationFloatingSheet.start()

                        clockStyleContent.isVisible = selectedTab == Tab.STYLE
                        clockColorContent.isVisible = selectedTab == Tab.COLOR
                        clockFontContent.isVisible = selectedTab == Tab.FONT

                        tabs.isVisible = selectedTab != Tab.FONT
                        clockFontToolbar.isVisible = selectedTab == Tab.FONT
                    }
                }

                launch {
                    var boundClockId: ClockId? = null
                    var boundEditorViews = mapOf<String, Pair<View, ClockFontAxis>>()
                    combine(viewModel.previewingClock, viewModel.previewingFontAxisMap, ::Pair)
                        .collect { pair ->
                            val (clock, fontAxisMap) = pair
                            if (clock == null) {
                                boundClockId = null
                                boundEditorViews = mapOf()
                                clockFontContent.removeAllViews()
                                return@collect
                            }

                            if (boundClockId != clock.clockId) {
                                boundEditorViews =
                                    initClockFontEditor(clockFontContent, clock.fontAxes, viewModel)
                                boundClockId = clock.clockId
                            }

                            for ((key, value) in fontAxisMap) {
                                boundEditorViews[key]?.let { pair ->
                                    val (view, axis) = pair
                                    view.findViewById<Switch>(R.id.clock_axis_switch)?.apply {
                                        isChecked = abs(value - axis.maxValue) < 0.01f
                                    }
                                    view.findViewById<SeekBar>(R.id.clock_axis_slider)?.apply {
                                        setProgress(value.toInt(), false)
                                    }
                                }
                            }
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
    }

    private fun initClockFontEditor(
        parent: ViewGroup,
        fontAxes: List<ClockFontAxis>,
        viewModel: ClockPickerViewModel,
    ): Map<String, Pair<View, ClockFontAxis>> {
        parent.removeAllViews()
        val inflater = LayoutInflater.from(parent.context)
        val axisMap = mutableMapOf<String, Pair<View, ClockFontAxis>>()
        var nextSwitch: View? = null
        for (axis in fontAxes) {
            val view =
                when (axis.type) {
                    AxisType.Float -> {
                        val id = R.layout.clock_font_axis_slider_row
                        val row = inflater.inflate(id, parent, false)
                        parent.addView(row)
                        row
                    }
                    AxisType.Boolean ->
                        nextSwitch?.also { nextSwitch = null }
                            ?: run {
                                val id = R.layout.clock_font_axis_switch_row
                                val row = inflater.inflate(id, parent, false)
                                parent.addView(row)

                                nextSwitch = row.requireViewById(R.id.clock_switch_two)
                                row.requireViewById(R.id.clock_switch_one)
                            }
                }

            view.visibility = View.VISIBLE
            axisMap[axis.key] = Pair(view, axis)
            view.contentDescription = axis.description
            view.requireViewById<TextView>(R.id.clock_axis_name).text = axis.name

            view.findViewById<Switch>(R.id.clock_axis_switch)?.apply {
                isChecked = abs(axis.currentValue - axis.maxValue) < 0.01f
                setOnCheckedChangeListener { v, _ ->
                    val value = if (v.isChecked) axis.maxValue else axis.minValue
                    viewModel.updatePreviewFontAxis(axis.key, value)
                }
            }

            view.findViewById<SeekBar>(R.id.clock_axis_slider)?.apply {
                setMax(axis.maxValue.toInt())
                setMin(axis.minValue.toInt())
                setProgress(axis.currentValue.toInt(), false)

                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean,
                        ) {
                            if (fromUser) {
                                viewModel.updatePreviewFontAxis(axis.key, progress.toFloat())
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    }
                )
            }
        }
        return axisMap
    }

    private fun createClockStyleOptionItemAdapter(
        lifecycleOwner: LifecycleOwner
    ): OptionItemAdapter<ClockStyleModel> =
        OptionItemAdapter(
            layoutResourceId = R.layout.clock_style_option,
            lifecycleOwner = lifecycleOwner,
            bindIcon = { view: View, style: ClockStyleModel ->
                (view.findViewById(R.id.clock_icon) as ImageView).setImageDrawable(style.thumbnail)
                (view.findViewById(R.id.edit_icon) as ImageView).setVisibility(
                    // TODO(b/364673969): Route isSelected here and hide when unselected
                    if (style.isEditable) View.VISIBLE else View.GONE
                )
            },
        )

    private fun RecyclerView.initStyleList(
        context: Context,
        adapter: OptionItemAdapter<ClockStyleModel>,
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
}
