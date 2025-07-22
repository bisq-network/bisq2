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

package bisq.common.threading;

public class ThreadName {
    // ThreadLocal is not shared across different threads.
    private static final ThreadLocal<String> originalNameThreadLocal = new ThreadLocal<>();

    public static void from(Object host) {
        from(host.getClass());
    }

    public static void from(Object host, String details) {
        from(host.getClass(), details);
    }

    public static void from(Class<?> hostClass) {
        from(hostClass.getSimpleName());
    }

    public static void from(Class<?> hostClass, String details) {
        from(hostClass.getSimpleName(), details);
    }

    public static void from(String hostName, String details) {
        from(hostName + "." + details);
    }

    public static void from(String name) {
        Thread.currentThread().setName(getOriginalName() + ":" + name);
    }

    public static String getOriginalName() {
        String originalName = originalNameThreadLocal.get();
        if (originalName == null) {
            originalName = Thread.currentThread().getName();
            originalNameThreadLocal.set(originalName);
        }
        return originalName;
    }
}
