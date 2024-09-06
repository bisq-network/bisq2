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


/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package bisq.common.jvm;

import bisq.common.threading.ThreadName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * Based on package private java.io.DeleteOnExitHook. Adds a remove method to avoid
 * memory leak (<a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6664633">...</a>).
 * <p>
 * This class holds a set of filenames to be deleted on VM exit through a shutdown hook.
 * A set is used both to prevent double-insertion of the same file and offer quick removal.
 */
@Slf4j
public class DeleteOnExitHook {
    private static LinkedHashSet<String> files = new LinkedHashSet<>();
    @Getter
    private static volatile boolean shutdownInProgress;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadName.set(DeleteOnExitHook.class, "runHooks");
            runHooks();
        }));
    }

    private DeleteOnExitHook() {
    }

    public static synchronized void add(String file) {
        if (files == null) {
            // DeleteOnExitHook is running. Too late to add a file
            throw new IllegalStateException("Shutdown in progress");
        }

        files.add(file);
    }

    public static synchronized void remove(String file) {
        if (files == null) {
            // DeleteOnExitHook is running. Too late to add a file
            throw new IllegalStateException("Shutdown in progress");
        }

        files.remove(file);
    }

    private static void runHooks() {
        shutdownInProgress = true;
        LinkedHashSet<String> theFiles;

        synchronized (DeleteOnExitHook.class) {
            theFiles = files;
            files = null;
        }

        List<String> toBeDeleted = new ArrayList<>(theFiles);

        // Reverse the list to maintain previous jdk deletion order.
        // Last in first deleted.
        Collections.reverse(toBeDeleted);
        for (String filename : toBeDeleted) {
            if (!(new File(filename)).delete()) {
                log.warn("Deleting {} failed", filename);
            }
        }
    }
}
