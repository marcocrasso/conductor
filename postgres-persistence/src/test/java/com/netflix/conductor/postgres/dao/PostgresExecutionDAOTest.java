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
package com.netflix.conductor.postgres.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.config.ObjectMapperConfiguration;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.ExecutionDAOTest;
import com.netflix.conductor.postgres.util.PostgresDAOTestUtil;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ContextConfiguration(classes = {ObjectMapperConfiguration.class})
@RunWith(SpringRunner.class)
public class PostgresExecutionDAOTest extends ExecutionDAOTest {

    private PostgresDAOTestUtil testUtil;

    private PostgresExecutionDAO executionDAO;

    @Autowired
    private ObjectMapper objectMapper;

    @Rule
    public TestName name = new TestName();

    public PostgreSQLContainer<?> postgreSQLContainer;

    @Before
    public void setup() {
        postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres")).withDatabaseName(name.getMethodName().toLowerCase());
        postgreSQLContainer.start();
        testUtil = new PostgresDAOTestUtil(postgreSQLContainer, objectMapper, name.getMethodName().toLowerCase());
        testUtil.resetAllData();
        executionDAO = new PostgresExecutionDAO(testUtil.getObjectMapper(), testUtil.getDataSource(), testUtil.getTestProperties());
    }

    @After
    public void teardown() {
        testUtil.resetAllData();
        testUtil.getDataSource().close();
    }

    @Test
    public void testPendingByCorrelationId() {

        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        Workflow workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        generateWorkflows(workflow, 10);

        List<Workflow> bycorrelationId = getExecutionDAO()
            .getWorkflowsByCorrelationId("pending_count_correlation_jtest", "corr001", true);
        assertNotNull(bycorrelationId);
        assertEquals(10, bycorrelationId.size());
    }

    @Test
    public void testRemoveWorkflow() {
        WorkflowDef def = new WorkflowDef();
        def.setName("workflow");

        Workflow workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        List<String> ids = generateWorkflows(workflow, 1);

        assertEquals(1, getExecutionDAO().getPendingWorkflowCount("workflow"));
        ids.forEach(wfId -> getExecutionDAO().removeWorkflow(wfId));
        assertEquals(0, getExecutionDAO().getPendingWorkflowCount("workflow"));
    }

    @Override
    public ExecutionDAO getExecutionDAO() {
        return executionDAO;
    }
}
