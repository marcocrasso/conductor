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
package com.netflix.conductor.spi;

import com.netflix.conductor.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.*;

/**
 * This component deals with SPI configuration. The goal is to allow configuring DAOs providers using one property and
 * individual ones optionally to override one specific DAO. For example:
 * <p>
 * conductor.db.type = mysql
 * <p>
 * has the same result than configuring each SPI with the expected MySQL class, e.g.:
 * <p>
 * com.netflix.conductor.dao.ExecutionDAO=com.netflix.conductor.mysql.dao.MySQLExecutionDAO
 * com.netflix.conductor.dao.EventHandlerDAO=com.netflix.conductor.mysql.dao.MySQLEventHandlerDAO
 * com.netflix.conductor.dao.MetadataDAO=com.netflix.conductor.mysql.dao.MySQLMetadataDAO
 * com.netflix.conductor.dao.PollDataDAO=com.netflix.conductor.mysql.dao.MySQLPollDataDAO
 * com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.mysql.dao.MySQLQueueDAO
 * com.netflix.conductor.dao.RateLimitingDAO=com.netflix.conductor.mysql.dao.MySQLRateLimitingDAO
 * <p>
 * while:
 * <p>
 * conductor.db.type = mysql
 * com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.redis.dao.DynoQueueDAO
 * <p>
 * will combine 5 DAOs from MySQL implementation with the Dynoqueue one for the queue.
 *
 * @author marco.crasso@invitae.com mcrasso@marsie.io
 */
@Component("BackwardCompatibleConfiguration")
@ConfigurationProperties(prefix = "spi.bundled")
public class SPIBundledProperties {

    private static final Logger log = LoggerFactory.getLogger(SPIBundledProperties.class);

    /**
     * Internal structure, which is loaded once at start
     */
    Map<String, Map<String, String>> spiByDao;

    @Value("${conductor.db.type:}")
    private String dbType;

    @Autowired
    private Environment env;

    /**
     * db types implementing DAOs
     *
     * @see <a href="https://github.com/Netflix/conductor/issues/2017">issue 2017</a>
     */
    private String[] types = {"cassandra", "dynomite", "mysql", "dynomite", "memory", "redis_cluster", "redis_sentinel", "redis_standalone"};

    /**
     * the Service Interfaces that must be fulfilled with a Service Provider
     */
    private String[] serviceProvidersInterfaces = {ExecutionDAO.class.getName(), MetadataDAO.class.getName(),
            EventHandlerDAO.class.getName(), PollDataDAO.class.getName(), RateLimitingDAO.class.getName(),
            QueueDAO.class.getName()};

    public SPIBundledProperties() {
        spiByDao = new HashMap<>();
    }

    /**
     * Initializes system configuration, by expanding expected SPI based on the properties files and conductor.db.type.
     * This must be run after dependency injection happens.
     */
    @PostConstruct
    void initInternalState() {
        for (String type : types) {
            spiByDao.put(type, new HashMap<>());

            for (String key : serviceProvidersInterfaces) {
                final String userValue = env.getProperty("spi.bundled" + '.' + type + '.' + key);
                if (userValue != null) {
                    spiByDao.get(type).put(key, userValue);
                }
            }

        }
        if (!spiByDao.values().isEmpty())
            log.debug("Automatically configured Services Providers Interfaces types with: {}", this.toString());

        if (!dbType.isEmpty()) {
            for (String serviceProviderInterface : serviceProvidersInterfaces) {
                if (!System.getProperties().contains(serviceProviderInterface) && !env.containsProperty(serviceProviderInterface)) {
                    final String userValue = getServiceProviderClassName(serviceProviderInterface);
                    Assert.notNull(userValue, MessageFormat.format("Check your configuration for key {0}",
                            "spi.bundled" + '.' + dbType + '.' + serviceProviderInterface));
                    if (!userValue.isEmpty()) {
                        log.info("Setting {} with {}", serviceProviderInterface, userValue);
                        System.setProperty(serviceProviderInterface, userValue);
                    }
                }
            }
        }
    }

    public String getServiceProviderClassName(String serviceInterface) {
        Assert.notNull(dbType, MessageFormat.format("Services provider type cannot be null. Check your configuration key {0}.", "conductor.db.type"));
        Assert.hasText(dbType, MessageFormat.format("Services provider type cannot be empty. Make sure your configuration key {0} has a valid classname.", "conductor.db.type"));
        final String r = spiByDao.get(dbType).get(serviceInterface);
        Assert.notNull(r, MessageFormat.format("Service provider implementation cannot be null for {0}, check your configuration key spi.bundled.{1}.{0}", serviceInterface, dbType));
        return r;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public String[] getServiceProvidersInterfaces() {
        return serviceProvidersInterfaces;
    }

    public void setServiceProvidersInterfaces(String[] serviceProvidersInterfaces) {
        this.serviceProvidersInterfaces = serviceProvidersInterfaces;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = spiByDao.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            sb.append('\n').append(key);
            spiByDao.get(key).entrySet().forEach(e -> sb.append('\n').append('\t').append(e.getKey()).append(e.getValue()));
        }
        return sb.toString();
    }
}
