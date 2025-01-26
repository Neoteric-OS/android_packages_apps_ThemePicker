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

package com.android.customization.picker.themedicon.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import com.android.customization.module.CustomizationPreferences
import com.android.themepicker.R
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class ThemedIconRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val appContext: Context,
    private val contentResolver: ContentResolver,
    packageManager: PackageManager,
    @BackgroundDispatcher private val backgroundScope: CoroutineScope,
) : ThemedIconRepository {
    private val uri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    private var getUriJob: Job =
        backgroundScope.launch {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo =
                packageManager.resolveActivity(
                    homeIntent,
                    PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_META_DATA,
                )
            val providerAuthority =
                resolveInfo
                    ?.activityInfo
                    ?.metaData
                    ?.getString(appContext.getString(R.string.themed_icon_metadata_key))
            val providerInfo =
                providerAuthority?.let { authority ->
                    val info = packageManager.resolveContentProvider(authority, 0)
                    val hasPermission =
                        info?.readPermission?.let {
                            if (it.isNotEmpty()) {
                                appContext.checkSelfPermission(it) ==
                                    PackageManager.PERMISSION_GRANTED
                            } else true
                        } ?: true
                    if (!hasPermission) {
                        null
                    } else {
                        info
                    }
                }
            uri.value =
                providerInfo?.let {
                    Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(providerInfo.authority)
                        .appendPath(ICON_THEMED)
                        .build()
                }
        }

    override val isAvailable: Flow<Boolean> =
        uri.map { it != null }
            .stateIn(
                scope = backgroundScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = false,
            )

    override val isActivated: Flow<Boolean> =
        callbackFlow {
                var disposableHandle: DisposableHandle? = null
                launch {
                    uri.collect {
                        disposableHandle?.dispose()
                        if (it != null) {
                            val contentObserver =
                                object : ContentObserver(null) {
                                    override fun onChange(selfChange: Boolean) {
                                        trySend(getThemedIconEnabled(it))
                                    }
                                }
                            contentResolver.registerContentObserver(
                                it,
                                /* notifyForDescendants= */ true,
                                contentObserver,
                            )

                            trySend(getThemedIconEnabled(it))

                            disposableHandle = DisposableHandle {
                                contentResolver.unregisterContentObserver(contentObserver)
                            }
                        }
                    }
                }
                awaitClose { disposableHandle?.dispose() }
            }
            .stateIn(
                scope = backgroundScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = false,
            )

    private fun getThemedIconEnabled(uri: Uri): Boolean {
        val cursor =
            contentResolver.query(
                uri,
                /* projection= */ null,
                /* selection= */ null,
                /* selectionArgs= */ null,
                /* sortOrder= */ null,
            )
        var isEnabled = false
        if (cursor != null && cursor.moveToNext()) {
            isEnabled = (cursor.getInt(cursor.getColumnIndex(COL_ICON_THEMED_VALUE)) == ENABLED)
            val preferences =
                InjectorProvider.getInjector().getPreferences(appContext)
                    as CustomizationPreferences
            if (preferences.getThemedIconEnabled() != isEnabled) {
                preferences.setThemedIconEnabled(isEnabled)
            }
        }
        cursor?.close()
        return isEnabled
    }

    override suspend fun setThemedIconEnabled(enabled: Boolean) {
        getUriJob.join()
        uri.value?.let {
            val values = ContentValues()
            values.put(COL_ICON_THEMED_VALUE, enabled)
            contentResolver.update(it, values, /* where= */ null, /* selectionArgs= */ null)
        }
    }

    companion object {
        private const val ICON_THEMED = "icon_themed"
        private const val COL_ICON_THEMED_VALUE = "boolean_value"
        private const val ENABLED = 1
    }
}
