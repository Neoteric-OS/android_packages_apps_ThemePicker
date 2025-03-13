/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.customization.picker.grid.data.repository

import android.content.ContentValues
import android.content.Context
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.COL_SHAPE_KEY
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.SET_SHAPE
import com.android.customization.model.grid.ShapeGridManager
import com.android.customization.model.grid.ShapeOptionModel
import com.android.wallpaper.R
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ShapeRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val shapeGridManager: ShapeGridManager,
    @BackgroundDispatcher private val bgScope: CoroutineScope,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {
    private val authorityMetadataKey: String =
        context.getString(R.string.grid_control_metadata_name)
    private val previewUtils: PreviewUtils = PreviewUtils(context, authorityMetadataKey)

    private val _shapeOptions = MutableStateFlow<List<ShapeOptionModel>?>(null)

    init {
        bgScope.launch { _shapeOptions.value = shapeGridManager.getShapeOptions() }
    }

    val shapeOptions: StateFlow<List<ShapeOptionModel>?> = _shapeOptions.asStateFlow()

    val selectedShapeOption: Flow<ShapeOptionModel?> =
        shapeOptions.map { shapeOptions -> shapeOptions?.firstOrNull { it.isCurrent } }

    suspend fun applyShape(shapeKey: String) =
        withContext(bgDispatcher) {
            context.contentResolver.update(
                previewUtils.getUri(SET_SHAPE),
                ContentValues().apply { put(COL_SHAPE_KEY, shapeKey) },
                null,
                null,
            )
            // After applying, we should query and update shape and grid options again.
            _shapeOptions.value = shapeGridManager.getShapeOptions()
        }
}
