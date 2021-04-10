/**
 * Copyright 2021 Netflix, Inc.
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

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.dao.RateLimitingDAO;

public class PostgresRateLimitingDAO implements RateLimitingDAO {

    public PostgresRateLimitingDAO() {
    }

    /**
     * This is a dummy implementation and this feature is not for Postgres backed Conductor
     *
     * @param task: which needs to be evaluated whether it is rateLimited or not
     */
    @Override
    public boolean exceedsRateLimitPerFrequency(Task task, TaskDef taskDef) {
        return false;
    }
}
