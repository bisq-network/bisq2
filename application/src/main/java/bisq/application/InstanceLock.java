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

package bisq.application;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Guards against running more than one instance against the same application data directory,
 * which would lead to corrupted persisted data and, with a wallet, to corrupted wallet state.
 * <p>
 * We acquire an OS level lock (via {@link FileChannel#tryLock()}) on a lock file inside the data
 * directory. The lock is held for the lifetime of the JVM and is released by the OS when the
 * process exits, including at a crash or kill. That is why we do not rely on a PID file alone.
 * <p>
 * We keep strong references to the {@link FileChannel} and the {@link FileLock}. If they got
 * garbage collected the lock would be released silently.
 */
@Slf4j
public class InstanceLock implements AutoCloseable {
    private static final String LOCK_FILE_NAME = "instance.lock";
    // We lock a region beyond any content of the lock file instead of the whole file. On Windows file locks are
    // mandatory, thus a lock on the region which holds the PID would prevent any other process from reading the
    // file. That would break the PID diagnostics as well as any code which copies the data directory, like the
    // backup feature.
    private static final long LOCK_REGION_POSITION = 1024;
    private static final long LOCK_REGION_SIZE = 1;

    private final Path lockFilePath;
    private FileChannel channel;
    private FileLock fileLock;

    public InstanceLock(Path appDataDirPath) {
        lockFilePath = appDataDirPath.resolve(LOCK_FILE_NAME);
    }

    /**
     * @return True if we hold the lock and are therefore the only instance using that data
     * directory. False if another running instance holds it.
     * @throws IOException If the lock file cannot be created or accessed, thus if the locking
     *                     mechanism itself is unusable.
     */
    public synchronized boolean tryLock() throws IOException {
        if (fileLock != null) {
            return true;
        }
        Files.createDirectories(lockFilePath.getParent());
        channel = FileChannel.open(lockFilePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        try {
            // tryLock returns null if the region is locked by another process
            fileLock = channel.tryLock(LOCK_REGION_POSITION, LOCK_REGION_SIZE, false);
        } catch (OverlappingFileLockException e) {
            // The region is locked by this same JVM. We treat that defensively as another instance.
            fileLock = null;
        } catch (IOException e) {
            // The lock mechanism itself failed. We close the channel before propagating so that we
            // do not leak the file descriptor.
            closeChannel();
            throw e;
        }

        if (fileLock == null) {
            closeChannel();
            return false;
        }

        writeOwnerPid();
        return true;
    }

    /**
     * @return The PID written by the instance which holds the lock, if it can be read. This is
     * best effort and used for diagnostics only.
     */
    public Optional<Long> readOwnerPid() {
        try {
            String content = Files.readString(lockFilePath, StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? Optional.empty() : Optional.of(Long.parseLong(content));
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }
        } catch (IOException e) {
            log.warn("Failed to release the instance lock at {}", lockFilePath, e);
        } finally {
            fileLock = null;
            closeChannel();
        }
    }

    private void writeOwnerPid() {
        try {
            String pid = ProcessHandle.current().pid() + System.lineSeparator();
            channel.truncate(0);
            channel.position(0);
            ByteBuffer buffer = ByteBuffer.wrap(pid.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            // We do not call channel.force as the PID is a diagnostic hint and not state which is
            // worth to survive a crash. The write is visible to other processes via the page cache,
            // which is all what readOwnerPid needs.
        } catch (IOException e) {
            // The PID is informational only and its absence does not affect the lock.
            log.warn("Could not write the owner PID to the instance lock file {}", lockFilePath, e);
        }
    }

    private void closeChannel() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            log.warn("Failed to close the instance lock channel at {}", lockFilePath, e);
        } finally {
            channel = null;
        }
    }
}
