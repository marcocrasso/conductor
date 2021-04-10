/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.test.integration.grpc.mysql;

import com.netflix.conductor.client.grpc.MetadataClient;
import com.netflix.conductor.client.grpc.TaskClient;
import com.netflix.conductor.client.grpc.WorkflowClient;
import com.netflix.conductor.test.integration.grpc.AbstractGrpcEndToEndTest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@Import(DataSourceAutoConfiguration.class)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "com.netflix.conductor.dao.ExecutionDAO=com.netflix.conductor.mysql.dao.MySQLExecutionDAO",
    "com.netflix.conductor.dao.EventHandlerDAO=com.netflix.conductor.mysql.dao.MySQLEventHandlerDAO",
    "com.netflix.conductor.dao.MetadataDAO=com.netflix.conductor.mysql.dao.MySQLMetadataDAO",
    "com.netflix.conductor.dao.PollDataDAO=com.netflix.conductor.mysql.dao.MySQLPollDataDAO",
    "com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.mysql.dao.MySQLQueueDAO",
    "com.netflix.conductor.dao.RateLimitingDAO=com.netflix.conductor.mysql.dao.MySQLRateLimitingDAO",
    "conductor.grpc-server.port=8094",
    "spring.datasource.url=jdbc:tc:mysql:///conductor", // "tc" prefix starts the MySql container
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.hikari.maximum-pool-size=8",
    "spring.datasource.hikari.minimum-idle=300000",
    "spring.flyway.locations=classpath:db/migration_mysql",
    "spring.flyway.enabled=true"
})
public class MySQLGrpcEndToEndTest extends AbstractGrpcEndToEndTest {

    @Before
    public void init() {
        taskClient = new TaskClient("localhost", 8094);
        workflowClient = new WorkflowClient("localhost", 8094);
        metadataClient = new MetadataClient("localhost", 8094);
    }
}
