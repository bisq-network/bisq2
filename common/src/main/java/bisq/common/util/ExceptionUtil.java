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

package bisq.common.util;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionUtil {
    public static String getStackTraceAsString(Throwable throwable) {
        return Throwables.getStackTraceAsString(throwable);
    }

    public static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        return getMessageOrToString(rootCause);
    }

    public static String getMessageOrToString(Throwable throwable) {
        if (throwable.getMessage() != null) {
            return throwable.getMessage();
        } else {
            return throwable.toString();
        }
    }

    public static String getCauseStackClassNames(Throwable throwable) {
        List<String> classNames = getCauseStack(throwable).stream()
                .map(e -> e.getClass().getSimpleName())
                .collect(Collectors.toList());
        return Joiner.on(", ").join(classNames);
    }

    public static List<Throwable> getCauseStack(Throwable throwable) {
        List<Throwable> stack = new ArrayList<>();
        while (throwable != null) {
            stack.add(0, throwable);
            throwable = throwable.getCause();
        }
        return stack;
    }

    // We do not want to print the message as that might leak private data, but only want the stack trace
    // Therefore we do not use guava's Throwables.getStackTraceAsString(throwable)
    public static String getSafeStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable rootCause = getRootCause(throwable);
        List<String> traceClasses = Arrays.stream(rootCause.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        traceClasses.add(0,rootCause.getClass().getName());
        return Joiner.on("\n    at ").join(traceClasses);
    }

    public static Throwable getRootCause(Throwable throwable) {
        while (throwable != null && throwable.getCause() != null && !throwable.getCause().equals(throwable)) {
            throwable = throwable.getCause();
        }
        return throwable;
    }
}