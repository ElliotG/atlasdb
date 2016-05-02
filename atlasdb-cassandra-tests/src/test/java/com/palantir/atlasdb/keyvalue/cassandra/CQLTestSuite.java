/**
 * Copyright 2015 Palantir Technologies
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
package com.palantir.atlasdb.keyvalue.cassandra;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceConfig;
import com.palantir.atlasdb.cassandra.ImmutableCassandraKeyValueServiceConfig;

@RunWith(Suite.class)
@SuiteClasses({
        CQLKeyValueServiceSerializableTransactionTest.class,
        CQLKeyValueServiceTransactionTest.class,
        CQLKeyValueServiceSweeperTest.class
})
@Ignore
public class CQLTestSuite {
    
    static CassandraKeyValueServiceConfig CQLKVS_CONFIG = ImmutableCassandraKeyValueServiceConfig.builder()
            .addServers(new InetSocketAddress("localhost", 9042))
            .poolSize(20)
            .keyspace("atlasdb")
            .ssl(false)
            .replicationFactor(1)
            .mutationBatchCount(10000)
            .mutationBatchSizeBytes(10000000)
            .fetchBatchCount(1000)
            .autoRefreshNodes(false)
            .build();

    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        CassandraService.start(CassandraService.DEFAULT_CONFIG);
    }

    @AfterClass
    public static void stop() throws IOException {
        CassandraService.stop();
    }

}
