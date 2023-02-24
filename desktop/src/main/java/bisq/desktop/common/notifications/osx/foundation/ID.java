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
package bisq.desktop.common.notifications.osx.foundation;

import com.sun.jna.NativeLong;
/**
 * Based on https://github.com/joewalnes/idea-community/blob/master/platform/util/src/com/intellij/ui/mac/foundation/ID.java
 */

/**
 * Could be an address in memory (if pointer to a class or method) or a value (like 0 or 1)
 */
public class ID extends NativeLong {
  public ID() {
  }

  public ID(long peer) {
    super(peer);
  }

  public static final ID NIL = new ID(0L);

  public boolean booleanValue() {
    return intValue() != 0;
  }
}
