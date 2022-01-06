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

package network.misq.persistence;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.FileUtils;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
public class Persistence<T extends Serializable> {
    public static final ExecutorService PERSISTENCE_IO_POOL = ExecutorFactory.newFixedThreadPool("Persistence-io-pool");

    private final String directory;
    @Getter
    private final String fileName;
    @Getter
    private final String storagePath;
    private final Object lock = new Object();


    public Persistence(String directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
        storagePath = directory + File.separator + fileName;
    }

    public CompletableFuture<Optional<T>> readAsync(Consumer<T> consumer) {
        return readAsync().whenComplete((result, throwable) -> result.ifPresent(consumer));
    }

    public CompletableFuture<Optional<T>> readAsync() {
        return CompletableFuture.supplyAsync(this::read, PERSISTENCE_IO_POOL);
    }

    public Optional<T> read() {
        if (!new File(storagePath).exists()) {
            return Optional.empty();
        }
        try (FileInputStream fileInputStream = new FileInputStream(storagePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Object object;
            synchronized (lock) {
                object = objectInputStream.readObject();
            }
            return Optional.of((T) object);
        } catch (Throwable exception) {
            log.error(exception.toString(), exception);
            return Optional.empty();
        }
    }

    public CompletableFuture<Boolean> persistAsync(T serializable) {
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("Persistence.persist-" + fileName);
            return persist(serializable);
        }, PERSISTENCE_IO_POOL);
    }

    public boolean persist(T serializable) {
        synchronized (lock) {
            boolean success = false;
            try {
                FileUtils.makeDirs(directory);
                // We use a temp file to not risk data corruption in case the write operation fails.
                // After write is done we rename the tempFile to our storageFile which is an atomic operation.
                File tempFile = File.createTempFile("temp_" + fileName, null, new File(directory));
                FileUtils.deleteOnExit(tempFile);
                File storageFile = new File(directory, fileName);
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                    objectOutputStream.writeObject(serializable);
                    objectOutputStream.flush();
                    fileOutputStream.flush();
                    fileOutputStream.getFD().sync();

                    // Atomic rename
                    FileUtils.renameFile(tempFile, storageFile);
                    log.info("Persisted {}", serializable);
                    success = true;
                } catch (IOException exception) {
                    log.error(exception.toString(), exception);
                } finally {
                    FileUtils.releaseTempFile(tempFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return success;
        }
    }
}
