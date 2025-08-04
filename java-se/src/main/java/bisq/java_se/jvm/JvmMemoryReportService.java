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

package bisq.java_se.jvm;

import bisq.common.formatter.DataSizeFormatter;
import bisq.common.formatter.SimpleTimeFormatter;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.common.platform.MemoryReportService;
import bisq.common.platform.OS;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.java_se.utils.ThreadProfiler;
import com.sun.management.UnixOperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JvmMemoryReportService implements MemoryReportService {
    private static final Set<String> JVM_THREAD_NAMES = Set.of("DestroyJavaVM",
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
            "InnocuousThreadGroup",
            "PulseTimer-CVDisplayLink thread");
    private static final Set<String> JVM_GROUP_NAMES = Set.of("system",
            "InnocuousThreadGroup");

    private final int memoryReportIntervalSec;
    private final boolean includeThreadListInMemoryReport;
    private final ObservableHashMap<String, ObservableHashMap<Long, AtomicInteger>> historicalNumThreadsByThreadName = new ObservableHashMap<>();
    private final Observable<Integer> currentNumThreads = new Observable<>(0);
    private final Observable<Integer> peakNumThreads = new Observable<>(0);
    @Nullable
    private Scheduler logReportScheduler, addStatisticsScheduler;

    public JvmMemoryReportService(int memoryReportIntervalSec, boolean includeThreadListInMemoryReport) {
        this.memoryReportIntervalSec = memoryReportIntervalSec;
        this.includeThreadListInMemoryReport = includeThreadListInMemoryReport;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        logReportScheduler = Scheduler.run(this::logReport)
                .host(JvmMemoryReportService.class)
                .runnableName("logReport")
                .periodically(90, memoryReportIntervalSec, TimeUnit.SECONDS);

        addStatisticsScheduler = Scheduler.run(this::addPoolThreadsStatistics)
                .host(JvmMemoryReportService.class)
                .runnableName("addStatistics")
                .periodically(0, NUM_POOL_THREADS_UPDATE_INTERVAL_SEC, TimeUnit.SECONDS);

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (logReportScheduler != null) {
            logReportScheduler.stop();
            logReportScheduler = null;
        }
        if (addStatisticsScheduler != null) {
            addStatisticsScheduler.stop();
            addStatisticsScheduler = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    // Tracks the number of pool threads at the given interval up to MAX_HISTORICAL_NUM_THREADS items per thread.
    private void addPoolThreadsStatistics() {
        int activeCount = Thread.activeCount();
        currentNumThreads.set(activeCount);
        peakNumThreads.set(Math.max(peakNumThreads.get(), activeCount));

        Map<String, AtomicInteger> numPoolThreadsByThreadName = new HashMap<>();
        Thread.getAllStackTraces().keySet().forEach(thread -> {
            String threadName = thread.getName();
            boolean isIndexedName = threadName.matches(".*-\\d+");
            if (!isJvmThread(thread) && isIndexedName) {
                // We use the patter with - and the thread ID for pools. As we are only interested in pool threads we filter for those.
                String poolName = threadName.replaceAll("-\\d+", "");
                numPoolThreadsByThreadName.computeIfAbsent(poolName, k -> new AtomicInteger())
                        .incrementAndGet();
            }
        });
        long now = System.currentTimeMillis();
        long cutOffDate = now - MAX_AGE_NUM_POOL_THREADS;
        numPoolThreadsByThreadName.forEach((poolName, numThreads) -> {
            ObservableHashMap<Long, AtomicInteger> history = historicalNumThreadsByThreadName.computeIfAbsent(poolName, k -> new ObservableHashMap<>());
            history.put(now, numThreads);
            history.entrySet().removeIf(entry -> entry.getKey() < cutOffDate);
        });

        /*log.debug("historicalNumThreadsByThreadName " + historicalNumThreadsByThreadName.entrySet().stream()
                .filter(e -> e.getKey().equals("Network.notify"))
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("\n")));*/
    }

    @Override
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
            // Cumulative allocated memory. There is no API for the current memory per thread (requires heap dump or profiler).
            // See: https://stackoverflow.com/questions/36176593/will-threadmxbeangetthreadallocatedbytes-return-size-of-allocated-memory-or-obj
            String header = String.format(format, "ID", "Priority", "[Group] Name", "State", "Time", "Cumulative allocated memory");

            StringBuilder customBisqThreads = new StringBuilder("Bisq custom threads:\n");
            StringBuilder jvmThreads = new StringBuilder("\nJVM threads:\n");
            customBisqThreads.append(header);
            boolean showJvmThreads = true;
            Thread.getAllStackTraces().keySet().stream()
                    .sorted(Comparator.comparing(Thread::threadId))
                    .forEach(thread -> {
                        try {
                            ThreadGroup threadGroup = thread.getThreadGroup();
                            String groupName = threadGroup != null ? threadGroup.getName() : "N/A";
                            String threadName = thread.getName();
                            String fullName = StringUtils.truncate("[" + groupName + "] " + threadName, nameLength);
                            long threadId = thread.threadId();
                            Optional<Long> threadTime = Optional.empty();
                            try {
                                threadTime = threadProfiler.getThreadTime(threadId);
                            } catch (Exception ignored) {
                                // Not all threads support that
                            }
                            String time = threadTime.map(nanoTime ->
                                            SimpleTimeFormatter.formatDuration(TimeUnit.NANOSECONDS.toMillis(nanoTime)))
                                    .orElse("N/A");
                            String memory = threadProfiler.getThreadMemory(threadId)
                                    .map(DataSizeFormatter::format)
                                    .orElse("N/A");
                            int priority = thread.getPriority();
                            String threadState = thread.getState().name();
                            if (threadState.equals("BLOCKED")) {
                                log.warn("Thread {} is in {} state. It might be caused by a deadlock or resource block. " +
                                        "thread={}", threadId, threadState, thread);
                            }
                            String line = String.format(format,
                                    threadId,
                                    priority,
                                    fullName,
                                    threadState,
                                    time,
                                    memory
                            );
                            if (!isJvmThread(thread)) {
                                customBisqThreads.append(line);
                            } else if (showJvmThreads) {
                                jvmThreads.append(line);
                            }
                        } catch (Exception e) {
                            log.error("Error at generating logs for threads", e);
                        }
                    });

            log.error("\n************************************************************************************************************************\n" +
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

    @Override
    public long getUsedMemoryInBytes() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        return total - free;
    }

    @Override
    public long getUsedMemoryInMB() {
        return getUsedMemoryInBytes() / 1024 / 1024;
    }

    @Override
    public long getFreeMemoryInMB() {
        return Runtime.getRuntime().freeMemory() / 1024 / 1024;
    }

    @Override
    public long getTotalMemoryInMB() {
        return Runtime.getRuntime().totalMemory() / 1024 / 1024;
    }

    @Override
    public ReadOnlyObservableMap<String, ObservableHashMap<Long, AtomicInteger>> getHistoricalNumThreadsByThreadName() {
        return historicalNumThreadsByThreadName;
    }

    @Override
    public ReadOnlyObservable<Integer> getCurrentNumThreads() {
        return currentNumThreads;
    }

    @Override
    public ReadOnlyObservable<Integer> getPeakNumThreads() {
        return peakNumThreads;
    }

    private static boolean isJvmThread(Thread thread) {
        ThreadGroup threadGroup = thread.getThreadGroup();
        String groupName = threadGroup != null ? threadGroup.getName() : "N/A";
        return JVM_THREAD_NAMES.contains(thread.getName()) || JVM_GROUP_NAMES.contains(groupName);
    }
}
