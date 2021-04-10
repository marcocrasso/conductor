## Backend
Conductor provides a pluggable backend.  The current implementation uses Dynomite.

There are 4 interfaces that needs to be implemented for each backend:

```java
//Store for workflow and task definitions
com.netflix.conductor.dao.MetadataDAO
```

```java
//Store for workflow executions
com.netflix.conductor.dao.ExecutionDAO
```

```java
//Index for workflow executions
com.netflix.conductor.dao.IndexDAO
```

```java
//Queue provider for tasks
com.netflix.conductor.dao.QueueDAO
```

It is possible to mix and match different implementations for each of these.  
For example, SQS for queueing and a relational store for others. To do this, you must follow the Java SPI architecture.


First, you should get familiar with "core DAO is the Service Interface" while "your DAO implementation is the Service 
Provider". Create a project, which depends on core, and implement each Service Interface in a separate class. Then, 
create folder main/resources/META-INF/services with text files, named as the fully qualified classes of the Service 
Interfaces, and  edit each file inserting the fully qualified class name of your respective Service Provider.

Example:
```
postgres-persistence
|- src
  |- main
    |- resources
      |- META-INF
        |- services
          |- com.netflix.conductor.dao.MetadataDAO
```
contains:
```
com.netflix.conductor.postgres.dao.PostgresMetadataDAO
```

When implementing your Service Provider, you must understand that Service Loader will  instantiate your Service Providers
using the default constructor. Make sure your code won't try to create a Bean of your Service Providers, the Service 
Loader will do it. Finally, use @Autowire for arguments required for initialize your Service Provide, and @PostConstruct 
annotation to run any initialization step. For example:


``` 
@Autowired PostgresProperty properties;
@Autowired Datasource datasource;

public PostgresMetadataDAO() {}

@PostConstruct
protected void init() {
  this.schema = this.properties.getSchema();
}
```




## System Tasks
To create system tasks follow the steps below:

* Extend ```com.netflix.conductor.core.execution.tasks.WorkflowSystemTask```
* Instantiate the new class as part of the startup (eager singleton)
* Implement the ```TaskMapper``` [interface](https://github.com/Netflix/conductor/blob/master/core/src/main/java/com/netflix/conductor/core/execution/mapper/TaskMapper.java)
* Add this implementation to the map identified by [TaskMappers](https://github.com/Netflix/conductor/blob/master/core/src/main/java/com/netflix/conductor/core/config/CoreModule.java#L70)

## External Payload Storage
To configure conductor to externalize the storage of large payloads:

* Implement the `ExternalPayloadStorage` [interface](https://github.com/Netflix/conductor/blob/master/common/src/main/java/com/netflix/conductor/common/utils/ExternalPayloadStorage.java).
* Add the storage option to the enum [here](https://github.com/Netflix/conductor/blob/master/server/src/main/java/com/netflix/conductor/bootstrap/ModulesProvider.java#L39).
* Set this JVM system property ```workflow.external.payload.storage``` to the value of the enum element added above.
* Add a binding similar to [this](https://github.com/Netflix/conductor/blob/master/server/src/main/java/com/netflix/conductor/bootstrap/ModulesProvider.java#L120-L127).

## Workflow Status Listener
To provide a notification mechanism upon completion/termination of workflows:

* Implement the ```WorkflowStatusListener``` [interface](https://github.com/Netflix/conductor/blob/master/core/src/main/java/com/netflix/conductor/core/execution/WorkflowStatusListener.java)
* This can be configured to plugin custom notification/eventing upon workflows reaching a terminal state.

## Locking Service

By default, Conductor Server module loads Zookeeper lock module. If you'd like to provide your own locking implementation module, 
for eg., with Dynomite and Redlock:

* Implement ```Lock``` interface.
* Add a binding similar to [this](https://github.com/Netflix/conductor/blob/master/server/src/main/java/com/netflix/conductor/bootstrap/ModulesProvider.java#L115-L129)
* Enable locking service: ```conductor.app.workflowExecutionLockEnabled: true```
