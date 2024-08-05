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
 * Manages LivenessScheduler per UserProfile to avoid to have too many UIScheduler running in case we have
 * multiple UserProfileIcon with the same UserProfile.
 */
@Slf4j
@Getter
public class LivenessScheduler {
    interface AgeConsumer {
        void setAge(long age);
    }

    interface FormattedAgeConsumer {
        void setFormattedAge(String formattedAge);
    }

    private static final Map<UserProfile, LivenessScheduler> schedulerByUserProfile = new HashMap<>();

    static LivenessScheduler addObserver(UserProfile userProfile,
                                         AgeConsumer age,
                                         FormattedAgeConsumer formattedAgeConsumer) {
        log.error("addObserver {} {} {}", userProfile.getNickName(), age, formattedAgeConsumer);
        LivenessScheduler livenessScheduler = schedulerByUserProfile.computeIfAbsent(userProfile, key -> new LivenessScheduler(userProfile));
        livenessScheduler.addObserver(age, formattedAgeConsumer);
        return livenessScheduler;
    }

    static void removeObserver(UserProfile userProfile,
                               AgeConsumer ageConsumer,
                               FormattedAgeConsumer formattedAgeConsumer) {
        log.error("removeObserver {} {} {}", userProfile.getNickName(), ageConsumer, formattedAgeConsumer);
        LivenessScheduler livenessScheduler = schedulerByUserProfile.get(userProfile);
        if (livenessScheduler != null) {
            boolean hasObservers = livenessScheduler.removeObserver(ageConsumer, formattedAgeConsumer);
            if (!hasObservers) {
                // We are the last client, so we can remove the LivenessUpdateScheduler
                schedulerByUserProfile.remove(userProfile);
            }
        }
    }

    private final UIScheduler scheduler;
    private final Set<FormattedAgeConsumer> formattedAgeConsumers = new HashSet<>();
    private final Set<AgeConsumer> ageConsumers = new HashSet<>();

    private LivenessScheduler(UserProfile userProfile) {
        scheduler = UIScheduler.run(() -> {
            log.error("TICK {} {} {}", userProfile.getNickName(), ageConsumers, formattedAgeConsumers);
            long age = System.currentTimeMillis() - userProfile.getPublishDate();
            ageConsumers.forEach(observer -> observer.setAge(age));
            String formattedAge = TimeFormatter.formatAge(age);
            formattedAgeConsumers.forEach(observer -> observer.setFormattedAge(formattedAge));
        }).periodically(1, TimeUnit.SECONDS);
    }

    private void addObserver(AgeConsumer ageConsumer, FormattedAgeConsumer formattedAgeConsumer) {
        ageConsumers.add(ageConsumer);
        formattedAgeConsumers.add(formattedAgeConsumer);
    }

    private boolean removeObserver(AgeConsumer age, FormattedAgeConsumer formattedAgeConsumer) {
        ageConsumers.remove(age);
        formattedAgeConsumers.remove(formattedAgeConsumer);
        if (ageConsumers.isEmpty()) {
            dispose();
            return false;
        }
        return true;
    }

    private void dispose() {
        scheduler.stop();
    }
}
