/*
 * Copyright (C) 2021 The Android Open Source Project
 *               2021 WaveOS
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
package com.android.calculator2

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton

/**
 * A basic ImageButton that vibrates on touch down.
 */
class HapticImageButton(context: Context?, attrs: AttributeSet?) : ImageButton(context, attrs) {

    init {
        HapticButton.initVibration(this)
    }
}