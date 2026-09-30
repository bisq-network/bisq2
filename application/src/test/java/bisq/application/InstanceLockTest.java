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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceLockTest {
    @TempDir
    Path appDataDirPath;

    @Test
    void tryLockCreatesLockFile() throws IOException {
        try (InstanceLock instanceLock = new InstanceLock(appDataDirPath)) {
            assertTrue(instanceLock.tryLock());
            assertTrue(Files.exists(appDataDirPath.resolve("instance.lock")));
        }
    }

    @Test
    void tryLockCreatesMissingDataDir() throws IOException {
        Path notYetCreatedDirPath = appDataDirPath.resolve("sub-dir");
        try (InstanceLock instanceLock = new InstanceLock(notYetCreatedDirPath)) {
            assertTrue(instanceLock.tryLock());
            assertTrue(Files.exists(notYetCreatedDirPath.resolve("instance.lock")));
        }
    }

    @Test
    void tryLockIsIdempotent() throws IOException {
        try (InstanceLock instanceLock = new InstanceLock(appDataDirPath)) {
            assertTrue(instanceLock.tryLock());
            assertTrue(instanceLock.tryLock());
        }
    }

    @Test
    void secondLockOnSameDataDirFails() throws IOException {
        try (InstanceLock firstLock = new InstanceLock(appDataDirPath);
             InstanceLock secondLock = new InstanceLock(appDataDirPath)) {
            assertTrue(firstLock.tryLock());
            assertFalse(secondLock.tryLock());
        }
    }

    @Test
    void lockCanBeAcquiredAfterRelease() throws IOException {
        InstanceLock firstLock = new InstanceLock(appDataDirPath);
        assertTrue(firstLock.tryLock());
        firstLock.close();

        try (InstanceLock secondLock = new InstanceLock(appDataDirPath)) {
            assertTrue(secondLock.tryLock());
        }
    }

    @Test
    void closeIsIdempotent() throws IOException {
        InstanceLock instanceLock = new InstanceLock(appDataDirPath);
        assertTrue(instanceLock.tryLock());
        instanceLock.close();
        instanceLock.close();

        try (InstanceLock secondLock = new InstanceLock(appDataDirPath)) {
            assertTrue(secondLock.tryLock());
        }
    }

    @Test
    void locksOnDifferentDataDirsAreIndependent() throws IOException {
        Path otherDataDirPath = appDataDirPath.resolve("other");
        try (InstanceLock lock = new InstanceLock(appDataDirPath);
             InstanceLock otherLock = new InstanceLock(otherDataDirPath)) {
            assertTrue(lock.tryLock());
            assertTrue(otherLock.tryLock());
        }
    }

    @Test
    void readOwnerPidReturnsPidOfLockOwner() throws IOException {
        try (InstanceLock firstLock = new InstanceLock(appDataDirPath);
             InstanceLock secondLock = new InstanceLock(appDataDirPath)) {
            assertTrue(firstLock.tryLock());
            assertFalse(secondLock.tryLock());
            assertEquals(Optional.of(ProcessHandle.current().pid()), secondLock.readOwnerPid());
        }
    }

    @Test
    void readOwnerPidDoesNotReadTheLockFileIfWeOwnTheLock() throws IOException {
        // Opening the lock file releases our own lock on POSIX systems, thus the owner must answer from memory.
        // We detect that by corrupting the file content, which a file based lookup could not parse.
        try (InstanceLock instanceLock = new InstanceLock(appDataDirPath)) {
            assertTrue(instanceLock.tryLock());
            Files.writeString(appDataDirPath.resolve("instance.lock"), "not-a-pid");
            assertEquals(Optional.of(ProcessHandle.current().pid()), instanceLock.readOwnerPid());
        }
    }

    @Test
    void readOwnerPidIsEmptyIfLockFileIsMissing() {
        assertEquals(Optional.empty(), new InstanceLock(appDataDirPath).readOwnerPid());
    }
}
