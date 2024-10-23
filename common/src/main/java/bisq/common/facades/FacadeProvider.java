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

package bisq.common.facades;

import bisq.common.facades.android.AndroidGuavaFacade;
import bisq.common.facades.android.AndroidJdkFacade;
import bisq.common.network.DefaultLocalhostFacade;
import bisq.common.network.LocalhostFacade;

// The JDKs for Java SE and Android have different API support, thus, we use a
// facade with Android compatible APIs by default and set for Java SE based applicationServices
// the Java SE facade.
// Guava has a different version of its library for Android, thus we apply the facade approach as well.
public class FacadeProvider {
    private static JdkFacade jdkFacade = new AndroidJdkFacade();
    private static GuavaFacade guavaFacade = new AndroidGuavaFacade();
    private static LocalhostFacade localhostFacade = new DefaultLocalhostFacade();

    public static void setJdkFacade(JdkFacade jdkFacade) {
        FacadeProvider.jdkFacade = jdkFacade;
    }

    public static void setGuavaFacade(GuavaFacade guavaFacade) {
        FacadeProvider.guavaFacade = guavaFacade;
    }

    public static JdkFacade getJdkFacade() {
        return jdkFacade;
    }

    public static GuavaFacade getGuavaFacade() {
        return guavaFacade;
    }

    public static LocalhostFacade getLocalhostFacade() {
        return localhostFacade;
    }

    public static void setLocalhostFacade(LocalhostFacade localhostFacade) {
        FacadeProvider.localhostFacade = localhostFacade;
    }

}
