# ZooKeeperEnvironmentRepository for Spring Cloud Config Server

A custom EnvironmentRepository which reads its configuration from ZooKeeper.
Configuration is read from the following hierarchically structure which allows for complex configuration setups via 
inheritance.
Associations, or labels, can be used to point to different sets of configuration for a service.

Configuration is stored as json in ZooKeeper nodes

    /<name>/conf        The actual configuration
    /<name>/nodemaps    A json map, mapping from an association to a config view
    /<name>/view        The effective config for name which includes all configuration from its ancestors

Nodes can inherit and overwrite configuration from their ancestors.
Following a complete example for a service myservice with configuration for the environments prod, dev and qa.
Their are three arbitrary associations mapping to each of the environments

    /myservice/nodemaps         -> {"live": "/myservice/conf/prod", "developer": "/myservice/conf/test/dev", "bob": "/myservice/conf/test/qa"}   
    /myservice/conf             -> JSON   
    /myservice/conf/prod        -> JSON   
    /myservice/conf/test        -> {"foo": "one", "user": "pwd"}   
    /myservice/conf/test/dev    -> {"foo": "two", "anna": "bob"}
    /myservice/conf/test/qa     -> JSON
    /myservice/views            -> effective JSON   
    /myservice/views/prod       -> effective JSON   
    /myservice/views/test       -> {"foo": "one"}
    /myservice/views/test/dev   -> {"foo": "one", "user": "pwd", anna": "bob"} note how foo has been overwritten
    /myservice/views/test/qa    -> effective JSON

The idea of for this configuration in this format is from https://github.com/mwhooker/jones

## Plug'n'Play
This project provides a Spring Boot auto configuration.
If the "zookeeper" Spring profile is added Spring Cloud Config Server automatically picks up the 
ZooKeeperEnvironmentRepository.

## Usage
The ZooKeeperEnvironmentRepository requires a ZooKeeper connection string to connect with as minimum.

### Compile and Install
Execute ```mvn clean install``` in the project directory.

### Add the dependecy ```pom.xml```

    <dependencies>
      ...
      <dependency>
        <groupId>com.github.felixoldenburg</groupId>
        <artifactId>spring-cloud-config-zookeeper</artifactId>
        <version>1.0.2</version>
      </dependency>
    </dependencies>
    
### application.yml
This is an example for using the ZooKeeperEnvironmentRepository in combination with the default Git one:

    spring:
      profiles:
        active: git, zookeeper
      application:
        name: configserver
      cloud:
        config:
          server:
            git:
              uri: ssh://git@stash.intapps.it:7999/con/{application}.git
              order: 1
            zookeeper:
              order: 2
              connectionString: zookeeper.address
       
## Dependenices
Next to Spring Cloud Config Server this library depends on...
* Curator Framework
* Gson
* JUnit(test) and obviously on the Spring Cloud Config Server 
depdendency.
