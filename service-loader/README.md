# Service Provider Loader

This component has been implemented to design core DAOs as Java SPI modules. 

At the moment of implementing this component, Conductor core services expect beans, which were injected by spring-boot. 
Choosing among different DAOs implementations was governed by the property conductor.db.type. The main shortcoming of
this approach was that admins couldn't combine two or more technologies for the storage system through configuration.
For example, using Dynomite for QueueDAO and Postgres for the others was impossible without modifying source.

Java SPI is a good solution for the stated problem, while it imposes some constraints that required refactorings, for 
instance:

- SPI modules must be registered in META-INF/services plain text files
- SPI modules must have default constructor

Previous design forced other refactorings too. Let analyze some cases. In the previous implementation:

1. different DAO implementations were not planned to co-exist in the spring-boot context since conditional bean loading 
  (OnCondition) was employed on conductor.db.type. Thus, having two candidates for the same bean wasn't possible
2. as two different implementation were not planned to co-exist, two implementation could try to register homonyms beans,
  like when mysql and post persistence modules tried to register a bean named DataSource
3. as two different implementation were not planned to co-exist, only one system property was enough
4. initialization methods occurred in parametrized constructors. This was ok, since those required parameters were auto
  wired in the constructors by spring-boot
  
To cope with 1: a refactoring occurred to convert DAOs implementation into SPI modules, and remove the Bean/Component 
annotation. This Service Loader component is in charge of __beanize__ them.

For 2: the autoconfiguration of DataSource was disabled and data sources for Postgres and MySQL are initialized by the 
abstract classes of the respective DAOs implementations.

For 3: back-ward compatible configuration support is implemented by this component. If the user says conductor.db.type
is 'mysql' that is the same that:
```
com.netflix.conductor.dao.ExecutionDAO=com.netflix.conductor.mysql.dao.MySQLExecutionDAO
com.netflix.conductor.dao.EventHandlerDAO=com.netflix.conductor.mysql.dao.MySQLEventHandlerDAO
com.netflix.conductor.dao.MetadataDAO=com.netflix.conductor.mysql.dao.MySQLMetadataDAO
com.netflix.conductor.dao.PollDataDAO=com.netflix.conductor.mysql.dao.MySQLPollDataDAO
com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.mysql.dao.MySQLQueueDAO
com.netflix.conductor.dao.RateLimitingDAO=com.netflix.conductor.mysql.dao.MySQLRateLimitingDAO
```
while:
```
conductor.db.type = mysql
com.netflix.conductor.dao.QueueDAO=com.netflix.conductor.redis.dao.DynoQueueDAO
```

will combine 5 DAOs from MySQL implementation with the Dynomite one for the queue. 

This support is configured in resources/spi.bundled.properties, since SPI class names can be renamed or types added.

Finally, to cope with the initialization of resources required by each DAO implementation @PostConstruct annotation was 
used because of the next combination of facts:


1. the Service Loader will create the DAO implementation beans using the default constructor
   
2. the initialization required parameters will be auto wired by spring-boot

3. the PostConstruct annotation runs after 1 and 2

## Mocks and Java SPI for testing

During Unit testing, it is common that backend is not required. Test containers are a good solution for integration 
tests when backend is required, while mocks are key for Unit testing. Therefore, for Unit testing you can implement a 
DAO interface with a mock, register it in META-INF/services respective file, and configure core to load it. 

However, there is a more straightforward approach which is declaring the mock as a primary bean. For example: 

```
@Bean
@Primary
QueueDAO mockedQueue() {
    DetachedMockFactory detachedMockFactory = new DetachedMockFactory()
    DynoQueueDAO dynoQueueDAO = new SpyableQueue()
    return detachedMockFactory.Spy(dynoQueueDAO)
}
```

The above is **just** an example, you can mock every DAO or provide your custom implementation without Java SPI, don't
forget the @Primary annotation.

## Persistence module migration guide

If you have implemented a layer for core DAOs, you might want to migrate it to Java SPI. DISCLAIMER: what you are 
about to do is combining two DI mechanisms in a way not so popular for the community, thus below you will find a humble 
migration guide.

### Step 1: register your Service Providers

Perform next check in your module source root. 

- [ ] get familiar with: "core DAO is the Service Interface" while your "DAO implementation is the Service Provider"
- [ ] create folder main/resources/META-INF/services
- [ ] create 6 files in the above folder, named as the fully qualified classes of the Service Interfaces
- [ ] edit each file inserting the fully qualified class name of your respective Service Provider

Example:
```
postgres-persistence
|- src
  |- main
    |- resources
      |- META-INF
        |- services
          |- com.netflix.conductor.dao.EventHandlerDAO 
```
contains: 
```
com.netflix.conductor.postgres.dao.PostgresEventHandlerDAO
```

### Step 2: refactor your code to adhere with Service Providers

- [ ] make sure the Service Loader will be able to instantiate your Service Providers using the default constructor
- [ ] make sure your code won't try to create a Bean of your Service Providers, the Service Loader will do it
- [ ] make sure your Service Providers will be ready when the Service Loader creates them using the default constructor

Last check means that your Service Providers initialization methods, e.g. open a connection to a db, must happen after
the default constructor will be called, and required dependencies are satisfied. Let's see an example:

Before: 

```
PostgresProperty properties;
Datasource datasource;

public PostgresMetadataDAO(PostgresProperty properties, Datasource datasource) {
  this.properties = properties;
  this.datasource = datasource;
  this.schema = this.properties.getSchema();
}
```

After:
``` 
@Autowired PostgresProperty properties;
@Autowired Datasource datasource;

public PostgresMetadataDAO() {}

@PostConstruct
protected void init() {
  this.schema = this.properties.getSchema();
}
```

Questions are welcome.
