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


package bisq.desktop.testutil;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class which drives mouse or keyboard input with a TestFX {@code FxRobot}.
 * <p>
 * A robot on a real desktop moves the real pointer and types into whichever window has focus, and on macOS
 * it needs the Accessibility permission for the application which runs the tests. The {@code test} task
 * therefore excludes the {@code ui} tag. These tests also require {@code bisq.uiTest=true}, so a runner
 * which ignores Gradle's tag filter cannot start a robot. The {@code uiTest} task enables them and uses
 * Monocle by default (see {@code apps/desktop/desktop/build.gradle.kts}).
 * <p>
 * Tests which only need the JavaFX application thread use {@link JavaFxToolkitExtension} instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("ui")
@EnabledIfSystemProperty(named = "bisq.uiTest", matches = "true")
@ExtendWith({JavaFxToolkitExtension.class, DesktopUiTestExtension.class})
public @interface DesktopUiTest {
}
