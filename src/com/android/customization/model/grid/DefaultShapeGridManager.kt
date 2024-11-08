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

package com.android.customization.model.grid

import android.content.ContentValues
import android.content.Context
import com.android.wallpaper.R
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultShapeGridManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) : ShapeGridManager {

    private val authorityMetadataKey: String =
        context.getString(R.string.grid_control_metadata_name)
    private val previewUtils: PreviewUtils = PreviewUtils(context, authorityMetadataKey)

    override suspend fun getGridOptions(): List<GridOptionModel>? =
        withContext(bgDispatcher) {
            if (previewUtils.supportsPreview()) {
                context.contentResolver
                    .query(previewUtils.getUri(GRID_OPTIONS), null, null, null, null)
                    ?.use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                val rows = cursor.getInt(cursor.getColumnIndex(COL_ROWS))
                                val cols = cursor.getInt(cursor.getColumnIndex(COL_COLS))
                                add(
                                    GridOptionModel(
                                        key = cursor.getString(cursor.getColumnIndex(COL_GRID_KEY)),
                                        title =
                                            context.getString(
                                                com.android.themepicker.R.string.grid_title_pattern,
                                                cols,
                                                rows,
                                            ),
                                        isCurrent =
                                            cursor
                                                .getString(cursor.getColumnIndex(COL_IS_DEFAULT))
                                                .toBoolean(),
                                        rows = rows,
                                        cols = cols,
                                    )
                                )
                            }
                        }
                    }
            } else {
                null
            }
        }

    override suspend fun getShapeOptions(): List<ShapeOptionModel>? =
        withContext(bgDispatcher) {
            if (previewUtils.supportsPreview()) {
                context.contentResolver
                    .query(previewUtils.getUri(SHAPE_OPTIONS), null, null, null, null)
                    ?.use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(
                                    ShapeOptionModel(
                                        key =
                                            cursor.getString(cursor.getColumnIndex(COL_SHAPE_KEY)),
                                        title = cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                                        path = cursor.getString(cursor.getColumnIndex(COL_PATH)),
                                        isCurrent =
                                            cursor
                                                .getString(cursor.getColumnIndex(COL_IS_DEFAULT))
                                                .toBoolean(),
                                    )
                                )
                            }
                        }
                    }
            } else {
                null
            }
        }

    override fun applyShapeGridOption(shapeKey: String, gridKey: String): Int {
        return context.contentResolver.update(
            previewUtils.getUri(SHAPE_GRID),
            ContentValues().apply {
                put(COL_SHAPE_KEY, shapeKey)
                put(COL_GRID_KEY, gridKey)
            },
            null,
            null,
        )
    }

    companion object {
        const val SHAPE_OPTIONS: String = "shape_options"
        const val GRID_OPTIONS: String = "grid_options"
        const val SHAPE_GRID: String = "default_grid"
        const val COL_SHAPE_KEY: String = "shape_key"
        const val COL_GRID_KEY: String = "name"
        const val COL_ROWS: String = "rows"
        const val COL_COLS: String = "cols"
        const val COL_IS_DEFAULT: String = "is_default"
        const val COL_TITLE: String = "title"
        const val COL_PATH: String = "path"
    }
}
