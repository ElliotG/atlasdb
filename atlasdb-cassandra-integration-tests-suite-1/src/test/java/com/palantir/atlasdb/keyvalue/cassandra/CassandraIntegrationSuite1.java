/**
 * Copyright 2015 Palantir Technologies
 * <p>
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://opensource.org/licenses/BSD-3-Clause
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.keyvalue.cassandra;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        CassandraKeyValueServiceTransactionIntegrationTest.class,
        CassandraKeyValueServiceSerializableTransactionIntegrationTest.class,
        CassandraClientPoolIntegrationTest.class,
        CassandraConnectionIntegrationTest.class,
        CassandraTimestampIntegrationTest.class,
        CassandraKeyValueServiceIntegrationTest.class,
})
public class CassandraIntegrationSuite1 {

    @ClassRule
    public static final RuleChain SETUP = CassandraTestSuite.CASSANDRA_DOCKER_SET_UP;

    @BeforeClass
    public static void foo() throws Exception {
        CassandraTestSuite.waitUntilCassandraIsUp();
    }
}