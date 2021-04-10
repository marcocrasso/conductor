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
package com.netflix.conductor.test.base;

import com.netflix.conductor.redis.dynomite.RedisDynomiteQueueDAO;
import com.netflix.conductor.redis.jedis.JedisMock;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.queues.ShardSupplier;
import com.netflix.dyno.queues.redis.RedisQueues;
import redis.clients.jedis.commands.JedisCommands;
import spock.mock.DetachedMockFactory;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SpyableQueue extends RedisDynomiteQueueDAO {

    public SpyableQueue() {
        super();
    }

    @Override
    @PostConstruct
    public void init() {
        DetachedMockFactory detachedMockFactory = new DetachedMockFactory();
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
        this.queues = new RedisQueues(jedisMock, jedisMock, "mockedQueues", shardSupplier, 60000, 120000);
    }
}
