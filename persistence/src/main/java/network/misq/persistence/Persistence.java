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

import lombok.extern.slf4j.Slf4j;
import network.misq.common.threading.ExecutorFactory;
import network.misq.common.util.FileUtils;

import java.io.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class Persistence {
    private final String directory;
    private final String fileName;
    private final String storagePath;
    private final Object writeLock = new Object();
    private File tempFile;
    private Serializable serializable;

    public Persistence(String directory, Serializable serializable) {
        this(directory, serializable.getClass().getSimpleName(), serializable);
    }

    public Persistence(String directory, String fileName, Serializable serializable) {
        this.directory = directory;
        this.serializable = serializable;
        this.fileName = fileName;
        storagePath = directory + File.separator + fileName;
    }

    public static Serializable read(String storagePath) {
        try (FileInputStream fileInputStream = new FileInputStream(storagePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (Serializable) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            log.error(exception.toString(), exception);
            return null;
        }
    }

    public CompletableFuture<Boolean> persist() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (writeLock) {
                boolean success = false;
                try {
                    FileUtils.makeDirs(directory);
                    // We use a temp file to not risk data corruption in case the write operation fails.
                    // After write is done we rename the tempFile to our storageFile which is an atomic operation.
                    tempFile = File.createTempFile("temp_" + fileName, null, new File(directory));
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
        }, ExecutorFactory.getSingleThreadExecutor("Write-to-disk: " + fileName));
    }
}
