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
package com.netflix.conductor.postgres.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.core.exception.ApplicationException;
import com.netflix.conductor.dao.EventHandlerDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class PostgresEventHandlerDAO extends PostgresBaseDAO implements EventHandlerDAO {

    public PostgresEventHandlerDAO() { super(); }

    public PostgresEventHandlerDAO(ObjectMapper objectMapper, DataSource dataSource, PostgresProperties properties) {
        super(objectMapper, dataSource, properties);
    }

    @Override
    public void addEventHandler(EventHandler eventHandler) {
        Preconditions.checkNotNull(eventHandler.getName(), "EventHandler name cannot be null");

        final String INSERT_EVENT_HANDLER_QUERY = "INSERT INTO meta_event_handler (name, event, active, json_data) " +
                "VALUES (?, ?, ?, ?)";

        withTransaction(tx -> {
            if (getEventHandler(tx, eventHandler.getName()) != null) {
                throw new ApplicationException(ApplicationException.Code.CONFLICT,
                        "EventHandler with name " + eventHandler.getName() + " already exists!");
            }

            execute(tx, INSERT_EVENT_HANDLER_QUERY, q -> q.addParameter(eventHandler.getName())
                    .addParameter(eventHandler.getEvent())
                    .addParameter(eventHandler.isActive())
                    .addJsonParameter(eventHandler)
                    .executeUpdate());
        });
    }

    @Override
    public void updateEventHandler(EventHandler eventHandler) {
        Preconditions.checkNotNull(eventHandler.getName(), "EventHandler name cannot be null");

        //@formatter:off
        final String UPDATE_EVENT_HANDLER_QUERY = "UPDATE meta_event_handler SET " +
                "event = ?, active = ?, json_data = ?, " +
                "modified_on = CURRENT_TIMESTAMP WHERE name = ?";
        //@formatter:on

        withTransaction(tx -> {
            EventHandler existing = getEventHandler(tx, eventHandler.getName());
            if (existing == null) {
                throw new ApplicationException(ApplicationException.Code.NOT_FOUND,
                        "EventHandler with name " + eventHandler.getName() + " not found!");
            }

            execute(tx, UPDATE_EVENT_HANDLER_QUERY, q -> q.addParameter(eventHandler.getEvent())
                    .addParameter(eventHandler.isActive())
                    .addJsonParameter(eventHandler)
                    .addParameter(eventHandler.getName())
                    .executeUpdate());
        });
    }

    @Override
    public void removeEventHandler(String name) {
        final String DELETE_EVENT_HANDLER_QUERY = "DELETE FROM meta_event_handler WHERE name = ?";

        withTransaction(tx -> {
            EventHandler existing = getEventHandler(tx, name);
            if (existing == null) {
                throw new ApplicationException(ApplicationException.Code.NOT_FOUND,
                        "EventHandler with name " + name + " not found!");
            }

            execute(tx, DELETE_EVENT_HANDLER_QUERY, q -> q.addParameter(name).executeDelete());
        });
    }

    @Override
    public List<EventHandler> getAllEventHandlers() {
        final String READ_ALL_EVENT_HANDLER_QUERY = "SELECT json_data FROM meta_event_handler";
        return queryWithTransaction(READ_ALL_EVENT_HANDLER_QUERY, q -> q.executeAndFetch(EventHandler.class));
    }

    @Override
    public List<EventHandler> getEventHandlersForEvent(String event, boolean activeOnly) {
        final String READ_ALL_EVENT_HANDLER_BY_EVENT_QUERY = "SELECT json_data FROM meta_event_handler WHERE event = ?";
        return queryWithTransaction(READ_ALL_EVENT_HANDLER_BY_EVENT_QUERY, q -> {
            q.addParameter(event);
            return q.executeAndFetch(rs -> {
                List<EventHandler> handlers = new ArrayList<>();
                while (rs.next()) {
                    EventHandler h = readValue(rs.getString(1), EventHandler.class);
                    if (!activeOnly || h.isActive()) {
                        handlers.add(h);
                    }
                }

                return handlers;
            });
        });
    }

    /**
     * Retrieve a {@link EventHandler} by {@literal name}.
     *
     * @param connection The {@link Connection} to use for queries.
     * @param name       The {@code EventHandler} name to look for.
     * @return {@literal null} if nothing is found, otherwise the {@code EventHandler}.
     */
    private EventHandler getEventHandler(Connection connection, String name) {
        final String READ_ONE_EVENT_HANDLER_QUERY = "SELECT json_data FROM meta_event_handler WHERE name = ?";

        return query(connection, READ_ONE_EVENT_HANDLER_QUERY,
                q -> q.addParameter(name).executeAndFetchFirst(EventHandler.class));
    }
}
