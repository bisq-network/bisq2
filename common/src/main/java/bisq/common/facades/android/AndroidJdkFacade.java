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

import java.io.File;
import java.util.stream.Stream;

public class AndroidJdkFacade implements JdkFacade {
    private final String myPid;

    public AndroidJdkFacade(int myPid) {
        this.myPid = String.valueOf(myPid);
    }

    @Override
    public String getMyPid() {
        return myPid;
    }

    @Override
    public Stream<String> getProcessCommandStream() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().commandLine().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        // ProcessBuilder.Redirect.DISCARD not supported on Android
        processBuilder.redirectError(new File("/dev/null"));
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        // ProcessBuilder.Redirect.DISCARD not supported on Android
        processBuilder.redirectError(new File("/dev/null"));
    }
}
