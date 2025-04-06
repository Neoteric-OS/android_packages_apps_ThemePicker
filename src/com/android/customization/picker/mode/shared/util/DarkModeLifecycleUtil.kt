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

package com.android.customization.picker.mode.shared.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.text.TextUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.mode.data.repository.DarkModeRepository
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * This class observes the activity lifecycle and updates the DarkModeRepositoryImpl based on
 * lifecycle phases.
 */
@ActivityScoped
class DarkModeLifecycleUtil
@Inject
constructor(
    @ActivityContext private val activityContext: Context,
    private val darkModeRepository: DarkModeRepository,
) {
    private val lifecycleOwner = activityContext as LifecycleOwner

    private val batterySaverStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (
                    intent != null &&
                        TextUtils.equals(intent.action, PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                ) {
                    darkModeRepository.refreshIsPowerSaveModeActivated()
                }
            }
        }
    private val lifecycleObserver =
        object : DefaultLifecycleObserver {
            @Synchronized
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                darkModeRepository.refreshIsPowerSaveModeActivated()
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    activityContext.registerReceiver(
                        batterySaverStateReceiver,
                        IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                    )
                }
            }

            @Synchronized
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                activityContext.unregisterReceiver(batterySaverStateReceiver)
            }

            @Synchronized
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        }

    init {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }
}
