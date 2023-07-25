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

package bisq.desktop.common;

import bisq.common.util.ExceptionUtil;
import bisq.common.util.OsUtils;
import javafx.application.HostServices;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class Browser {
    @Nullable
    private static HostServices hostServices;

    public static void setHostServices(@NonNull HostServices hostServices) {
        Browser.hostServices = hostServices;
    }

    public static void open(String uri) {
        if (hostServices == null) {
            throw new IllegalArgumentException("hostServices must be set before open is called");
        }
        try {
            hostServices.showDocument(uri);
        } catch (Exception e) {
            log.error("Error at opening URL with hostServices.showDocument. We try to open it via OsUtils.browse. Error={}; URL={}", ExceptionUtil.print(e), uri);
            OsUtils.browse(uri);
        }
    }
}