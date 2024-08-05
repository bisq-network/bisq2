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

import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class LivenessScheduler {
    interface AgeConsumer {
        void setAge(Long age);
    }

    interface FormattedAgeConsumer {
        void setFormattedAge(String formattedAge);
    }

    private UIScheduler scheduler;
    private UserProfile userProfile;
    private final AgeConsumer ageConsumer;
    private final FormattedAgeConsumer formattedAgeConsumer;
    private boolean disabled;

    LivenessScheduler(AgeConsumer ageConsumer, FormattedAgeConsumer formattedAgeConsumer) {
        this.ageConsumer = ageConsumer;
        this.formattedAgeConsumer = formattedAgeConsumer;
    }

    void start(UserProfile userProfile) {
        if (disabled) {
            return;
        }
        if (userProfile == null) {
            this.userProfile = null;
            dispose();
            return;
        }
        // PublishDate is transient and thus not included in the equals check
        if (userProfile.equals(this.userProfile) && userProfile.getPublishDate() == this.userProfile.getPublishDate()) {
            return;
        }

        dispose();
        scheduler = UIScheduler.run(() -> {
            long publishDate = userProfile.getPublishDate();
            if (publishDate == 0) {
                ageConsumer.setAge(null);
                formattedAgeConsumer.setFormattedAge(null);
            } else {
                long age = Math.max(0, System.currentTimeMillis() - publishDate);
                ageConsumer.setAge(age);
                String formattedAge = TimeFormatter.formatAge(age);
                formattedAgeConsumer.setFormattedAge(formattedAge);
            }
        }).periodically(0, 1, TimeUnit.SECONDS);
    }

    void dispose() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
    }

    public void disable() {
        disabled = true;
        dispose();
    }

    public void enable() {
        disabled = false;
    }
}
