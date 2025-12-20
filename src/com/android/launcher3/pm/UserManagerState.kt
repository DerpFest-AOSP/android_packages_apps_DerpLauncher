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
package com.android.launcher3.pm

import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.android.launcher3.pm.UserCache.CachedUserInfo
import com.android.launcher3.util.UserIconInfo

/** Current snapshot of UserManager information */
class UserManagerState(private val userMap: Map<UserHandle, CachedUserInfo>) {

    private val userSerialMap = userMap.mapKeys { it.value.iconInfo.userSerial }

    /** Returns true if quiet mode is enabled for the provided user */
    fun isUserQuiet(serialNo: Long): Boolean = userSerialMap[serialNo]?.isQuietModeEnabled ?: false

    /** Returns true if quiet mode is enabled for the provided user */
    fun isUserQuiet(user: UserHandle): Boolean = userMap[user]?.isQuietModeEnabled ?: false

    /** Returns the [UserHandle] corresponding to the [serialNo] */
    fun getUser(serialNo: Long): UserHandle =
        userSerialMap[serialNo]?.iconInfo?.user ?: Process.myUserHandle()

    /** Returns the user locked state */
    fun isUserUnlocked(user: UserHandle) = userMap[user]?.isUnlocked ?: true

    /**
     * Returns true if any user profile has quiet mode enabled.
     *
     * Do not use this for determining if a specific profile has quiet mode enabled, as their can be
     * more than one profile in quiet mode.
     */
    val isAnyProfileQuietModeEnabled: Boolean
        get() = userMap.any { it.value.isQuietModeEnabled }

    /**
     * Returns true if all managed profiles have quiet mode enabled.
     */
    fun isAllProfilesQuietModeEnabled(): Boolean {
        // Because the parent user is included, there will always be at least one user returned
        // by getUserProfiles and tracked by userMap, even if there are no managed profiles.
        val numProfilesIncludingParent = userMap.size
        if (numProfilesIncludingParent <= 1) {
            // There are no managed profiles, only the parent user, so we can return early.
            return false
        }
        for ((user, cachedInfo) in userMap) {
            if (Process.myUserHandle() == user) {
                // Skip the parent user.
                continue
            }
            if (!cachedInfo.isQuietModeEnabled) {
                return false
            }
        }
        // Quiet mode is on for all users.
        return true
    }

    fun hasMultipleProfiles(): Boolean {
        val numProfiles = userMap.size - 1 // not including the parent
        return numProfiles > 1
    }

    /**
     * Returns true if all managed work profiles have quiet mode enabled.
     */
    fun isAllWorkProfilesQuietModeEnabled(): Boolean {
        // Because the parent user is included, there will always be at least one user returned
        // by getUserProfiles and tracked by userMap, even if there are no managed profiles.
        val numProfilesIncludingParent = userMap.size
        if (numProfilesIncludingParent <= 1) {
            // There are no managed profiles, only the parent user, so we can return early.
            return false
        }
        for ((user, cachedInfo) in userMap) {
            if (!cachedInfo.iconInfo.isWork) {
                // Skip if it's not work profile
                continue
            }
            if (!cachedInfo.isQuietModeEnabled) {
                return false
            }
        }
        // Quiet mode is on for all work profiles.
        return true
    }

    fun hasMultipleWorkProfiles(): Boolean {
        // Because the parent user is included, there will always be at least one user returned
        // by getUserProfiles and tracked by userMap, even if there are no managed profiles.
        val numProfilesIncludingParent = userMap.size
        if (numProfilesIncludingParent <= 1) {
            // There are no managed profiles, only the parent user, so we can return early.
            return false
        }
        var workProfileCount = 0
        for ((_, cachedInfo) in userMap) {
            if (cachedInfo.iconInfo.isWork) {
                workProfileCount++
            }
        }
        return workProfileCount > 1
    }

    /** Returns the user properties for the provided user or default values */
    fun getUserInfo(user: UserHandle): UserIconInfo = getCachedInfo(user).iconInfo

    /** @see UserManager.getUserProfiles */
    val userProfiles: List<UserHandle>
        get() = userMap.keys.toList()

    fun getAllCachedInfos() = userMap.values

    /** Returns the pre-installed apps for a user. */
    fun getPreInstallApps(user: UserHandle): Set<String> =
        userMap[user]?.preInstallApps ?: emptySet()

    fun getCachedInfo(user: UserHandle): CachedUserInfo =
        userMap[user]
            ?: CachedUserInfo(
                iconInfo = UserIconInfo(user, UserIconInfo.TYPE_MAIN),
                isUnlocked = true,
                isQuietModeEnabled = false,
            )
}
