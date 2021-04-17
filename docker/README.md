# Docker

## Building

Every time you change source code and want to test it on Docker, please use:

```
docker-compose build conductor-server
```

## Setups 

This folder contains docker-compose services manifests to run Conductor, a front-end, and required infrastructure. The 
infra-structure requirements will vary from one setup to another. Each setup is accompanied by one config-[setup].properties 
file inside _server/config_ folder. The setups are driven by the storage technology, in this sense it is possible to use
main memory, Dynomite, MySQL, Postgres, or Cassandra, and combinations.


Next instructions are based on docker-compose. In general, a service will start in background with `docker-compose up -d [service]`,
to terminate it use `docker-compose stop [service]`, and to see its stdout `docker-compose logs [-f] [service]`.

### Minimal setup
The minimal setup required is Conductor server. The server will use main memory as storage and index will be disabled. 
The main docker-compose-minimal.yaml file is intended to start it. Such infrastructure requires that backend store data 
in memory.

```
docker-compose -f docker-compose-minimal.yaml up -d
```

### Default setup
This setup launches one instance of Elastic Search, another of Dynomite, the Conductor UI, and the server. 

```
docker-compose up -d
```

### MySQL setup

This requires to run a MySQL instance and configure Conductor server. Please check server/config/config-mysql.properties.

```
docker-compose -f docker-compose-mysql.yaml up -d mysql
export CONDUCTOR_CONFIG_FILE=config-mysql.properties
docker-compose up -d conductor-server
```

### MySQL combined with Dynomite for QueueDAO setup

This can be achieved through properties:

```
# Database persistence type.
conductor.db.type=mysql
# Overrides queue to use Dynomite instead of MySQL one
com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.redis.dao.DynoQueueDAO
```

Assuming MySQL and Dynomite are still up and running, just hit:

```
export CONDUCTOR_CONFIG_FILE=config-mysql-dynomite.properties
docker-compose up -d conductor-server
```

### Postgres setup

This requires to run a Postgres instance and configure Conductor server. Please check server/config/config-postgres.properties.

```
docker-compose -f docker-compose-postgres.yaml up -d postgres
export CONDUCTOR_CONFIG_FILE=config-postgres.properties
docker-compose up -d conductor-server
```

### Cassandra, MySQL and Dynomite setup

At the moment of writing this, cassandra-persistence module partially implements the backend. Therefore, you need to 
combine it with something else. To combine it with MySQL and Dynomite follow the recipe:

First, start Cassandra. This example uses three instances.

```
docker-compose -f docker-compose-cassandra.yaml up -d
```

Wait until the cluster is ready and type:
```
export CONDUCTOR_CONFIG_FILE=config-cassandra-mysql-dynomite.properties
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

## Monitoring with Prometheus

Start Prometheus with:

`docker-compose -f docker-compose-prometheus.yaml up -d`

Go to [http://127.0.0.1:9090](http://127.0.0.1:9090).
