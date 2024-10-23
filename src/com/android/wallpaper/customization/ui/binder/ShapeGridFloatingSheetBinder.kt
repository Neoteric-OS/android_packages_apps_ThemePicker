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
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.grid.ui.binder.GridIconViewBinder
import com.android.customization.picker.grid.ui.viewmodel.GridIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_SHAPE_GRID
import com.android.wallpaper.customization.ui.viewmodel.ShapeGridFloatingSheetHeightsViewModel
import com.android.wallpaper.customization.ui.viewmodel.ShapeGridPickerViewModel.Tab.GRID
import com.android.wallpaper.customization.ui.viewmodel.ShapeGridPickerViewModel.Tab.SHAPE
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import com.android.wallpaper.picker.option.ui.binder.OptionItemBinder
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object ShapeGridFloatingSheetBinder {
    private const val ANIMATION_DURATION = 200L

    private val _shapeGridFloatingSheetHeights:
        MutableStateFlow<ShapeGridFloatingSheetHeightsViewModel?> =
        MutableStateFlow(null)
    private val shapeGridFloatingSheetHeights: Flow<ShapeGridFloatingSheetHeightsViewModel> =
        _shapeGridFloatingSheetHeights.asStateFlow().filterNotNull().filter {
            it.shapeContentHeight != null && it.gridContentHeight != null
        }

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val floatingSheetContentVerticalPadding =
            view.resources.getDimensionPixelSize(R.dimen.floating_sheet_content_vertical_padding)
        val viewModel = optionsViewModel.shapeGridPickerViewModel

        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
        val tabAdapter =
            FloatingToolbarTabAdapter(
                    colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                    shouldAnimateColor = { optionsViewModel.selectedOption.value == APP_SHAPE_GRID },
                )
                .also { tabs.setAdapter(it) }
        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.shape_grid_floating_sheet_content_container)

        val shapeContent = view.requireViewById<View>(R.id.app_shape_container)

        val gridContent = view.requireViewById<View>(R.id.app_grid_container)
        val adapter = createOptionItemAdapter(view.context, lifecycleOwner, backgroundDispatcher)
        val gridOptionList =
            view.requireViewById<RecyclerView>(R.id.grid_options).also {
                it.initGridOptionList(view.context, adapter)
            }

        // Get the shape content height when it is ready
        shapeContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    _shapeGridFloatingSheetHeights.value =
                        _shapeGridFloatingSheetHeights.value?.copy(
                            shapeContentHeight = shapeContent.height
                        )
                            ?: ShapeGridFloatingSheetHeightsViewModel(
                                shapeContentHeight = shapeContent.height
                            )
                    shapeContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
        // Get the grid content height when it is ready
        gridContent.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Make sure the recycler view height is the same as its parent. It's possible
                    // that the recycler view is shorter than expected.
                    gridOptionList.layoutParams =
                        gridOptionList.layoutParams.apply { height = gridContent.height }
                    _shapeGridFloatingSheetHeights.value =
                        _shapeGridFloatingSheetHeights.value?.copy(
                            gridContentHeight = gridContent.height
                        )
                            ?: ShapeGridFloatingSheetHeightsViewModel(
                                gridContentHeight = shapeContent.height
                            )
                    shapeContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect { tabAdapter.submitList(it) } }

                launch {
                    combine(shapeGridFloatingSheetHeights, viewModel.selectedTab) {
                            heights,
                            selectedTab ->
                            heights to selectedTab
                        }
                        .collect { (heights, selectedTab) ->
                            val (shapeContentHeight, gridContentHeight) = heights
                            shapeContentHeight ?: return@collect
                            gridContentHeight ?: return@collect
                            val targetHeight =
                                when (selectedTab) {
                                    SHAPE -> shapeContentHeight
                                    GRID -> gridContentHeight
                                } + floatingSheetContentVerticalPadding * 2

                            ValueAnimator.ofInt(floatingSheetContainer.height, targetHeight)
                                .apply {
                                    addUpdateListener { valueAnimator ->
                                        val value = valueAnimator.animatedValue as Int
                                        floatingSheetContainer.layoutParams =
                                            floatingSheetContainer.layoutParams.apply {
                                                height = value
                                            }
                                    }
                                    duration = ANIMATION_DURATION
                                }
                                .start()

                            shapeContent.isVisible = selectedTab == SHAPE
                            gridContent.isVisible = selectedTab == GRID
                        }
                }

                launch {
                    viewModel.optionItems.collect { options ->
                        adapter.setItems(options) {
                            val indexToFocus =
                                options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (gridOptionList.layoutManager as LinearLayoutManager).scrollToPosition(
                                indexToFocus
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createOptionItemAdapter(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter<GridIconViewModel> =
        OptionItemAdapter(
            layoutResourceId = R.layout.grid_option,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            foregroundTintSpec =
                OptionItemBinder.TintSpec(
                    selectedColor =
                        context.getColor(com.android.wallpaper.R.color.system_on_surface),
                    unselectedColor =
                        context.getColor(com.android.wallpaper.R.color.system_on_surface),
                ),
            bindIcon = { foregroundView: View, gridIcon: GridIconViewModel ->
                val imageView = foregroundView as? ImageView
                imageView?.let { GridIconViewBinder.bind(imageView, gridIcon) }
            },
        )

    private fun RecyclerView.initGridOptionList(
        context: Context,
        adapter: OptionItemAdapter<GridIconViewModel>,
    ) {
        apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                SingleRowListItemSpacing(
                    edgeItemSpacePx =
                        context.resources.getDimensionPixelSize(
                            com.android.themepicker.R.dimen
                                .floating_sheet_content_horizontal_padding
                        ),
                    itemHorizontalSpacePx =
                        context.resources.getDimensionPixelSize(
                            com.android.themepicker.R.dimen
                                .floating_sheet_list_item_horizontal_space
                        ),
                )
            )
            this.adapter = adapter
        }
    }
}
