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
package com.netflix.conductor.redis.memory;

import com.netflix.conductor.redis.config.InMemoryRedisConfiguration;
import com.netflix.conductor.redis.dao.DynoQueueDAO;
import com.netflix.conductor.redis.jedis.JedisMock;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.queues.ShardSupplier;
import com.netflix.dyno.queues.redis.RedisQueues;
import redis.clients.jedis.commands.JedisCommands;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RedisMemoryQueueDAO extends DynoQueueDAO {

    public RedisMemoryQueueDAO() {
        super();
        this.configurator = new InMemoryRedisConfiguration();
    }

    @Override
    @PostConstruct
    public void init() {
        HostSupplier hs =  configurator.hostSupplier(properties);
        JedisCommands jcmds = configurator.jedisCommands(this.properties,
                this.conductorProperties,
                hs,
                this.tokenMapSupplier);
        JedisCommands rcmds = configurator.readJedisCommands(this.properties,
                this.conductorProperties,
                configurator.hostSupplier(properties),
                this.tokenMapSupplier);

        JedisCommands jedisMock = new JedisMock();
        ShardSupplier shardSupplier = new ShardSupplier() {
            @Override
            public Set<String> getQueueShards() {
                return new HashSet<>(Collections.singletonList("a"));
            }

            @Override
            public String getCurrentShard() {
                return "a";
            }

            @Override
            public String getShardForHost(Host host) {
                return "a";
            }
        };

        this.queues = new RedisQueues(jedisMock, jedisMock, properties.getQueuePrefix(), shardSupplier, 60000, 120000);

    }
}
