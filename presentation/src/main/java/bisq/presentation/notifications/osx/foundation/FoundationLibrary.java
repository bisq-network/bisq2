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

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Based on https://github.com/joewalnes/idea-community/blob/master/platform/util/src/com/intellij/ui/mac/foundation/FoundationLibrary.java
 */
public interface FoundationLibrary extends Library {
    void NSLog(Pointer pString, Object thing);

    ID NSFullUserName();

    ID objc_allocateClassPair(ID supercls, String name, int extraBytes);

    void objc_registerClassPair(ID cls);

    ID CFStringCreateWithBytes(Pointer allocator, byte[] bytes, int byteCount, int encoding, byte isExternalRepresentation);

    byte CFStringGetCString(ID theString, byte[] buffer, int bufferSize, int encoding);

    int CFStringGetLength(ID theString);

    long CFStringConvertNSStringEncodingToEncoding(long nsEncoding);

    ID CFStringConvertEncodingToIANACharSetName(long cfEncoding);

    long CFStringConvertIANACharSetNameToEncoding(ID encodingName);

    long CFStringConvertEncodingToNSStringEncoding(long cfEncoding);

    void CFRetain(ID cfTypeRef);

    void CFRelease(ID cfTypeRef);

    int CFGetRetainCount(Pointer cfTypeRef);

    ID objc_getClass(String className);

    ID objc_getProtocol(String name);

    ID class_createInstance(ID pClass, int extraBytes);

    Pointer sel_registerName(String selectorName);

    ID class_replaceMethod(ID cls, Pointer selName, Callback impl, String types);

    ID objc_getMetaClass(String name);

    /**
     * Note: Vararg version. Should only be used only for selectors with a single fixed argument followed by varargs.
     */
    ID objc_msgSend(ID receiver, Pointer selector, Object firstArg, Object... args);

    boolean class_respondsToSelector(ID cls, Pointer selName);

    boolean class_addMethod(ID cls, Pointer selName, Callback imp, String types);

    boolean class_addMethod(ID cls, Pointer selName, ID imp, String types);

    boolean class_addProtocol(ID aClass, ID protocol);

    boolean class_isMetaClass(ID cls);

    ID NSStringFromSelector(Pointer selector);

    ID NSStringFromClass(ID aClass);

    Pointer objc_getClass(Pointer clazz);

    int kCFStringEncodingMacRoman = 0;
    int kCFStringEncodingWindowsLatin1 = 0x0500;
    int kCFStringEncodingISOLatin1 = 0x0201;
    int kCFStringEncodingNextStepLatin = 0x0B01;
    int kCFStringEncodingASCII = 0x0600;
    int kCFStringEncodingUnicode = 0x0100;
    int kCFStringEncodingUTF8 = 0x08000100;
    int kCFStringEncodingNonLossyASCII = 0x0BFF;

    int kCFStringEncodingUTF16 = 0x0100;
    int kCFStringEncodingUTF16BE = 0x10000100;
    int kCFStringEncodingUTF16LE = 0x14000100;
    int kCFStringEncodingUTF32 = 0x0c000100;
    int kCFStringEncodingUTF32BE = 0x18000100;
    int kCFStringEncodingUTF32LE = 0x1c000100;

    // https://developer.apple.com/library/mac/documentation/Carbon/Reference/CGWindow_Reference/Constants/Constants.html#//apple_ref/doc/constant_group/Window_List_Option_Constants
    int kCGWindowListOptionAll = 0;
    int kCGWindowListOptionOnScreenOnly = 1;
    int kCGWindowListOptionOnScreenAboveWindow = 2;
    int kCGWindowListOptionOnScreenBelowWindow = 4;
    int kCGWindowListOptionIncludingWindow = 8;
    int kCGWindowListExcludeDesktopElements = 16;

    //https://developer.apple.com/library/mac/documentation/Carbon/Reference/CGWindow_Reference/Constants/Constants.html#//apple_ref/doc/constant_group/Window_Image_Types
    int kCGWindowImageDefault = 0;
    int kCGWindowImageBoundsIgnoreFraming = 1;
    int kCGWindowImageShouldBeOpaque = 2;
    int kCGWindowImageOnlyShadows = 4;
    int kCGWindowImageBestResolution = 8;
    int kCGWindowImageNominalResolution = 16;


    // see enum NSBitmapImageFileType
    int NSBitmapImageFileTypeTIFF = 0;
    int NSBitmapImageFileTypeBMP = 1;
    int NSBitmapImageFileTypeGIF = 2;
    int NSBitmapImageFileTypeJPEG = 3;
    int NSBitmapImageFileTypePNG = 4;
    int NSBitmapImageFileTypeJPEG2000 = 5;
}
