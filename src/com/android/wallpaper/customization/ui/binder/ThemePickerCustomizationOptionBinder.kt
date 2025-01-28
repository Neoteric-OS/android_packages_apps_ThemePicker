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

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView.Companion.addClockViews
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder2
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.grid.ui.binder.GridIconViewBinder
import com.android.customization.picker.settings.ui.binder.ColorContrastSectionViewBinder2
import com.android.systemui.plugins.clocks.ClockFontAxisSetting
import com.android.systemui.plugins.clocks.ClockPreviewConfig
import com.android.systemui.shared.Flags
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.google.android.material.materialswitch.MaterialSwitch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    override fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
    ) {
        defaultCustomizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToWallpaperCategoriesScreen,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
        )

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel

        val isOnMainScreen = { optionsViewModel.selectedOption.value == null }

        val allCustomizationOptionEntries =
            lockScreenCustomizationOptionEntries + homeScreenCustomizationOptionEntries
        allCustomizationOptionEntries.forEach { (_, view) ->
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(view.background), color)
                },
                color = colorUpdateViewModel.colorSurfaceBright,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view
                        .findViewById<ViewGroup>(R.id.option_entry_icon_container)
                        ?.background
                        ?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
                },
                color = colorUpdateViewModel.colorSurfaceContainerHigh,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_title)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_description)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurfaceVariant,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
        }

        val optionClock: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.CLOCK }
                .second
        val optionClockIcon: ImageView = optionClock.requireViewById(R.id.option_entry_icon)

        val optionShortcut: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.SHORTCUTS }
                .second
        val optionShortcutDescription: TextView =
            optionShortcut.requireViewById(R.id.option_entry_description)
        val optionShortcutIcon1: ImageView =
            optionShortcut.requireViewById(R.id.option_entry_icon_1)
        val optionShortcutIcon2: ImageView =
            optionShortcut.requireViewById(R.id.option_entry_icon_2)

        val optionLockScreenNotificationsSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.LOCK_SCREEN_NOTIFICATIONS }
                .second
        optionLockScreenNotificationsSettings.setOnClickListener {
            navigateToLockScreenNotificationsSettingsActivity.invoke()
        }

        val optionMoreLockScreenSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS }
                .second
        optionMoreLockScreenSettings.setOnClickListener {
            navigateToMoreLockScreenSettingsActivity.invoke()
        }

        val optionColors: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLORS }
                .second
        val optionColorsIcon: ColorOptionIconView2 =
            optionColors.requireViewById(R.id.option_entry_icon)

        val optionShapeGrid: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.APP_SHAPE_GRID }
                .second
        val optionShapeGridDescription: TextView =
            optionShapeGrid.requireViewById(R.id.option_entry_description)
        val optionShapeGridIcon: ImageView = optionShapeGrid.requireViewById(R.id.option_entry_icon)

        val optionColorContrast: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLOR_CONTRAST }
                .second
        optionColorContrast.setOnClickListener { navigateToColorContrastSettingsActivity.invoke() }

        val optionThemedIcons =
            homeScreenCustomizationOptionEntries
                .find { it.first == ThemePickerHomeCustomizationOption.THEMED_ICONS }
                ?.second
        val optionThemedIconsSwitch =
            optionThemedIcons?.findViewById<MaterialSwitch>(R.id.option_entry_switch)

        ColorUpdateBinder.bind(
            setColor = { color ->
                optionClockIcon.setColorFilter(color)
                optionShortcutIcon1.setColorFilter(color)
                optionShortcutIcon2.setColorFilter(color)
                optionShapeGridIcon.setColorFilter(color)
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.onCustomizeClockClicked.collect {
                        optionClock.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.clockPickerViewModel.selectedClock.collect {
                        optionClockIcon.setImageDrawable(it.thumbnail)
                    }
                }

                launch {
                    optionsViewModel.onCustomizeShortcutClicked.collect {
                        optionShortcut.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.keyguardQuickAffordancePickerViewModel2.summary.collect {
                        summary ->
                        optionShortcutDescription.let {
                            TextViewBinder.bind(view = it, viewModel = summary.description)
                        }
                        summary.icon1?.let { icon ->
                            optionShortcutIcon1.let {
                                IconViewBinder.bind(view = it, viewModel = icon)
                            }
                        }
                        optionShortcutIcon1.isVisible = summary.icon1 != null

                        summary.icon2?.let { icon ->
                            optionShortcutIcon2.let {
                                IconViewBinder.bind(view = it, viewModel = icon)
                            }
                        }
                        optionShortcutIcon2.isVisible = summary.icon2 != null
                    }
                }

                launch {
                    optionsViewModel.onCustomizeColorsClicked.collect {
                        optionColors.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeShapeGridClicked.collect {
                        optionShapeGrid.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.shapeGridPickerViewModel.selectedGridOption.collect {
                        gridOption ->
                        TextViewBinder.bind(optionShapeGridDescription, gridOption.text)
                        gridOption.payload?.let { gridIconViewModel ->
                            GridIconViewBinder.bind(
                                view = optionShapeGridIcon,
                                viewModel = gridIconViewModel,
                            )
                        }
                    }
                }

                launch {
                    var binding: ColorContrastSectionViewBinder2.Binding? = null
                    optionsViewModel.colorContrastSectionViewModel.contrast.collectLatest { contrast
                        ->
                        binding?.destroy()
                        binding =
                            ColorContrastSectionViewBinder2.bind(
                                view = optionColorContrast,
                                contrast = contrast,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isOnMainScreen,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    var binding: ColorOptionIconBinder2.Binding? = null
                    optionsViewModel.colorPickerViewModel2.selectedColorOption.collect { colorOption
                        ->
                        (colorOption as? ColorOptionImpl)?.let {
                            binding?.destroy()
                            binding =
                                ColorOptionIconBinder2.bind(
                                    view = optionColorsIcon,
                                    viewModel =
                                        ColorOptionIconViewModel.fromColorOption(colorOption),
                                    darkTheme = view.resources.configuration.isNightModeActive,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isOnMainScreen,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }
                }

                if (optionThemedIconsSwitch != null) {
                    launch {
                        optionsViewModel.themedIconViewModel.isAvailable.collect { isAvailable ->
                            optionThemedIconsSwitch.isEnabled = isAvailable
                        }
                    }

                    launch {
                        optionsViewModel.themedIconViewModel.isActivated.collect {
                            optionThemedIconsSwitch.isChecked = it
                        }
                    }

                    launch {
                        optionsViewModel.themedIconViewModel.toggleThemedIcon.collect {
                            optionThemedIconsSwitch.setOnCheckedChangeListener { _, _ ->
                                launch { it.invoke() }
                            }
                        }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
            ?.let {
                ShortcutFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.COLORS)
            ?.let {
                ColorsFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.APP_SHAPE_GRID)
            ?.let {
                ShapeGridFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                    Dispatchers.IO,
                )
            }
    }

    override fun bindClockPreview(
        context: Context,
        clockHostView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        clockHostView as ClockConstraintLayoutHostView
        val clockPickerViewModel =
            (viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel)
                .clockPickerViewModel

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockSize,
                        ) { clock, size ->
                            clock to size
                        }
                        .collect { (clock, size) ->
                            clockHostView.removeAllViews()
                            // For new customization picker, we should get views from clocklayout
                            if (Flags.newCustomizationPickerUi()) {
                                clockViewFactory.getController(clock.clockId).let { clockController
                                    ->
                                    val udfpsTop =
                                        clockPickerViewModel.getUdfpsLocation()?.let {
                                            it.centerY - it.radius
                                        }
                                    val previewConfig =
                                        ClockPreviewConfig(
                                            context = context,
                                            isShadeLayoutWide =
                                                clockPickerViewModel.getIsShadeLayoutWide(),
                                            isSceneContainerFlagEnabled = false,
                                            udfpsTop = udfpsTop,
                                        )
                                    addClockViews(clockController, clockHostView, size)
                                    val cs = ConstraintSet()
                                    clockController.largeClock.layout.applyPreviewConstraints(
                                        previewConfig,
                                        cs,
                                    )
                                    clockController.smallClock.layout.applyPreviewConstraints(
                                        previewConfig,
                                        cs,
                                    )
                                    cs.applyTo(clockHostView)
                                }
                            } else {
                                val clockView =
                                    when (size) {
                                        ClockSize.DYNAMIC ->
                                            clockViewFactory.getLargeView(clock.clockId)
                                        ClockSize.SMALL ->
                                            clockViewFactory.getSmallView(clock.clockId)
                                    }
                                // The clock view might still be attached to an existing parent.
                                // Detach
                                // before adding to another parent.
                                (clockView.parent as? ViewGroup)?.removeView(clockView)
                                clockHostView.addView(clockView)
                            }
                        }
                }

                launch {
                    combine(
                            clockPickerViewModel.previewingSeedColor,
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockFontAxisMap,
                            colorUpdateViewModel.systemColorsUpdated,
                            ::Quadruple,
                        )
                        .collect { quadruple ->
                            val (color, clock, axisMap, _) = quadruple
                            clockViewFactory.updateColor(clock.clockId, color)
                            val axisList = axisMap.map { ClockFontAxisSetting(it.key, it.value) }
                            clockViewFactory.updateFontAxes(clock.clockId, axisList)
                        }
                }
            }
        }
    }

    override fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    ) {
        defaultCustomizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel,
            lifecycleOwner,
            activity,
        )
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
