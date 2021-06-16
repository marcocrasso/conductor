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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "conductor.db.type=dynomite"
})
@EnableConfigurationProperties(SPIBundledProperties.class)
@TestPropertySource(locations = "classpath:test.spi.bundled.properties")
public class SPIBundledPropertiesTests {

    @Autowired private SPIBundledProperties configuration;

    /**
     * tests that System properties have been filled based on back compatible conductor.db.type property
     */
    @Test
    public void testPropertiesBeenSet() {
        Assert.assertEquals(System.getProperty(QueueDAO.class.getName()), "com.netflix.conductor.redis.dynomite.RedisDynomiteQueueDAO");
        Assert.assertEquals(System.getProperty(MetadataDAO.class.getName()), "com.netflix.conductor.redis.dynomite.RedisDynomiteMetadataDAO");
        Assert.assertEquals(System.getProperty(ExecutionDAO.class.getName()), "com.netflix.conductor.redis.dynomite.RedisDynomiteExecutionDAO");
        Assert.assertEquals(System.getProperty(EventHandlerDAO.class.getName()), "com.netflix.conductor.redis.dynomite.RedisDynomiteEventHandlerDAO");
        Assert.assertEquals(System.getProperty(PollDataDAO.class.getName()),"com.netflix.conductor.redis.dynomite.RedisDynomitePollDataDAO");
        Assert.assertEquals(System.getProperty(RateLimitingDAO.class.getName()),"com.netflix.conductor.redis.dynomite.RedisDynomiteRateLimitingDAO");
    }
}