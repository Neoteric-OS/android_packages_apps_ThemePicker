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

package com.android.customization.picker.grid.domain.interactor

import android.graphics.drawable.Drawable
import com.android.customization.picker.grid.data.repository.ShapeGridRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShapeGridInteractor @Inject constructor(private val repository: ShapeGridRepository) {

    val shapeOptions = repository.shapeOptions

    val selectedShapeOption = repository.selectedShapeOption

    val gridOptions = repository.gridOptions

    val selectedGridOption = repository.selectedGridOption

    suspend fun applySelectedOption(shapeKey: String, gridKey: String) =
        repository.applySelectedOption(shapeKey, gridKey)

    fun getGridOptionDrawable(iconId: Int): Drawable? = repository.getGridOptionDrawable(iconId)
}
