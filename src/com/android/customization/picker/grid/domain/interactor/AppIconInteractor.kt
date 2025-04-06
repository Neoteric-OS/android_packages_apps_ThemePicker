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

package com.android.customization.picker.grid.domain.interactor

import com.android.customization.picker.grid.data.repository.ShapeRepository
import com.android.customization.picker.themedicon.data.repository.ThemedIconRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AppIconInteractor
@Inject
constructor(
    private val shapeRepository: ShapeRepository,
    private val themedIconRepository: ThemedIconRepository,
) {

    val shapeOptions = shapeRepository.shapeOptions

    val selectedShapeOption = shapeRepository.selectedShapeOption

    val isThemedIconAvailable: Flow<Boolean> = themedIconRepository.isAvailable

    val isThemedIconEnabled: Flow<Boolean> = themedIconRepository.isActivated

    suspend fun applyThemedIconEnabled(enabled: Boolean) =
        themedIconRepository.setThemedIconEnabled(enabled)

    suspend fun applyShape(shapeKey: String) = shapeRepository.applyShape(shapeKey)
}
