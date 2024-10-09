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
package com.android.customization.picker.color.data.util

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import android.util.SparseIntArray
import com.android.customization.model.ResourceConstants
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.Style
import com.android.systemui.shared.settings.data.repository.SecureSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONException
import org.json.JSONObject

/**
 * Extract material next colors from wallpaper colors. Based on Nexus Launcher's
 * MaterialColorsGenerator, nexuslauncher/widget/MaterialColorsGenerator.java
 */
@Singleton
class MaterialColorsGenerator
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val secureSettingsRepository: SecureSettingsRepository,
) {
    private fun addShades(shades: List<Int>, resources: IntArray, output: SparseIntArray) {
        if (shades.size != resources.size) {
            Log.e(TAG, "The number of shades computed doesn't match the number of resources.")
            return
        }
        for (i in resources.indices) {
            output.put(resources[i], 0xff000000.toInt() or shades[i])
        }
    }

    /**
     * Generates the mapping from system color resources to values from wallpaper colors.
     *
     * @return a list of color resource IDs and a corresponding list of their color values
     */
    suspend fun generate(colors: WallpaperColors): Pair<IntArray, IntArray> {
        val isDarkMode =
            (applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colorScheme = ColorScheme(colors, isDarkMode, fetchThemeStyleFromSetting())
        return generate(colorScheme)
    }

    /**
     * Generates the mapping from system color resources to values from color seed and style.
     *
     * @return a list of color resource IDs and a corresponding list of their color values
     */
    fun generate(colorSeed: Int, style: Style): Pair<IntArray, IntArray> {
        val isDarkMode =
            (applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colorScheme = ColorScheme(colorSeed, isDarkMode, style)
        return generate(colorScheme)
    }

    private fun generate(colorScheme: ColorScheme): Pair<IntArray, IntArray> {
        val allNeutralColors: MutableList<Int> = ArrayList()
        allNeutralColors.addAll(colorScheme.neutral1.allShades)
        allNeutralColors.addAll(colorScheme.neutral2.allShades)

        val allAccentColors: MutableList<Int> = ArrayList()
        allAccentColors.addAll(colorScheme.accent1.allShades)
        allAccentColors.addAll(colorScheme.accent2.allShades)
        allAccentColors.addAll(colorScheme.accent3.allShades)

        return Pair(
            NEUTRAL_RESOURCES + ACCENT_RESOURCES,
            (allNeutralColors + allAccentColors).toIntArray(),
        )
    }

    private suspend fun fetchThemeStyleFromSetting(): Style {
        val overlayPackageJson =
            secureSettingsRepository.getString(Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES)
        return if (!overlayPackageJson.isNullOrEmpty()) {
            try {
                val jsonObject = JSONObject(overlayPackageJson)
                Style.valueOf(jsonObject.getString(ResourceConstants.OVERLAY_CATEGORY_THEME_STYLE))
            } catch (e: (JSONException)) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                Style.TONAL_SPOT
            } catch (e: IllegalArgumentException) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                Style.TONAL_SPOT
            }
        } else {
            Style.TONAL_SPOT
        }
    }

    companion object {
        private const val TAG = "MaterialColorsGenerator"

        private val ACCENT_RESOURCES =
            intArrayOf(
                android.R.color.system_accent1_0,
                android.R.color.system_accent1_10,
                android.R.color.system_accent1_50,
                android.R.color.system_accent1_100,
                android.R.color.system_accent1_200,
                android.R.color.system_accent1_300,
                android.R.color.system_accent1_400,
                android.R.color.system_accent1_500,
                android.R.color.system_accent1_600,
                android.R.color.system_accent1_700,
                android.R.color.system_accent1_800,
                android.R.color.system_accent1_900,
                android.R.color.system_accent1_1000,
                android.R.color.system_accent2_0,
                android.R.color.system_accent2_10,
                android.R.color.system_accent2_50,
                android.R.color.system_accent2_100,
                android.R.color.system_accent2_200,
                android.R.color.system_accent2_300,
                android.R.color.system_accent2_400,
                android.R.color.system_accent2_500,
                android.R.color.system_accent2_600,
                android.R.color.system_accent2_700,
                android.R.color.system_accent2_800,
                android.R.color.system_accent2_900,
                android.R.color.system_accent2_1000,
                android.R.color.system_accent3_0,
                android.R.color.system_accent3_10,
                android.R.color.system_accent3_50,
                android.R.color.system_accent3_100,
                android.R.color.system_accent3_200,
                android.R.color.system_accent3_300,
                android.R.color.system_accent3_400,
                android.R.color.system_accent3_500,
                android.R.color.system_accent3_600,
                android.R.color.system_accent3_700,
                android.R.color.system_accent3_800,
                android.R.color.system_accent3_900,
                android.R.color.system_accent3_1000,
            )
        private val NEUTRAL_RESOURCES =
            intArrayOf(
                android.R.color.system_neutral1_0,
                android.R.color.system_neutral1_10,
                android.R.color.system_neutral1_50,
                android.R.color.system_neutral1_100,
                android.R.color.system_neutral1_200,
                android.R.color.system_neutral1_300,
                android.R.color.system_neutral1_400,
                android.R.color.system_neutral1_500,
                android.R.color.system_neutral1_600,
                android.R.color.system_neutral1_700,
                android.R.color.system_neutral1_800,
                android.R.color.system_neutral1_900,
                android.R.color.system_neutral1_1000,
                android.R.color.system_neutral2_0,
                android.R.color.system_neutral2_10,
                android.R.color.system_neutral2_50,
                android.R.color.system_neutral2_100,
                android.R.color.system_neutral2_200,
                android.R.color.system_neutral2_300,
                android.R.color.system_neutral2_400,
                android.R.color.system_neutral2_500,
                android.R.color.system_neutral2_600,
                android.R.color.system_neutral2_700,
                android.R.color.system_neutral2_800,
                android.R.color.system_neutral2_900,
                android.R.color.system_neutral2_1000,
            )
    }
}
