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

import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ExpressionProfileActivatorTest {

    @Test
    public void noProfileShouldResultInNoPresence() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        assertThat(activator.presentInConfig(null, null, null)).isFalse();
    }

    @Test
    public void shouldDelegateToDefault() {
        final ProfileActivator delegate = mock(ProfileActivator.class);

        final ExpressionProfileActivator expressionActivator = new ExpressionProfileActivator(delegate);

        final Profile profile = new Profile();
        final ProfileActivationContext context = new DefaultProfileActivationContext();
        final ModelProblemCollector problems = mock(ModelProblemCollector.class);

        when(delegate.isActive(profile, context, problems)).thenReturn(true);

        assertThat(expressionActivator.isActive(profile, context, problems)).isTrue();

        verify(delegate).isActive(profile, context, problems);
    }

    @Test
    public void shouldDetectPresence() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        final ActivationProperty property = new ActivationProperty();
        property.setName("expression:");
        property.setValue("true || true");

        final Activation activation = new Activation();
        activation.setProperty(property);
        final Profile profile = new Profile();
        profile.setActivation(activation);

        assertThat(activator.presentInConfig(profile, null, null)).isTrue();
    }

    @Test
    public void shouldEvaluateExpressions() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        final Profile profile = new Profile();
        final Activation activation = new Activation();
        final ActivationProperty property = new ActivationProperty();
        property.setName("expression:test");
        property.setValue("a == 'system' && b == 'project' && c == 'user'");
        activation.setProperty(property);
        profile.setActivation(activation);

        final DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        // lowest priority is overridden by project and user properties
        context.setSystemProperties(abc("system", "system", "system"));
        // mid priority, overrides project is overridden by user properties
        context.setProjectProperties(abc(null, "project", "project"));
        // high priority, overrides project and system properties
        context.setUserProperties(abc(null, null, "user"));

        final ModelProblemCollector problems = mock(ModelProblemCollector.class);

        assertThat(activator.isActive(profile, context, problems)).isTrue();

        verifyNoInteractions(problems);
    }

    @Test
    public void shouldNotDetectPresenceWhenEmptyActivationIsProvided() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        final Profile profile = new Profile();
        profile.setActivation(new Activation());

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();
    }

    @Test
    public void shouldNotDetectPresenceWhenNoActivationIsProvided() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        assertThat(activator.presentInConfig(new Profile(), null, null)).isFalse();
    }

    @Test
    public void shouldNotDetectPresenceWhenNoPropertyActivationIsProvided() {
        final ExpressionProfileActivator activator = new ExpressionProfileActivator(new DefaultProfileSelector());

        final Activation activation = new Activation();
        activation.setOs(new ActivationOS());
        final Profile profile = new Profile();
        profile.setActivation(activation);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();
    }

    @Test
    public void shouldNotDetectPresenceWhenPropertyActivationHasAnEmptyName() {
        final ProfileActivator defaultActivator = mock(ProfileActivator.class);

        final ExpressionProfileActivator activator = new ExpressionProfileActivator(defaultActivator);

        final ActivationProperty property = new ActivationProperty();
        property.setName("");

        final Activation activation = new Activation();
        activation.setProperty(property);
        final Profile profile = new Profile();
        profile.setActivation(activation);

        when(defaultActivator.presentInConfig(profile, null, null)).thenReturn(false);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();

        verify(defaultActivator).presentInConfig(profile, null, null);
    }

    @Test
    public void shouldNotDetectPresenceWhenPropertyActivationHasAnEmptyValue() {
        final ProfileActivator defaultActivator = mock(ProfileActivator.class);

        final ExpressionProfileActivator activator = new ExpressionProfileActivator(defaultActivator);

        final ActivationProperty property = new ActivationProperty();
        property.setName("expression:");

        final Activation activation = new Activation();
        activation.setProperty(property);
        final Profile profile = new Profile();
        profile.setActivation(activation);

        when(defaultActivator.presentInConfig(profile, null, null)).thenReturn(false);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();

        verify(defaultActivator).presentInConfig(profile, null, null);
    }

    @Test
    public void shouldNotDetectPresenceWhenPropertyActivationHasAnTrimmedEmptyName() {
        final ProfileActivator defaultActivator = mock(ProfileActivator.class);

        final ExpressionProfileActivator activator = new ExpressionProfileActivator(defaultActivator);

        final ActivationProperty property = new ActivationProperty();
        property.setName("   ");

        final Activation activation = new Activation();
        activation.setProperty(property);
        final Profile profile = new Profile();
        profile.setActivation(activation);

        when(defaultActivator.presentInConfig(profile, null, null)).thenReturn(false);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();

        verify(defaultActivator).presentInConfig(profile, null, null);
    }

    @Test
    public void shouldNotDetectPresenceWhenPropertyActivationHasAnTrimmedEmptyValue() {
        final ProfileActivator defaultActivator = mock(ProfileActivator.class);

        final ExpressionProfileActivator activator = new ExpressionProfileActivator(defaultActivator);

        final ActivationProperty property = new ActivationProperty();
        property.setName("expression:");
        property.setValue("   ");

        final Activation activation = new Activation();
        activation.setProperty(property);
        final Profile profile = new Profile();
        profile.setActivation(activation);

        when(defaultActivator.presentInConfig(profile, null, null)).thenReturn(false);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();

        verify(defaultActivator).presentInConfig(profile, null, null);
    }

    @Test
    public void shouldNotDetectPresenceWhenPropertyActivationIsNotNamed() {
        final ProfileActivator defaultActivator = mock(ProfileActivator.class);

        final ExpressionProfileActivator activator = new ExpressionProfileActivator(defaultActivator);

        final Activation activation = new Activation();
        activation.setProperty(new ActivationProperty());
        final Profile profile = new Profile();
        profile.setActivation(activation);

        when(defaultActivator.presentInConfig(profile, null, null)).thenReturn(false);

        assertThat(activator.presentInConfig(profile, null, null)).isFalse();

        verify(defaultActivator).presentInConfig(profile, null, null);
    }

    private static Properties abc(final String a, final String b, final String c) {
        final Properties properties = new Properties();
        if (a != null) {
            properties.setProperty("a", a);
        }
        if (b != null) {
            properties.setProperty("b", b);
        }
        if (c != null) {
            properties.setProperty("c", c);
        }

        return properties;
    }
}
