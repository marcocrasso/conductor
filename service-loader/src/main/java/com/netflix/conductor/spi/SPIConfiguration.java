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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * This component combines Java SPI with Spring-boot beans. Admin should tell which SPI classname wants to register, and
 * this component will register it as a Bean.
 *
 * @author marco.crasso@invitae.com mcrasso@marsie.io
 */
@Configuration("spiServiceLoaderConfiguration")
@EnableConfigurationProperties(SPIBundledProperties.class)
@PropertySource("classpath:/spi.bundled.properties")
@DependsOn("BackwardCompatibleConfiguration")
public class SPIConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SPIConfiguration.class);

    @Bean
    public MetadataDAO metadataDAOProvider(@Value("${com.netflix.conductor.dao.MetadataDAO:}") String userValue) throws Exception {
        return loadService(MetadataDAO.class, userValue);
    }

    @Bean
    public QueueDAO queueDAOProvider(@Value("${com.netflix.conductor.dao.QueueDAO:}") String userValue) throws Exception {
        return loadService(QueueDAO.class, userValue);
    }

    @Bean
    public ExecutionDAO executionDAOProvider(@Value("${com.netflix.conductor.dao.ExecutionDAO:}") String userValue) throws Exception {
        return loadService(ExecutionDAO.class, userValue);
    }

    @Bean
    public EventHandlerDAO eventHandlerDAOProvider(@Value("${com.netflix.conductor.dao.EventHandlerDAO:}") String userValue) throws Exception {
        return loadService(EventHandlerDAO.class, userValue);
    }

    @Bean
    public PollDataDAO pollDataDAOProvider(@Value("${com.netflix.conductor.dao.PollDataDAO:}") String userValue) throws Exception {
        return loadService(PollDataDAO.class, userValue);
    }

    @Bean
    public RateLimitingDAO rateLimitingDAOProvider(@Value("${com.netflix.conductor.dao.RateLimitingDAO:}") String userValue) throws Exception {
        return loadService(RateLimitingDAO.class, userValue);
    }

    protected <T> T loadService(Class<T> typeKey, String expectedType) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.notNull(expectedType, MessageFormat.format("Expected SPI type cannot be null. Check your configuration key {0}", typeKey.getName()));
        Assert.hasText(expectedType, MessageFormat.format("Expected SPI type cannot be empty. Check your configuration key {0}", typeKey.getName()));
        try {
            Class.forName(expectedType);
        } catch (ClassNotFoundException e) {
            log.info("Couldn't find class '{}' in the classpath while loading '{}'. Check build.gradle 'dependencies' section", expectedType, typeKey.getName());
            throw new RuntimeException();
        }
        ServiceLoader<T> loader = ServiceLoader.load(typeKey);
        Iterator<T> it = loader.iterator();
        while (it.hasNext()) {
            T candidate = it.next();
            if (candidate.getClass().getName().equals(expectedType)) {
                log.info("Found '{}' providing service named: '{}'.", expectedType, typeKey.getName());
                return candidate;
            }
        }
        throw new RuntimeException(MessageFormat.format("SPI '{0}' is not registered for service named '{1}'. Check META-INF/services declarations", expectedType, typeKey.getName()));
    }
}
