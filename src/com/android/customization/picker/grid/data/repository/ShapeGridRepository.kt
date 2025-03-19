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

package com.android.customization.picker.grid.data.repository

import android.graphics.drawable.Drawable
import com.android.customization.model.grid.GridOptionModel
import com.android.customization.model.grid.ShapeGridManager
import com.android.customization.model.grid.ShapeOptionModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
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
class ShapeGridRepository
@Inject
constructor(
    private val manager: ShapeGridManager,
    @BackgroundDispatcher private val bgScope: CoroutineScope,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {

    private val _shapeOptions = MutableStateFlow<List<ShapeOptionModel>?>(null)
    private val _gridOptions = MutableStateFlow<List<GridOptionModel>?>(null)

    init {
        bgScope.launch {
            _gridOptions.value = manager.getGridOptions()
            _shapeOptions.value = manager.getShapeOptions()
        }
    }

    val shapeOptions: StateFlow<List<ShapeOptionModel>?> = _shapeOptions.asStateFlow()

    val selectedShapeOption: Flow<ShapeOptionModel?> =
        shapeOptions.map { shapeOptions -> shapeOptions?.firstOrNull { it.isCurrent } }

    val gridOptions: StateFlow<List<GridOptionModel>?> = _gridOptions.asStateFlow()

    val selectedGridOption: Flow<GridOptionModel?> =
        gridOptions.map { gridOptions -> gridOptions?.firstOrNull { it.isCurrent } }

    suspend fun applySelectedOption(shapeKey: String, gridKey: String) =
        withContext(bgDispatcher) {
            manager.applyShapeGridOption(shapeKey, gridKey)
            // After applying, we should query and update shape and grid options again.
            _gridOptions.value = manager.getGridOptions()
            _shapeOptions.value = manager.getShapeOptions()
        }

    fun getGridOptionDrawable(iconId: Int): Drawable? {
        return manager.getGridOptionDrawable(iconId)
    }
}
