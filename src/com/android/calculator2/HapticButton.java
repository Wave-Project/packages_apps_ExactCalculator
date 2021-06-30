/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.calculator2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * A basic Button that vibrates on finger down.
 */
public class HapticButton extends Button {
    public HapticButton(Context context) {
        super(context);
        initVibration();
    }

    public HapticButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVibration();
    }

    public HapticButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVibration();
    }

    private void initVibration() {
        setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            return false;
        });
    }
}
