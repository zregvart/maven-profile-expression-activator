/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.zregvart.maven.profile.activator;

import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;

import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

final class Helper {

    private Helper() {
        // integration test helper
    }

    static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertActiveProfilesFrom(final MavenExecutionResult result) {
        return assertThat(result).out().plain()
            // active profiles are listed with " - " at the start
            .filteredOn(l -> l.startsWith(" - "))
            // extract the profile name from string like " - active-by-setting
            // (source: ..."
            .extracting(l -> l.replaceAll("^ - (.*) \\(.*$", "$1"));
    }
}
