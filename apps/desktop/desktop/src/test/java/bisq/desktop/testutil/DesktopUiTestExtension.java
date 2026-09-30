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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.testfx.framework.junit5.ApplicationExtension;

/**
 * Defers robot creation until after JUnit has checked the UI test's execution condition.
 * Declarative extensions are constructed even for disabled classes, so registering TestFX's
 * ApplicationExtension directly would create a robot before the opt-in check.
 */
public class DesktopUiTestExtension implements TestInstancePostProcessor, BeforeEachCallback,
        AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(DesktopUiTestExtension.class);

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        getDelegate(context).postProcessTestInstance(testInstance, context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        getDelegate(context).beforeEach(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        getDelegate(context).afterEach(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        return getDelegate(context).supportsParameter(parameterContext, context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        return getDelegate(context).resolveParameter(parameterContext, context);
    }

    private ApplicationExtension getDelegate(ExtensionContext context) {
        return context.getStore(NAMESPACE).computeIfAbsent(ApplicationExtension.class,
                key -> new ApplicationExtension(), ApplicationExtension.class);
    }
}
