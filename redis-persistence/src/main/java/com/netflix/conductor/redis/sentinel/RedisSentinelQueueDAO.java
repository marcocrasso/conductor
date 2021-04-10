/**
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.redis.sentinel;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.redis.config.InMemoryRedisConfiguration;
import com.netflix.conductor.redis.config.RedisClusterConfiguration;
import com.netflix.conductor.redis.config.RedisCommonConfiguration;
import com.netflix.conductor.redis.config.RedisProperties;
import com.netflix.conductor.redis.dao.DynoQueueDAO;

public class RedisSentinelQueueDAO extends DynoQueueDAO {

    public RedisSentinelQueueDAO() {
        super();
        this.configurator = new InMemoryRedisConfiguration();
    }

    public RedisSentinelQueueDAO(ConductorProperties conductorProperties, RedisProperties properties) {
        this.conductorProperties = conductorProperties;
        this.properties = properties;
        this.configurator = new InMemoryRedisConfiguration();
        init();
    }
}
