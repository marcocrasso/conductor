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
package com.netflix.conductor.redis.config;

import com.google.inject.ProvisionException;
import com.netflix.conductor.redis.dynoqueue.RedisQueuesShardingStrategyProvider;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.connectionpool.TokenMapSupplier;
import com.netflix.dyno.connectionpool.impl.lb.HostToken;
import com.netflix.dyno.connectionpool.impl.utils.CollectionUtils;
import com.netflix.dyno.queues.ShardSupplier;
import com.netflix.dyno.queues.redis.RedisQueues;
import com.netflix.dyno.queues.redis.sharding.ShardingStrategy;
import com.netflix.dyno.queues.shard.DynoShardSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.commands.JedisCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisCommonConfiguration {

    public static final String DEFAULT_CLIENT_INJECTION_NAME = "DefaultJedisCommands";
    public static final String READ_CLIENT_INJECTION_NAME = "ReadJedisCommands";

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCommonConfiguration.class);

    public ShardSupplier shardSupplier(HostSupplier hostSupplier, RedisProperties properties) {
        if (properties.getAvailabilityZone() == null) {
            throw new ProvisionException(
                "Availability zone is not defined.  Ensure Configuration.getAvailabilityZone() returns a non-null " +
                    "and non-empty value."
            );
        }
        String localDC = properties.getAvailabilityZone().replaceAll(properties.getDataCenterRegion(), "");
        return new DynoShardSupplier(hostSupplier, properties.getDataCenterRegion(), localDC);
    }

    public static TokenMapSupplier tokenMapSupplier() {
        final List<HostToken> hostTokens = new ArrayList<>();
        return new TokenMapSupplier() {
            @Override
            public List<HostToken> getTokens(Set<Host> activeHosts) {
                long i = activeHosts.size();
                for (Host host : activeHosts) {
                    HostToken hostToken = new HostToken(i, host);
                    hostTokens.add(hostToken);
                    i--;
                }
                return hostTokens;
            }

            @Override
            public HostToken getTokenForHost(Host host, Set<Host> activeHosts) {
                return CollectionUtils.find(hostTokens, token -> token.getHost().compareTo(host) == 0);
            }
        };
    }

    public ShardingStrategy shardingStrategy(ShardSupplier shardSupplier, RedisProperties properties) {
        return new RedisQueuesShardingStrategyProvider(shardSupplier, properties).get();
    }

    public RedisQueues redisQueues(@Qualifier(DEFAULT_CLIENT_INJECTION_NAME) JedisCommands jedisCommands,
        @Qualifier(READ_CLIENT_INJECTION_NAME) JedisCommands jedisCommandsRead,
        ShardSupplier shardSupplier, RedisProperties properties, ShardingStrategy shardingStrategy) {
        RedisQueues queues = new RedisQueues(jedisCommands, jedisCommandsRead, properties.getQueuePrefix(),
            shardSupplier,
            60_000, 60_000, shardingStrategy);
        LOGGER.info("DynoQueueDAO initialized with prefix " + properties.getQueuePrefix() + "!");
        return queues;
    }
}
