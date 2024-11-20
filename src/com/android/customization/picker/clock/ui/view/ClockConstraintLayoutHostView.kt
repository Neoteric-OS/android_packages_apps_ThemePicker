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
package com.android.customization.picker.clock.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.customization.picker.clock.shared.ClockSize
import com.android.systemui.plugins.clocks.ClockController
import com.android.wallpaper.util.ScreenSizeCalculator

/**
 * Parent view for the clock view. We will calculate the current display size and the preview size
 * and scale down the clock view to fit in the preview.
 */
class ClockConstraintLayoutHostView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenSize = ScreenSizeCalculator.getInstance().getScreenSize(display)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(screenSize.x, EXACTLY),
            MeasureSpec.makeMeasureSpec(screenSize.y, EXACTLY),
        )
        val ratio = MeasureSpec.getSize(widthMeasureSpec) / screenSize.x.toFloat()
        scaleX = ratio
        scaleY = ratio
    }

    companion object {
        fun addClockViews(
            clockController: ClockController,
            rootView: ClockConstraintLayoutHostView,
            size: ClockSize,
        ) {
            clockController.let { clock ->
                when (size) {
                    ClockSize.DYNAMIC -> {
                        clock.largeClock.layout.views.forEach {
                            if (it.parent != null) {
                                (it.parent as ViewGroup).removeView(it)
                            }
                            rootView.addView(it).apply { it.visibility = View.VISIBLE }
                        }
                    }

                    ClockSize.SMALL -> {
                        clock.smallClock.layout.views.forEach {
                            if (it.parent != null) {
                                (it.parent as ViewGroup).removeView(it)
                            }
                            rootView.addView(it).apply { it.visibility = View.VISIBLE }
                        }
                    }
                }
            }
        }
    }
}
