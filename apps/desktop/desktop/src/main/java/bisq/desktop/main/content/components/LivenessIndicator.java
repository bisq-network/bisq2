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
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
class LivenessIndicator extends ImageView implements LivenessScheduler.AgeConsumer {
    private static final long MOST_RECENT = TimeUnit.SECONDS.toMillis(3);
    private static final long RECENT = TimeUnit.SECONDS.toMillis(5);

    // As we are used in a hashSet we want to be sure to have a controlled EqualsAndHashCode
    @EqualsAndHashCode.Include
    private final String id = UUID.randomUUID().toString();
    private double size;
    private long age;

    LivenessIndicator() {
    }

    @Override
    public void setAge(long age) {
        this.age = age;
        update();
    }

    void hide() {
        setVisible(false);
        setManaged(false);
    }

    void setSize(double size) {
        this.size = size;
        update();
    }

    private void update() {
        String color;
        if (age == 0 || age > RECENT) {
            color = "grey";
        } else if (age > MOST_RECENT) {
            color = "yellow";
        } else {
            color = "green";
        }

        String sizePostFix = size < 60 ? "-small-dot" : "-dot";
        String id = color + sizePostFix;
        setId(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }
}
