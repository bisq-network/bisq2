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

package bisq.wallets.regtest;

import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.process.BisqProcess;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public abstract class AbstractRegtestSetup<T extends BisqProcess> implements BisqProcess {
    public static final String WALLET_PASSPHRASE = "My super secret passphrase that nobody can guess.";

    protected T daemonProcess;
    protected final Path tmpDirPath;

    public AbstractRegtestSetup() throws IOException {
        this.tmpDirPath = createTempDir();
    }

    protected abstract T createProcess();

    public void start() throws InterruptedException {
        daemonProcess = createProcess();
        daemonProcess.start();
    }

    public void shutdown() {
        daemonProcess.shutdown();
    }

    public abstract List<String> mineOneBlock() throws InterruptedException;

    public abstract RpcConfig getRpcConfig();

    public static Path createTempDir() throws IOException {
        Path tempDirPath = Files.createTempDirectory(null);
        recursiveDeleteOnShutdownHook(tempDirPath);
        return tempDirPath;
    }

    public static void recursiveDeleteOnShutdownHook(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file,
                                                             @SuppressWarnings("unused") BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                                    throws IOException {
                                if (e == null) {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                                // directory iteration failed
                                throw e;
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + path, e);
                    }
                }));
    }
}
