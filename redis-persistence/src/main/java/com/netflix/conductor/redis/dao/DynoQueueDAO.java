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
package com.netflix.conductor.redis.dao;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.redis.config.JedisCommandsConfigurer;
import com.netflix.conductor.redis.config.RedisCommonConfiguration;
import com.netflix.conductor.redis.config.RedisProperties;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.connectionpool.TokenMapSupplier;
import com.netflix.dyno.queues.DynoQueue;
import com.netflix.dyno.queues.Message;
import com.netflix.dyno.queues.ShardSupplier;
import com.netflix.dyno.queues.redis.RedisQueues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import redis.clients.jedis.commands.JedisCommands;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@EnableConfigurationProperties(RedisProperties.class)
public class DynoQueueDAO implements QueueDAO {

    @Autowired
    protected RedisCommonConfiguration configuration;

    @Autowired
    protected RedisProperties properties;

    @Autowired
    protected ConductorProperties conductorProperties;

    protected TokenMapSupplier tokenMapSupplier = RedisCommonConfiguration.tokenMapSupplier();

    protected JedisCommandsConfigurer configurator;

    protected RedisQueues queues;

    public DynoQueueDAO() {
    }

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
        ShardSupplier ss = configuration.shardSupplier(hs, properties);
        queues = configuration.redisQueues(jcmds, rcmds, ss, properties, configuration.shardingStrategy(ss, properties));
    }

    @Override
    public void push(String queueName, String id, long offsetTimeInSecond) {
        push(queueName, id, -1, offsetTimeInSecond);
    }

    @Override
    public void push(String queueName, String id, int priority, long offsetTimeInSecond) {
        Message msg = new Message(id, null);
        msg.setTimeout(offsetTimeInSecond, TimeUnit.SECONDS);
        if (priority >= 0 && priority <= 99) {
            msg.setPriority(priority);
        }
        queues.get(queueName).push(Collections.singletonList(msg));
    }

    @Override
    public void push(String queueName, List<com.netflix.conductor.core.events.queue.Message> messages) {
        List<Message> msgs = messages.stream()
            .map(msg -> {
                Message m = new Message(msg.getId(), msg.getPayload());
                if (msg.getPriority() > 0) {
                    m.setPriority(msg.getPriority());
                }
                return m;
            })
            .collect(Collectors.toList());
        queues.get(queueName).push(msgs);
    }

    @Override
    public boolean pushIfNotExists(String queueName, String id, long offsetTimeInSecond) {
        return pushIfNotExists(queueName, id, -1, offsetTimeInSecond);
    }

    @Override
    public boolean pushIfNotExists(String queueName, String id, int priority, long offsetTimeInSecond) {
        DynoQueue queue = queues.get(queueName);
        if (queue.get(id) != null) {
            return false;
        }
        Message msg = new Message(id, null);
        if (priority >= 0 && priority <= 99) {
            msg.setPriority(priority);
        }
        msg.setTimeout(offsetTimeInSecond, TimeUnit.SECONDS);
        queue.push(Collections.singletonList(msg));
        return true;
    }

    @Override
    public List<String> pop(String queueName, int count, int timeout) {
        List<Message> msg = queues.get(queueName).pop(count, timeout, TimeUnit.MILLISECONDS);
        return msg.stream()
            .map(Message::getId)
            .collect(Collectors.toList());
    }

    @Override
    public List<com.netflix.conductor.core.events.queue.Message> pollMessages(String queueName, int count,
        int timeout) {
        List<Message> msgs = queues.get(queueName).pop(count, timeout, TimeUnit.MILLISECONDS);
        return msgs.stream()
            .map(msg -> new com.netflix.conductor.core.events.queue.Message(msg.getId(), msg.getPayload(), null,
                msg.getPriority()))
            .collect(Collectors.toList());
    }

    @Override
    public void remove(String queueName, String messageId) {
        queues.get(queueName).remove(messageId);
    }

    @Override
    public int getSize(String queueName) {
        return (int) queues.get(queueName).size();
    }

    @Override
    public boolean ack(String queueName, String messageId) {
        return queues.get(queueName).ack(messageId);

    }

    @Override
    public boolean setUnackTimeout(String queueName, String messageId, long timeout) {
        return queues.get(queueName).setUnackTimeout(messageId, timeout);
    }

    @Override
    public void flush(String queueName) {
        DynoQueue queue = queues.get(queueName);
        if (queue != null) {
            queue.clear();
        }
    }

    @Override
    public Map<String, Long> queuesDetail() {
        return queues.queues().stream()
            .collect(Collectors.toMap(DynoQueue::getName, DynoQueue::size));
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> queuesDetailVerbose() {
        return queues.queues().stream()
            .collect(Collectors.toMap(DynoQueue::getName, DynoQueue::shardSizes));
    }

    public void processUnacks(String queueName) {
        queues.get(queueName).processUnacks();
    }

    @Override
    public boolean resetOffsetTime(String queueName, String id) {
        DynoQueue queue = queues.get(queueName);
        return queue.setTimeout(id, 0);
    }

    @Override
    public boolean containsMessage(String queueName, String messageId) {
        DynoQueue queue = queues.get(queueName);
        Message message = queue.get(messageId);
        return Objects.nonNull(message);
    }
}
