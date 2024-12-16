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
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.customization.ui.viewmodel.ToolbarHeightsViewModel
import com.android.wallpaper.picker.customization.ui.binder.DefaultToolbarBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Singleton
class ThemePickerToolbarBinder
@Inject
constructor(private val defaultToolbarBinder: DefaultToolbarBinder) : ToolbarBinder {

    private val _toolbarHeights: MutableStateFlow<ToolbarHeightsViewModel?> = MutableStateFlow(null)
    private val toolbarHeights = _toolbarHeights.asStateFlow().filterNotNull()

    override fun bind(
        navButton: FrameLayout,
        toolbar: Toolbar,
        applyButton: Button,
        viewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        onNavBack: () -> Unit,
    ) {
        defaultToolbarBinder.bind(
            navButton,
            toolbar,
            applyButton,
            viewModel,
            lifecycleOwner,
            onNavBack,
        )

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        navButton.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (navButton.height != 0) {
                        _toolbarHeights.value =
                            _toolbarHeights.value?.copy(navButtonHeight = navButton.height)
                                ?: ToolbarHeightsViewModel(navButtonHeight = navButton.height)
                    }
                    navButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        toolbar.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (toolbar.height != 0) {
                        _toolbarHeights.value =
                            _toolbarHeights.value?.copy(toolbarHeight = toolbar.height)
                                ?: ToolbarHeightsViewModel(toolbarHeight = toolbar.height)
                    }
                    navButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        applyButton.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (applyButton.height != 0) {
                        _toolbarHeights.value =
                            _toolbarHeights.value?.copy(applyButtonHeight = applyButton.height)
                                ?: ToolbarHeightsViewModel(applyButtonHeight = applyButton.height)
                    }
                    applyButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        val applyButtonTextColorEnabled =
            applyButton.resources.getColor(R.color.system_on_primary, null)
        val applyButtonTextColorDisabled =
            ColorUtils.setAlphaComponent(
                applyButton.resources.getColor(R.color.system_on_surface, null),
                97,
            ) // 97 for 38% transparent

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onApplyButtonClicked.collect { onApplyButtonClicked ->
                        applyButton.setOnClickListener { onApplyButtonClicked?.invoke(onNavBack) }
                    }
                }

                launch { viewModel.isApplyButtonVisible.collect { applyButton.isInvisible = !it } }

                launch {
                    viewModel.isApplyButtonEnabled.collect {
                        applyButton.isEnabled = it
                        applyButton.background.alpha =
                            if (it) 255 else 31 // 255 for 100%, 31 for 12% transparent,
                        applyButton.setTextColor(
                            if (it) applyButtonTextColorEnabled else applyButtonTextColorDisabled
                        )
                    }
                }

                launch {
                    combine(toolbarHeights, viewModel.isToolbarCollapsed, ::Pair).collect {
                        (toolbarHeights, isToolbarCollapsed) ->
                        val (navButtonHeight, toolbarHeight, applyButtonHeight) = toolbarHeights
                        navButtonHeight ?: return@collect
                        toolbarHeight ?: return@collect
                        applyButtonHeight ?: return@collect

                        val navButtonToHeight = if (isToolbarCollapsed) 0 else navButtonHeight
                        val toolbarToHeight = if (isToolbarCollapsed) 0 else toolbarHeight
                        val applyButtonToHeight = if (isToolbarCollapsed) 0 else applyButtonHeight
                        ValueAnimator.ofInt(navButton.height, navButtonToHeight)
                            .apply {
                                addUpdateListener { valueAnimator ->
                                    val value = valueAnimator.animatedValue as Int
                                    navButton.layoutParams =
                                        navButton.layoutParams.apply { height = value }
                                }
                                duration = ANIMATION_DURATION
                            }
                            .start()

                        ValueAnimator.ofInt(toolbar.height, toolbarToHeight)
                            .apply {
                                addUpdateListener { valueAnimator ->
                                    val value = valueAnimator.animatedValue as Int
                                    toolbar.layoutParams =
                                        toolbar.layoutParams.apply { height = value }
                                }
                                duration = ANIMATION_DURATION
                            }
                            .start()

                        ValueAnimator.ofInt(applyButton.height, applyButtonToHeight)
                            .apply {
                                addUpdateListener { valueAnimator ->
                                    val value = valueAnimator.animatedValue as Int
                                    applyButton.layoutParams =
                                        applyButton.layoutParams.apply { height = value }
                                }
                                duration = ANIMATION_DURATION
                            }
                            .start()
                    }
                }
            }
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
    }
}
