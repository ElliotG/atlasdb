/**
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.containers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ContainerTest {
    @Parameterized.Parameters(name = "With container {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                { new CassandraContainer() },
                { new ThreeNodeCassandraCluster() }
        });
    }

    private final Container container;

    public ContainerTest(Container container) {
        this.container = container;
    }

    @Test
    public void dockerComposeFileShouldExist() {
        assertThat(ContainerTest.class.getResource(container.getDockerComposeFile())).isNotNull();
    }
}
