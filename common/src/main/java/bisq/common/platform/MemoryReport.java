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

import bisq.common.formatter.DataSizeFormatter;
import bisq.common.formatter.SimpleTimeFormatter;
import bisq.common.threading.ThreadProfiler;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemoryReport {
    private static Scheduler scheduler;
    private static boolean includeThreadListInMemoryReport;

    public static void printPeriodically(int memoryReportIntervalSec, boolean includeThreadListInMemoryReport) {
        MemoryReport.includeThreadListInMemoryReport = includeThreadListInMemoryReport;
        if (scheduler != null) {
            scheduler.stop();
        }
        scheduler = Scheduler.run(MemoryReport::logReport)
                .host(MemoryReport.class)
                .runnableName("logReport")
                .periodically(30, memoryReportIntervalSec, TimeUnit.SECONDS);
    }

    public static void logReport() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long used = total - free;

        StringBuilder sb = new StringBuilder();
        if (includeThreadListInMemoryReport) {
            ThreadProfiler threadProfiler = ThreadProfiler.INSTANCE;
            int nameLength = 100;
            String format = "%-3s\t %-8s\t %-" + nameLength + "s \t %-15s\t %-15s\t %-15s\n";
            sb.append(String.format(format, "ID", "Priority", "[Group] Name", "State", "Time", "Memory"));
            sb.append("-----------------------------------------------------------------------------------------------------------------------------\n");
            Thread.getAllStackTraces().keySet().stream()
                    .sorted(Comparator.comparing(Thread::threadId))
                    .forEach(thread -> {
                        String name = StringUtils.truncate("[" + thread.getThreadGroup().getName() + "] " + thread.getName(), nameLength);
                        String time = threadProfiler.getThreadTime(thread.threadId()).map(nanoTime ->
                                        SimpleTimeFormatter.formatDuration(TimeUnit.NANOSECONDS.toMillis(nanoTime)))
                                .orElse("N/A");
                        String memory = threadProfiler.getThreadMemory(thread.threadId())
                                .map(DataSizeFormatter::format)
                                .orElse("N/A");
                        sb.append(String.format(format,
                                thread.threadId(),
                                thread.getPriority(),
                                name,
                                thread.getState().name(),
                                time,
                                memory
                        ));
                    });
            sb.append("-----------------------------------------------------------------------------------------------------------------------------\n");
            log.info("\n************************************************************************************************************************\n" +
                            "Total memory: {}; Used memory: {}; Free memory: {}; Max memory: {}; No. of threads: {}\n" +
                            "************************************************************************************************************************\n\n" +
                            "Threads:\n{}",
                    StringUtils.formatBytes(total),
                    StringUtils.formatBytes(used),
                    StringUtils.formatBytes(free),
                    StringUtils.formatBytes(runtime.maxMemory()),
                    Thread.activeCount(),
                    sb);
        } else {
            log.info("\n************************************************************************************************************************\n" +
                            "Total memory: {}; Used memory: {}; Free memory: {}; Max memory: {}; No. of threads: {}\n" +
                            "************************************************************************************************************************",
                    StringUtils.formatBytes(total),
                    StringUtils.formatBytes(used),
                    StringUtils.formatBytes(free),
                    StringUtils.formatBytes(runtime.maxMemory()),
                    Thread.activeCount());
        }
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
