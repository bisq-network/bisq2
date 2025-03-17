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

import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
class LivenessIndicator extends ImageView implements LivenessScheduler.AgeConsumer {
    private static final long LAST_MINUTES = TimeUnit.MINUTES.toMillis(10);
    private static final long LAST_HOUR = TimeUnit.HOURS.toMillis(1);

    private double size;
    private Long age;

    LivenessIndicator() {
    }

    @Override
    public void setAge(Long age) {
        this.age = age;
        updateId();
    }

    void hide() {
        setVisible(false);
        setManaged(false);
    }

    void setSize(double size) {
        this.size = size;
        updateId();
    }

    private void updateId() {
        String color;
        if (age == null || age > LAST_HOUR) {
            color = "grey";
        } else if (age > LAST_MINUTES) {
            color = "yellow";
        } else {
            color = "green";
        }

        String sizePostFix = size < 60 ? "-small-dot" : "-dot";
        String id = color + sizePostFix;
        setId(id);
    }
}
