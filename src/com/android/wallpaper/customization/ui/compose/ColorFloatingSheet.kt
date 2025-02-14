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
 */

package com.android.wallpaper.customization.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.compose.theme.PlatformTheme
import com.android.themepicker.R

@Preview
@Composable
fun ColorFloatingSheet() {
    // TODO (b/391927276): figure out how to animate color scheme changes
    PlatformTheme {
        val colorScheme = MaterialTheme.colorScheme
        // TODO (b/391927276): replace placeholder colors with actual values
        val colorsList = remember {
            mutableStateListOf(
                Color.Red,
                Color.Green,
                Color.Blue,
                Color.Cyan,
                Color.Magenta,
                Color.Yellow,
                Color.Black,
            )
        }
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(shape = RoundedCornerShape(28.dp))
                    .drawBehind { drawRect(colorScheme.surfaceBright) }
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    text = stringResource(R.string.wallpaper_color_subheader),
                    color = colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(colorsList) { color ->
                        Box(
                            modifier =
                                Modifier.size(
                                    dimensionResource(R.dimen.floating_sheet_color_option_size)
                                )
                        ) {
                            ColorOptionIcon(color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOptionIcon(color: Color) {
    Box(modifier = Modifier.clip(CircleShape).fillMaxSize().drawBehind { drawRect(color) })
}
