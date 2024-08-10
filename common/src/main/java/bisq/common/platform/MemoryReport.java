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

package bisq.common.platform;

import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MemoryReport {

    private static Scheduler scheduler;

    public static void printPeriodically() {
        if (scheduler != null) {
            scheduler.stop();
        }
        scheduler = Scheduler.run(MemoryReport::logReport).periodically(10, 60, TimeUnit.SECONDS);
    }

    public static void logReport() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long used = total - free;

        log.info("\n************************************************************************************************************************\n" +
                        "Total memory: {}; Used memory: {}; Free memory: {}; Max memory: {}; No. of threads: {}\n" +
                        "************************************************************************************************************************",
                StringUtils.formatBytes(total),
                StringUtils.formatBytes(used),
                StringUtils.formatBytes(free),
                StringUtils.formatBytes(runtime.maxMemory()),
                Thread.activeCount());
    }

    public static long getUsedMemoryInBytes() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        return total - free;
    }

    public static long getUsedMemoryInMB() {
        return getUsedMemoryInBytes() / 1024 / 1024;
    }

    public static long getFreeMemoryInMB() {
        return Runtime.getRuntime().freeMemory() / 1024 / 1024;
    }

    public static long getTotalMemoryInMB() {
        return Runtime.getRuntime().totalMemory() / 1024 / 1024;
    }
}
