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

package bisq.desktop.components.controls.validator;

import bisq.common.util.StringUtils;
import javafx.scene.control.TextInputControl;
import lombok.extern.slf4j.Slf4j;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Paths.get;

@Slf4j
public class DirectoryPathValidator extends ValidatorBase {
    private final boolean allowEmptyString;

    public DirectoryPathValidator(String message) {
        this(message, true);
    }

    public DirectoryPathValidator(String message, boolean allowEmptyString) {
        super(message);
        this.allowEmptyString = allowEmptyString;
    }

    @Override
    protected void eval() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        String path = textField.getText();
        if (allowEmptyString && StringUtils.isEmpty(path)) {
            hasErrors.set(false);
            return;
        }
        try {
            hasErrors.set(StringUtils.isEmpty(path) || !isDirectory(get(path)));
        } catch (Exception e) {
            log.debug("Exception found while validating directory path. " +
                    "Directory path validation will treat this as directory not valid. ", e);
            hasErrors.set(true);
        }
    }
}

