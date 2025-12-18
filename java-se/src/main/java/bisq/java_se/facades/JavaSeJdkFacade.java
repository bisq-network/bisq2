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

package bisq.java_se.facades;

import bisq.common.facades.FacadeProvider;
import bisq.common.facades.JdkFacade;
import bisq.common.file.FileMutatorUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
public class JavaSeJdkFacade implements JdkFacade {
    @Override
    public String getMyPid() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        return processName.split("@")[0];
    }

    @Override
    public Stream<String> getProcessCommandStream() {
        return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().command().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public String readString(Path path, Charset charset) throws IOException {
        return Files.readString(path, charset);
    }

    @Override
    public void writeString(String data, Path path) throws IOException {
        Files.writeString(path, data, StandardCharsets.UTF_8);
    }
}
