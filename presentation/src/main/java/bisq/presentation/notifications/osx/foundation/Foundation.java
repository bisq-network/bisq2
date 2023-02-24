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

/**
 * Copyright 2000-2010 JetBrains s.r.o.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package bisq.presentation.notifications.osx.foundation;

import com.sun.jna.*;

import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Based on https://github.com/joewalnes/idea-community/blob/master/platform/util/src/com/intellij/ui/mac/foundation/Foundation.java
 */
public final class Foundation {
    private static final FoundationLibrary myFoundationLibrary;
    private static final Function myObjcMsgSend;

    static {
        myFoundationLibrary = Native.load("Foundation", FoundationLibrary.class,
                Collections.singletonMap("jna.encoding", "UTF8"));
        NativeLibrary nativeLibrary = ((Library.Handler) Proxy.getInvocationHandler(myFoundationLibrary)).getNativeLibrary();
        myObjcMsgSend = nativeLibrary.getFunction("objc_msgSend");
    }

    public static void init() {
        // Triggers static initializer
    }

    private Foundation() {
    }

    public static ID getObjcClass(String className) {
        return myFoundationLibrary.objc_getClass(className);
    }

    public static Pointer createSelector(String s) {
        return myFoundationLibrary.sel_registerName(s);
    }

    private static Object[] prepInvoke(ID id, Pointer selector, Object[] args) {
        Object[] invokArgs = new Object[args.length + 2];
        invokArgs[0] = id;
        invokArgs[1] = selector;
        System.arraycopy(args, 0, invokArgs, 2, args.length);
        return invokArgs;
    }

    public static ID invoke(final ID id, final Pointer selector, Object... args) {
        // objc_msgSend is called with the calling convention of the target method
        // on x86_64 this does not make a difference, but arm64 uses a different calling convention for varargs
        // it is therefore important to not call objc_msgSend as a vararg function
        return new ID(myObjcMsgSend.invokeLong(prepInvoke(id, selector, args)));
    }

    public static ID invoke(final ID id, final String selector, Object... args) {
        return invoke(id, createSelector(selector), args);
    }

    private static final class NSString {
        private static final ID nsStringCls = getObjcClass("NSString");
        private static final Pointer stringSel = createSelector("string");
        private static final Pointer allocSel = createSelector("alloc");
        private static final Pointer autoreleaseSel = createSelector("autorelease");
        private static final Pointer initWithBytesLengthEncodingSel = createSelector("initWithBytes:length:encoding:");
        private static final long nsEncodingUTF16LE = convertCFEncodingToNS(FoundationLibrary.kCFStringEncodingUTF16LE);


        public static ID create(String s) {
            // Use a byte[] rather than letting jna do the String -> char* marshalling itself.
            // Turns out about 10% quicker for long strings.
            if (s.isEmpty()) {
                return invoke(nsStringCls, stringSel);
            }

            byte[] utf16Bytes = s.getBytes(StandardCharsets.UTF_16LE);
            return invoke(invoke(invoke(nsStringCls, allocSel),
                            initWithBytesLengthEncodingSel, utf16Bytes, utf16Bytes.length, nsEncodingUTF16LE),
                    autoreleaseSel);
        }
    }

    public static ID nsString(@Nullable String s) {
        return s == null ? ID.NIL : NSString.create(s);
    }

    private static long convertCFEncodingToNS(long cfEncoding) {
        return myFoundationLibrary.CFStringConvertEncodingToNSStringEncoding(cfEncoding) & 0xffffffffffL;  // trim to C-type limits
    }
}
