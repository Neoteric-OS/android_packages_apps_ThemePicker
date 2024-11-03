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

package com.android.wallpaper.customization.ui.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser

/**
 * Drawable that draws a shape tile with a given path.
 *
 * @param path Path of the shape assuming drawing on a 100x100 canvas.
 */
class ShapeTileDrawable(path: String) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = PathParser.createPathFromPathData(path)
    // The path scaled with regard to the update of drawable bounds
    private val scaledPath = Path(this.path)
    private val scaleMatrix = Matrix()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        scaleMatrix.setScale(bounds.width() / PATH_SIZE, bounds.height() / PATH_SIZE)
        path.transform(scaleMatrix, scaledPath)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(scaledPath, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    @Deprecated(
        "getOpacity() is deprecated",
        ReplaceWith("setAlpha(int)", "android.graphics.drawable.Drawable"),
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    companion object {
        const val PATH_SIZE = 100f
    }
}
