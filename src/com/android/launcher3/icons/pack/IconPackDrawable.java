/*
 * Copyright (C) 2026 The uwuAOSP Project
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
package com.android.launcher3.icons.pack;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

import com.android.launcher3.icons.IconPackArtwork;

/** Marks user-supplied artwork that must retain its shape and transparency. */
public final class IconPackDrawable extends DrawableWrapper implements IconPackArtwork {
    public IconPackDrawable(Drawable drawable) {
        super(drawable);
    }
}
