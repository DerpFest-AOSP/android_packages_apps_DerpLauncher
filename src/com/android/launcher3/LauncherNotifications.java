/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3;

import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.notification.NotificationRepository;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.SafeCloseable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Proxy class that extends notification listener functionality by allowing multiple
 * listeners to be registered and forwarding updates from NotificationRepository.
 */
public class LauncherNotifications {
    private static LauncherNotifications sInstance;
    private SafeCloseable mStreamSubscription;
    private final Set<NotificationUpdateListener> mListeners = new HashSet<>();

    /**
     * Interface for listeners that want to receive notification updates.
     */
    public interface NotificationUpdateListener {
        /**
         * Called when notification data is updated.
         * @param updatedDots Predicate that tests if a PackageUserKey was updated
         */
        void onNotificationUpdate(Predicate<PackageUserKey> updatedDots);
    }

    private LauncherNotifications() {
    }

    public static synchronized LauncherNotifications getInstance() {
        if (sInstance == null) {
            sInstance = new LauncherNotifications();
        }
        return sInstance;
    }

    /**
     * Initializes the notification listener by subscribing to NotificationRepository updates.
     * Should be called when the application context is available.
     */
    public synchronized void initialize(android.content.Context context) {
        if (mStreamSubscription != null) {
            return; // Already initialized
        }
        NotificationRepository repository = LauncherComponentProvider.get(context)
                .getNotificationRepository();
        mStreamSubscription = repository.getUpdateStream().forEach(
                Executors.MAIN_EXECUTOR, updatedDots -> {
                    onNotificationUpdate(updatedDots);
                    return null;
                });
    }

    /**
     * Adds a listener to receive notification updates.
     * @param listener The listener to add
     */
    public synchronized void addListener(NotificationUpdateListener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener from receiving notification updates.
     * @param listener The listener to remove
     */
    public synchronized void removeListener(NotificationUpdateListener listener) {
        mListeners.remove(listener);
    }

    private void onNotificationUpdate(Predicate<PackageUserKey> updatedDots) {
        // Create a copy of the listeners set to avoid concurrent modification issues
        Set<NotificationUpdateListener> listenersCopy;
        synchronized (this) {
            listenersCopy = new HashSet<>(mListeners);
        }
        for (NotificationUpdateListener listener : listenersCopy) {
            listener.onNotificationUpdate(updatedDots);
        }
    }
}

