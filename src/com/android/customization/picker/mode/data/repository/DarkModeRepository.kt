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

package com.android.customization.picker.mode.data.repository

import com.android.app.tracing.coroutines.flow.map
import com.android.customization.picker.mode.shared.util.DarkModeUtil
import com.android.wallpaper.system.PowerManagerWrapper
import com.android.wallpaper.system.UiModeManagerWrapper
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

@Singleton
class DarkModeRepository
@Inject
constructor(
    darkModeUtil: DarkModeUtil,
    private val uiModeManager: UiModeManagerWrapper,
    private val powerManager: PowerManagerWrapper,
) {
    private val isPowerSaveMode = MutableStateFlow(powerManager.getIsPowerSaveMode() ?: false)

    private val isAvailable = darkModeUtil.isAvailable()

    val isEnabled =
        if (isAvailable) {
            isPowerSaveMode.map { !it }
        } else flowOf(false)

    private val _isDarkMode = MutableStateFlow(uiModeManager.getIsNightModeActivated())
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkModeActivated(isActive: Boolean) {
        uiModeManager.setNightModeActivated(isActive)
        refreshIsDarkModeActivated()
    }

    fun refreshIsDarkModeActivated() {
        _isDarkMode.value = uiModeManager.getIsNightModeActivated()
    }

    fun refreshIsPowerSaveModeActivated() {
        powerManager.getIsPowerSaveMode()?.let { isPowerSaveMode.value = it }
    }
}
