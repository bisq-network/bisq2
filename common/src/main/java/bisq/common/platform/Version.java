/**
 * Based on com.sshtools.twoslices.Version
 * <p>
 * Copyright © 2018 SSHTOOLS Limited (support@sshtools.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bisq.common.platform;

import lombok.Getter;

public class Version implements Comparable<Version> {
    public static void validate(String versionAsString) {
        if (versionAsString == null || versionAsString.isEmpty()) {
            throw new InvalidVersionException("Version must not be null or empty");
        }
        if (!versionAsString.matches("[0-9]+(\\.[0-9]+)*")) {
            throw new InvalidVersionException("Invalid version format. version=" + versionAsString);
        }
    }

    public static boolean isValid(String versionAsString) {
        try {
            validate(versionAsString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Getter
    private final String versionAsString;

    public Version(String versionAsString) {
        validate(versionAsString);
        this.versionAsString = versionAsString;
    }

    public boolean below(String other) {
        return below(new Version(other));
    }

    public boolean below(Version other) {
        return compareTo(other) < 0;
    }

    public boolean belowOrEqual(Version other) {
        return compareTo(other) <= 0;
    }

    public boolean above(String other) {
        return above(new Version(other));
    }

    public boolean above(Version other) {
        return compareTo(other) > 0;
    }

    public boolean aboveOrEqual(Version other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Version other) {
        if (other == null) {
            return 1;
        }
        var thisParts = getVersionAsString().split("\\.");
        var thatParts = other.getVersionAsString().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (that == null)
            return false;
        if (getClass() != that.getClass())
            return false;
        return compareTo((Version) that) == 0;
    }

    @Override
    public String toString() {
        return versionAsString;
    }
}