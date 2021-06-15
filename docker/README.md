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

## Monitoring with Prometheus

Start Prometheus with:

`docker-compose -f docker-compose-prometheus.yaml up -d`

Go to [http://127.0.0.1:9090](http://127.0.0.1:9090).


## Potential problem when using docker compose

Elasticsearch timeout
Standalone(single node) elasticsearch has a yellow status which will cause timeout for conductor server(Required: Green).
Spin up a cluster(More than one) to prevent timeout or edit the local code(check the issue tagged for more)
Check issue: https://github.com/Netflix/conductor/issues/2262

Changes does not reflect after changes in config.properties
Config is copy into image during docker build. You have to rebuild the image or better, link a volume to it to reflect new changes.

To troubleshoot a failed startup
Check the log of the server, which is located at app/logs (default directory in dockerfile)

Unable to access to conductor:server with rest
It may takes some time for conductor server to start. Please check server log for potential error.
issue: https://github.com/Netflix/conductor/issues/1725#issuecomment-651806800

How to disable elasticsearch
Elasticsearch is optional, please be aware that disable it will make most of the conductor UI not functional.
Set `workflow.indexing.enabled=false` in your_config.properties
Comment out all the config related to elasticsearch
eg: `conductor.elasticsearch.url=http://es:9200`
Pull request: https://github.com/Netflix/conductor/pull/1555#issue-382145486


