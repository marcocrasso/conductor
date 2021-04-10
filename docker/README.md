# Docker

This folder contains docker-compose services manifests to run Conductor, a front-end, and required infrastructure. Next
instructions are based on docker-compose. In general, services will be started in background with `docker-compose up -d [service]`,
to terminate it use `docker-compose stop [service]`, and to see its stdout `docker-compose logs [-f] [service]`.

## Building

Every time you change source code and want to test it on Docker, please use:

```
docker-compose build conductor-server
```

## Running

### Minimal setup
The minimal setup required is Conductor server and Elastic Search. The main docker-compose-minimal.yaml file 
is intended to start it. Such infrastructure requires that backend store data in memory.

```
docker-compose -f docker-compose-minimal.yaml up -d
```

In addition to Dynomite, it is possible to use MySQL, Postgres, or Cassandra, and combinations thanks to Java SPI.

### MySQL example

This requires to run a MySQL instance and configure Conductor server. Please check server/config/config-mysql.properties.

```
docker-compose -f docker-compose-mysql.yaml up -d mysql
export CONDUCTOR_CONFIG_FILE=config-mysql.properties
docker-compose up -d conductor-server
```

### MySQL combined with Dynomite for QueueDAO

This can be achieved through properties:

```
# Database persistence type.
conductor.db.type=mysql
# Overrides queue to use Dynomite instead of MySQL one
com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.redis.dao.DynoQueueDAO
```

assuming MySQL is still up and running, just hit:

```
export CONDUCTOR_CONFIG_FILE=config-mysql-dynoqueue.properties
docker-compose up -d conductor-server
```

### Postgres example

This requires to run a Postgres instance and configure Conductor server. Please check server/config/config-postgres.properties.

```
docker-compose -f docker-compose-mysql.yaml up -d mysql
export CONDUCTOR_CONFIG_FILE=config-mysql.properties
docker-compose up -d conductor-server
```

### Cassandra and MySQL example

At the moment of writing this, cassandra-persistence module partially implements the backend. Therefore, you need to 
combine it with something else. To combine it with MySQL, follow the recipe:

First, start Cassandra. This example uses one instance, but if you want you could setup a cluster.

```
docker-compose -f docker-compose-cassandra.yaml up -d cassandra1
```

```
export CONDUCTOR_CONFIG_FILE=config-cassandra-mysql.properties
docker-compose up -d conductor-server
```

### The front-end

The UI component can be built and started with docker-compose too. 

``` 
docker-compose build conductor-ui
```

```
docker-compose up -d conductor-ui
```