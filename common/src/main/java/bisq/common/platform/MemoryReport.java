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
import com.sun.management.UnixOperatingSystemMXBean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemoryReport {
    @Getter
    private static final MemoryReport INSTANCE = new MemoryReport();

    private Scheduler scheduler;
    private boolean includeThreadListInMemoryReport;

    public MemoryReport() {
    }

    public void printPeriodically(int memoryReportIntervalSec, boolean includeThreadListInMemoryReport) {
        this.includeThreadListInMemoryReport = includeThreadListInMemoryReport;
        if (scheduler != null) {
            scheduler.stop();
        }
        scheduler = Scheduler.run(this::logReport)
                .host(MemoryReport.class)
                .runnableName("logReport")
                .periodically(30, memoryReportIntervalSec, TimeUnit.SECONDS);
    }

    public void logReport() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long used = total - free;

        if (includeThreadListInMemoryReport) {
            if (OS.isMacOs() || OS.isLinux()) {
                try {
                    UnixOperatingSystemMXBean osBean = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    long openFileDescriptors = osBean.getOpenFileDescriptorCount();
                    long maxFileDescriptors = osBean.getMaxFileDescriptorCount();
                    log.info("openFileDescriptors={}; maxFileDescriptors={}", openFileDescriptors, maxFileDescriptors);
                } catch (Exception e) {
                    log.error("Try to use UnixOperatingSystemMXBean failed", e);
                }
            }

            ThreadProfiler threadProfiler = ThreadProfiler.INSTANCE;
            int nameLength = 80;
            String format = "%-5s\t %-8s\t %-" + nameLength + "s\t %-15s\t %-15s\t %-15s\n";
            String header = String.format(format, "ID", "Priority", "[Group] Name", "State", "Time", "Memory");

            StringBuilder customBisqThreads = new StringBuilder("Bisq custom threads:\n");
            StringBuilder jvmThreads = new StringBuilder("\nJVM threads:\n");
            customBisqThreads.append(header);
            boolean showJvmThreads = true;
            Thread.getAllStackTraces().keySet().stream()
                    .sorted(Comparator.comparing(Thread::threadId))
                    .forEach(thread -> {
                        String groupName = thread.getThreadGroup().getName();
                        String threadName = thread.getName();
                        String fullName = StringUtils.truncate("[" + groupName + "] " + threadName, nameLength);
                        String time = threadProfiler.getThreadTime(thread.threadId()).map(nanoTime ->
                                        SimpleTimeFormatter.formatDuration(TimeUnit.NANOSECONDS.toMillis(nanoTime)))
                                .orElse("N/A");
                        String memory = threadProfiler.getThreadMemory(thread.threadId())
                                .map(DataSizeFormatter::format)
                                .orElse("N/A");
                        int priority = thread.getPriority();
                        String threadState = thread.getState().name();
                        if (threadState.equals("BLOCKED")) {
                            log.warn("Thread {} is in {} state. It might be caused by a deadlock or resource block. " +
                                    "thread={}", thread.threadId(), threadState, thread);
                        }
                        String line = String.format(format,
                                thread.threadId(),
                                priority,
                                fullName,
                                threadState,
                                time,
                                memory
                        );
                        Set<String> excludes = Set.of("DestroyJavaVM",
                                "JavaFX-Launcher",
                                "JavaFX Application Thread",
                                "QuantumRenderer-0",
                                "JNA Cleaner",
                                "HTTP-Dispatcher",
                                "idle-timeout-task",
                                "InvokeLaterDispatcher",
                                "Prism Font Disposer",
                                "Java Sound Event Dispatcher",
                                "CompletableFutureDelayScheduler",
                                "PulseTimer-CVDisplayLink thread");//
                        if (groupName.equals("main") && priority <= 5 && !excludes.contains(threadName)) {
                            customBisqThreads.append(line);
                        } else if (showJvmThreads) {
                            jvmThreads.append(line);
                        }
                    });

            log.info("\n************************************************************************************************************************\n" +
                            "Total memory: {}; Used memory: {}; Free memory: {}; Max memory: {}; No. of threads: {}\n" +
                            "************************************************************************************************************************\n\n" +
                            "{}{}",
                    StringUtils.formatBytes(total),
                    StringUtils.formatBytes(used),
                    StringUtils.formatBytes(free),
                    StringUtils.formatBytes(runtime.maxMemory()),
                    Thread.activeCount(),
                    customBisqThreads,
                    jvmThreads);
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
