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

package bisq.desktop.common.qr.webcam;

import lombok.Getter;

@Getter
public enum VideoSize {
    TINY(320, 240),
    SMALL(480, 360),
    SD(640, 480),
    HD(1280, 720),
    FULL_HD(1920, 1080),
    QUAD_HD(2560, 1440),
    TWO_K(2048, 1080),
    ULTRA_HD(3840, 2160); // 4K

    private final int width;
    private final int height;

    VideoSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
