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

package bisq.desktop.testutil;

import bisq.i18n.Res;

public abstract class TestFxHeadlessSupport {
    static {
        System.setProperty("testfx.robot", "glass");

        // JavaFX 21 + current Monocle dependency can fail with AbstractMethodError.
        // Default to non-headless mode, but allow overriding via -Dtestfx.headless=true.
        String headless = System.getProperty("testfx.headless", "false");
        System.setProperty("testfx.headless", headless);

        if ("true".equalsIgnoreCase(headless)) {
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
        }

        Res.setAndApplyLanguageTag("en");
    }
}
