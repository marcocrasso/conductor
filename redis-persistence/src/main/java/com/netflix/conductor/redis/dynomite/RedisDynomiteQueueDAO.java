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
package com.netflix.conductor.redis.dynomite;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.redis.config.DynomiteClusterConfiguration;
import com.netflix.conductor.redis.config.RedisClusterConfiguration;
import com.netflix.conductor.redis.config.RedisCommonConfiguration;
import com.netflix.conductor.redis.config.RedisProperties;
import com.netflix.conductor.redis.dao.DynoQueueDAO;

public class RedisDynomiteQueueDAO extends DynoQueueDAO {

    public RedisDynomiteQueueDAO() {
        super();
        this.configurator = new DynomiteClusterConfiguration();
    }

    public RedisDynomiteQueueDAO(ConductorProperties conductorProperties, RedisProperties properties) {
        this.configuration = configuration;
        this.conductorProperties = conductorProperties;
        this.properties = properties;
        this.configurator = new DynomiteClusterConfiguration();
        init();
    }

}
