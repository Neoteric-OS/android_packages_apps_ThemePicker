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
package com.android.customization.picker.color.ui.view

import android.annotation.ColorInt
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import com.android.themepicker.R
import com.android.wallpaper.picker.option.ui.view.OptionItemBackground

/**
 * Draw a color option icon, which is a quadrant circle that can show at most 4 different colors.
 */
class ColorOptionIconView2(context: Context, attrs: AttributeSet) :
    OptionItemBackground(context, attrs) {

    private val paint = Paint().apply { style = Paint.Style.FILL }

    private val path = Path()

    private var color0 = DEFAULT_PLACEHOLDER_COLOR
    private var color1 = DEFAULT_PLACEHOLDER_COLOR
    private var color2 = DEFAULT_PLACEHOLDER_COLOR
    private var color3 = DEFAULT_PLACEHOLDER_COLOR
    private var strokeColor = DEFAULT_PLACEHOLDER_COLOR
    private val strokeWidth =
        context.resources
            .getDimensionPixelSize(R.dimen.floating_sheet_color_option_stroke_width)
            .toFloat()

    private var w = 0
    private var h = 0

    /**
     * @param color0 the color in the top left quadrant
     * @param color1 the color in the top right quadrant
     * @param color2 the color in the bottom left quadrant
     * @param color3 the color in the bottom right quadrant
     */
    fun bindColor(
        @ColorInt strokeColor: Int,
        @ColorInt color0: Int,
        @ColorInt color1: Int,
        @ColorInt color2: Int,
        @ColorInt color3: Int,
    ) {
        this.strokeColor = strokeColor
        this.color0 = color0
        this.color1 = color1
        this.color2 = color2
        this.color3 = color3
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.w = w
        this.h = h
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        // The w and h need to be an even number to avoid tiny pixel-level gaps between the pies
        w = w.roundDownToEven()
        h = h.roundDownToEven()

        val width = w.toFloat()
        val height = h.toFloat()

        val left = 2 * strokeWidth
        val right = width - 2 * strokeWidth
        val top = 2 * strokeWidth
        val bottom = height - 2 * strokeWidth
        val cornerRadius = ((right - left) / 2) * (1f - 0.25f * progress)
        val save = canvas.save()
        path.reset()
        path.addRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, Path.Direction.CW)
        path.close()
        canvas.clipPath(path)

        canvas.apply {
            paint.style = Paint.Style.FILL
            // top left
            paint.color = color0
            drawRect(0f, 0f, width / 2, height / 2, paint)
            // top right
            paint.color = color1
            drawRect(width / 2, 0f, width, height / 2, paint)
            // bottom left
            paint.color = color2
            drawRect(0f, height / 2, width / 2, height, paint)
            // bottom right
            paint.color = color3
            drawRect(width / 2, height / 2, width, height, paint)
        }

        canvas.restoreToCount(save)
        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.alpha = (255 * progress).toInt()
        paint.strokeWidth = this.strokeWidth
        val strokeCornerRadius = ((width - strokeWidth) / 2) * (1f - 0.25f * progress)
        val halfStrokeWidth = 0.5f * strokeWidth
        // Stroke is centered along the path, so account for half strokeWidth to stay within View
        canvas.drawRoundRect(
            halfStrokeWidth,
            halfStrokeWidth,
            width - halfStrokeWidth,
            height - halfStrokeWidth,
            strokeCornerRadius,
            strokeCornerRadius,
            paint,
        )
    }

    companion object {
        const val DEFAULT_PLACEHOLDER_COLOR = Color.BLACK

        fun Int.roundDownToEven(): Int {
            return if (this % 2 == 0) this else this - 1
        }
    }
}
