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

package bisq.common.facades.android;

import bisq.common.facades.JdkFacade;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AndroidJdkFacade implements JdkFacade {
    @Override
    public String getMyPid() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // String processName = ManagementFactory.getRuntimeMXBean().getName();
        // return processName.split("@")[0];
    }

    @Override
    public Stream<String> getProcessCommandLineStream() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().commandLine().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }

    @Override
    public void writeString(Path torrcPath, String torrc) throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // Files.writeString(torrcPath, torrc);
    }


    @Override
    public String readString(Path controlPortFilePath) throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // return Files.readString(controlPortFilePath);
    }
}
