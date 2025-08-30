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

package bisq.network.i2p.router.utils;

import lombok.extern.slf4j.Slf4j;
import net.i2p.util.Log;

import java.io.PrintStream;
import java.util.Set;

/**
 * Redirects logs to System.out and System.err to our log back logger and apply some filtering.
 * There are also some logs from native libraries which cannot be redirected.
 */
@Slf4j
public class LogRedirector {
    // Warn and error logs which are considered not critical, level down to INFO
    private static final Set<String> LEVEL_DOWN = Set.of(
            "ERROR - Cannot find I2P installation in",
            "WARNING: Unable to read /proc/cpuinfo",
            "WARNING: Native BigInteger library jbigi not loaded",
            "Warning - No client apps or router console configured - we are just a router");

    public static void redirectSystemStreams() {
        System.setOut(new PrintStream(System.out, true) {
            @Override
            public void println(String logLine) {
                doLog(logLine);
            }

            @Override
            public void println() {
                doLog("");
            }

            @Override
            public void println(Object obj) {
                doLog(String.valueOf(obj));
            }

            @Override
            public void print(String s) {
                doLog(s);
            }

            @Override
            public void print(Object obj) {
                doLog(String.valueOf(obj));
            }
        });

        System.setErr(new PrintStream(System.err, true) {
            @Override
            public void println(String logLine) {
                doLog(logLine);
            }

            @Override
            public void println() {
                doLog("");
            }

            @Override
            public void println(Object obj) {
                doLog(String.valueOf(obj));
            }

            @Override
            public void print(String s) {
                doLog(s);
            }

            @Override
            public void print(Object obj) {
                doLog(String.valueOf(obj));
            }
        });
    }

    private static void doLog(String message) {
        if (LEVEL_DOWN.stream().anyMatch(message::contains)) {
            log.info(message);
            return;
        }
        String trimmed = message.trim().toUpperCase();
        if (trimmed.startsWith(Log.STR_DEBUG)) {
            log.debug(message);
        } else if (trimmed.startsWith(Log.STR_WARN) || trimmed.startsWith("WARNING")) {
            log.warn(message);
        } else if (trimmed.startsWith(Log.STR_ERROR) || trimmed.startsWith(Log.STR_CRIT)) {
            log.error(message);
        } else {   // Default
            log.info(message);
        }
    }
}
