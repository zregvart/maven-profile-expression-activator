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

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenOption;
import com.soebes.itf.jupiter.extension.MavenOptions;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import static com.github.zregvart.maven.profile.activator.Helper.assertActiveProfilesFrom;
import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

@MavenJupiterExtension
public class DefaultBehaviourIT {

    @MavenTest
    @MavenGoal("help:active-profiles")
    @MavenOptions({
        @MavenOption("-Dset"),
        @MavenOption("-Dvalue=expected")
    })
    public void default_property_activation(final MavenExecutionResult result) {
        assertThat(result).isSuccessful();
        assertActiveProfilesFrom(result).containsOnly("active-by-setting", "active-by-value", "active-by-value-not-matching", "active-by-property-not-present");
    }

}
