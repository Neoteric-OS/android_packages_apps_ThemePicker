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

import android.util.Log
import com.android.customization.model.CustomizationManager
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.color.ColorOption
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.picker.color.shared.model.ColorType
import com.android.wallpaper.picker.customization.data.repository.WallpaperColorsRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class ColorPickerRepositoryImpl2
@Inject
constructor(
    @BackgroundDispatcher private val scope: CoroutineScope,
    wallpaperColorsRepository: WallpaperColorsRepository,
    private val colorManager: ColorCustomizationManager,
) : ColorPickerRepository2 {

    private val homeWallpaperColors: StateFlow<WallpaperColorsModel?> =
        wallpaperColorsRepository.homeWallpaperColors
    private val lockWallpaperColors: StateFlow<WallpaperColorsModel?> =
        wallpaperColorsRepository.lockWallpaperColors

    override val colorOptions: Flow<Map<ColorType, List<ColorOption>>> =
        combine(homeWallpaperColors, lockWallpaperColors) { homeColors, lockColors ->
                homeColors to lockColors
            }
            .map { (homeColors, lockColors) ->
                suspendCancellableCoroutine { continuation ->
                    if (
                        homeColors is WallpaperColorsModel.Loading ||
                            lockColors is WallpaperColorsModel.Loading
                    ) {
                        continuation.resumeWith(
                            Result.success(
                                mapOf(
                                    ColorType.WALLPAPER_COLOR to listOf(),
                                    ColorType.PRESET_COLOR to listOf(),
                                )
                            )
                        )
                        return@suspendCancellableCoroutine
                    }
                    val homeColorsLoaded = homeColors as WallpaperColorsModel.Loaded
                    val lockColorsLoaded = lockColors as WallpaperColorsModel.Loaded
                    colorManager.setWallpaperColors(
                        homeColorsLoaded.colors,
                        lockColorsLoaded.colors,
                    )
                    colorManager.fetchOptions(
                        object : CustomizationManager.OptionsFetchedListener<ColorOption> {
                            override fun onOptionsLoaded(options: MutableList<ColorOption>?) {
                                val wallpaperColorOptions: MutableList<ColorOption> =
                                    mutableListOf()
                                val presetColorOptions: MutableList<ColorOption> = mutableListOf()
                                options?.forEach { option ->
                                    when ((option as ColorOptionImpl).type) {
                                        ColorType.WALLPAPER_COLOR ->
                                            wallpaperColorOptions.add(option)
                                        ColorType.PRESET_COLOR -> presetColorOptions.add(option)
                                    }
                                }
                                continuation.resumeWith(
                                    Result.success(
                                        mapOf(
                                            ColorType.WALLPAPER_COLOR to wallpaperColorOptions,
                                            ColorType.PRESET_COLOR to presetColorOptions,
                                        )
                                    )
                                )
                            }

                            override fun onError(throwable: Throwable?) {
                                Log.e(TAG, "Error loading theme bundles", throwable)
                                continuation.resumeWith(
                                    Result.failure(
                                        throwable ?: Throwable("Error loading theme bundles")
                                    )
                                )
                            }
                        },
                        /* reload= */ false,
                    )
                }
            }

    private val settingsChanged = callbackFlow {
        trySend(Unit)
        colorManager.setListener { trySend(Unit) }
        awaitClose { colorManager.setListener(null) }
    }

    override val selectedColorOption =
        combine(colorOptions, settingsChanged) { options, _ ->
                options.forEach { (_, optionsByType) ->
                    optionsByType.forEach {
                        if (it.isActive(colorManager)) {
                            return@combine it
                        }
                    }
                }
                return@combine null
            }
            .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(), initialValue = null)

    override suspend fun select(colorOption: ColorOption) {
        suspendCancellableCoroutine { continuation ->
            colorManager.apply(
                colorOption,
                object : CustomizationManager.Callback {
                    override fun onSuccess() {
                        continuation.resumeWith(Result.success(Unit))
                    }

                    override fun onError(throwable: Throwable?) {
                        Log.w(TAG, "Apply theme with error", throwable)
                        continuation.resumeWith(
                            Result.failure(throwable ?: Throwable("Error loading theme bundles"))
                        )
                    }
                },
            )
        }
    }

    companion object {
        private const val TAG = "ColorPickerRepositoryImpl"
    }
}
