/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.components;

import bisq.desktop.common.threading.UIScheduler;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages LivenessUpdateScheduler per UserProfile to avoid to have too many UIScheduler running in case we have 
 * multiple UserProfileIcon with the same UserProfile.
 */
@Slf4j
@Getter
public class LivenessUpdateScheduler {
    interface LivenessAgeConsumer {
        void setAge(long age);
    }

    interface LivenessFormattedAgeConsumer {
        void setFormattedAge(String formattedAge);
    }

    private static final Map<UserProfile, LivenessUpdateScheduler> schedulerByUserProfile = new HashMap<>();

    static LivenessUpdateScheduler addObserver(UserProfile userProfile,
                                               LivenessAgeConsumer age,
                                               LivenessFormattedAgeConsumer formattedAgeConsumer) {
        LivenessUpdateScheduler livenessUpdateScheduler = schedulerByUserProfile.computeIfAbsent(userProfile, key -> new LivenessUpdateScheduler(userProfile));
        livenessUpdateScheduler.addObserver(age, formattedAgeConsumer);
        return livenessUpdateScheduler;
    }

    static void removeObserver(UserProfile userProfile,
                               LivenessAgeConsumer age,
                               LivenessFormattedAgeConsumer formattedAge) {
        LivenessUpdateScheduler livenessUpdateScheduler = schedulerByUserProfile.get(userProfile);
        if (livenessUpdateScheduler != null) {
            boolean hasObservers = livenessUpdateScheduler.removeObserver(age, formattedAge);
            if (!hasObservers) {
                // We are the last client, so we can remove the LivenessUpdateScheduler
                schedulerByUserProfile.remove(userProfile);
            }
        }
    }

    private final UIScheduler scheduler;
    private final Set<LivenessFormattedAgeConsumer> formattedAgeObservers = new HashSet<>();
    private final Set<LivenessAgeConsumer> ageObservers = new HashSet<>();

    private LivenessUpdateScheduler(UserProfile userProfile) {
        scheduler = UIScheduler.run(() -> {
            long age = System.currentTimeMillis() - userProfile.getPublishDate();
            ageObservers.forEach(observer -> observer.setAge(age));
            String formattedAge = TimeFormatter.formatAge(age);
            formattedAgeObservers.forEach(observer -> observer.setFormattedAge(formattedAge));
        }).periodically(1, TimeUnit.SECONDS);
    }

    private void addObserver(LivenessAgeConsumer age, LivenessFormattedAgeConsumer formattedAge) {
        ageObservers.add(age);
        formattedAgeObservers.add(formattedAge);
    }

    private boolean removeObserver(LivenessAgeConsumer age, LivenessFormattedAgeConsumer formattedAge) {
        ageObservers.remove(age);
        formattedAgeObservers.remove(formattedAge);
        if (ageObservers.isEmpty()) {
            dispose();
            return false;
        }
        return true;
    }

    private void dispose() {
        scheduler.stop();
    }
}
