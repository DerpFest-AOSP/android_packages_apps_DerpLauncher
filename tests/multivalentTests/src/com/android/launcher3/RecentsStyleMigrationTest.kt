/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.launcher3.LauncherPrefs.Companion.RECENTS_STYLE
import com.android.launcher3.LauncherPrefs.Companion.RECENTS_STYLE_MIGRATED_V3
import com.android.launcher3.util.SandboxApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RecentsStyleMigrationTest {

    @get:Rule val context = SandboxApplication()

    private val prefs = InMemoryLauncherPrefs(context)

    @Test
    fun legacy_four_option_indices_migrate_to_strings() {
        prefs.put(RECENTS_STYLE.to("2"))
        prefs.migrateRecentsStyleIfNeeded()
        assertEquals("ios", prefs.get(RECENTS_STYLE))
        assertEquals(true, prefs.get(RECENTS_STYLE_MIGRATED_V3))
    }

    @Test
    fun legacy_v2_five_option_indices_migrate_to_strings() {
        prefs.put(RECENTS_STYLE.to("1"))
        prefs.backedUpPrefs
            .edit()
            .putBoolean("pref_recents_style_migrated_v2", true)
            .apply()
        prefs.migrateRecentsStyleIfNeeded()
        assertEquals("stock", prefs.get(RECENTS_STYLE))
        assertFalse(prefs.backedUpPrefs.contains("pref_recents_style_migrated_v2"))
    }

    @Test
    fun current_string_values_are_left_unchanged() {
        prefs.put(RECENTS_STYLE.to("staple"))
        prefs.migrateRecentsStyleIfNeeded()
        assertEquals("staple", prefs.get(RECENTS_STYLE))
    }

    @Test
    fun missing_pref_uses_default_without_writing_style() {
        prefs.migrateRecentsStyleIfNeeded()
        assertFalse(prefs.has(RECENTS_STYLE))
        assertEquals(true, prefs.get(RECENTS_STYLE_MIGRATED_V3))
    }
}
