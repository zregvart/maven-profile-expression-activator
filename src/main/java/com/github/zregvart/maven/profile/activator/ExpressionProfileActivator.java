/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.zregvart.maven.profile.activator;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.mvel2.MVEL;

@Named("property")
@Singleton
public class ExpressionProfileActivator implements ProfileActivator {

    final ProfileActivator defaultActivator;

    private static final class FilteredActivatorsList extends AbstractList<ProfileActivator> {
        private final List<ProfileActivator> defaultActivators;

        private FilteredActivatorsList(final List<ProfileActivator> defaultActivators) {
            this.defaultActivators = defaultActivators;
        }

        @Override
        public ProfileActivator get(final int index) {
            int idx = -1;
            for (final ProfileActivator activator : defaultActivators) {
                if (!(activator instanceof PropertyProfileActivator)) {
                    idx++;
                }

                if (idx == index) {
                    return activator;
                }
            }

            throw new IndexOutOfBoundsException("Accessed" + index + " with size: " + size());
        }

        @Override
        public int size() {
            int size = 0;
            for (final ProfileActivator activator : defaultActivators) {
                if (!(activator instanceof PropertyProfileActivator)) {
                    size++;
                }
            }

            return size;
        }
    }

    @Inject
    public ExpressionProfileActivator(final ProfileSelector selector) {
        this(new PropertyProfileActivator());

        patchProfileSelector(selector);
    }

    ExpressionProfileActivator(final ProfileActivator activator) {
        defaultActivator = activator;
    }

    @Override
    public boolean isActive(final Profile profile, final ProfileActivationContext context, final ModelProblemCollector problems) {
        if (!shouldEvaluateExpression(profile.getActivation())) {
            // if the name doesn't start with "expression:" delegate to the
            // default implementation
            return defaultActivator.isActive(profile, context, problems);
        }

        final Activation activation = profile.getActivation();

        final ActivationProperty property = activation.getProperty();

        final String expression = property.getValue();

        final Map<String, Object> vars = new HashMap<>();
        vars.putAll(context.getSystemProperties());
        vars.putAll(context.getProjectProperties());
        vars.putAll(context.getUserProperties());
        vars.put("$profile", profile);
        // allow properties, that are not valid MVEL identifiers to be accessed by $vars['my.property']
        vars.put("$vars", vars);
        // add $basedir
        vars.put("$basedir", context.getProjectDirectory().getAbsolutePath());

        return MVEL.evalToBoolean(expression, context, vars);
    }

    @Override
    public boolean presentInConfig(final Profile profile, final ProfileActivationContext context, final ModelProblemCollector problems) {
        if (profile == null) {
            // should not ever happen, but well
            return false;
        }

        return shouldEvaluateExpression(profile.getActivation()) || defaultActivator.presentInConfig(profile, context, problems);
    }

    static boolean shouldEvaluateExpression(final Activation activation) {
        if (activation == null) {
            return false;
        }

        final ActivationProperty property = activation.getProperty();
        if (property == null) {
            return false;
        }

        final String name = property.getName();
        if (name == null) {
            return false;
        }

        final String value = property.getValue();

        final boolean canConsider = name.trim().startsWith("expression:") && value != null && !value.trim().isEmpty();

        return canConsider;
    }

    /**
     * Patches the activators field of {@link DefaultProfileSelector} to remove
     * the default {@link PropertyProfileActivator}. This is needed as now the
     * two {@link ProfileActivator}s will try to handle the property, and unless
     * the {@link PropertyProfileActivator} arrives to the same conclusion
     * weather
     * {@link ProfileActivator#isActive(Profile, ProfileActivationContext, ModelProblemCollector)}
     * returns the same as this class profile activation becomes erratic.
     */
    private static void patchProfileSelector(final ProfileSelector selector) {
        try {
            final Field activators = DefaultProfileSelector.class.getDeclaredField("activators");
            activators.setAccessible(true);

            @SuppressWarnings("unchecked")
            final List<ProfileActivator> defaultActivators = (List<ProfileActivator>) activators.get(selector);
            activators.set(selector, new FilteredActivatorsList(defaultActivators));
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Unable to subvert the `activators` field of DefaultProfileSelector in order to remove the default PropertyProfileActivator which messes up my style",
                e);
        }
    }

}
