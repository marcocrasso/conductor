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
package com.netflix.conductor.test.integration.grpc.postgres;

import com.netflix.conductor.client.grpc.MetadataClient;
import com.netflix.conductor.client.grpc.TaskClient;
import com.netflix.conductor.client.grpc.WorkflowClient;
import com.netflix.conductor.test.integration.grpc.AbstractGrpcEndToEndTest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "com.netflix.conductor.dao.ExecutionDAO=com.netflix.conductor.postgres.dao.PostgresExecutionDAO",
    "com.netflix.conductor.dao.EventHandlerDAO=com.netflix.conductor.postgres.dao.PostgresEventHandlerDAO",
    "com.netflix.conductor.dao.MetadataDAO=com.netflix.conductor.postgres.dao.PostgresMetadataDAO",
    "com.netflix.conductor.dao.PollDataDAO=com.netflix.conductor.postgres.dao.PostgresPollDataDAO",
    "com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.postgres.dao.PostgresQueueDAO",
    "com.netflix.conductor.dao.RateLimitingDAO=com.netflix.conductor.postgres.dao.PostgresRateLimitingDAO",
    "conductor.grpc-server.port=8098",
    "spring.datasource.url=jdbc:tc:postgresql:///conductor", // "tc" prefix starts the Postgres container
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.datasource.hikari.maximum-pool-size=8",
    "spring.datasource.hikari.minimum-idle=300000",
    "spring.flyway.locations=classpath:db/migration_postgres",
    "spring.flyway.enabled=true"
})
public class PostgresGrpcEndToEndTest extends AbstractGrpcEndToEndTest {

    @Before
    public void init() {
        taskClient = new TaskClient("localhost", 8098);
        workflowClient = new WorkflowClient("localhost", 8098);
        metadataClient = new MetadataClient("localhost", 8098);
    }
}
