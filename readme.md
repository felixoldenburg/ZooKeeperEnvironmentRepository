# Custom EnvironmentRepository for Spring Cloud Config Server

This is a custom environment repository which reads its configuration from ZooKeeper in the Jones format.
Jones is configuration front end and specification on how to store configuration in a ZooKeeper cluster.
See https://github.com/mwhooker/jones on how the configuration data (actually JSON) is expected by 
JonesEnvironmentRepository.
JonesEnvironmentRepository leverages the Curator framework to work on the ZooKeeper data

## Plug'n'Play
This project provides a Spring Boot auto configuration.
If the "jones" Spring profile is added Spring Cloud Config Server automatically picks up the 
JonesEnvironmentRepository.

## Usage
The JonesEnvironmentRepository requires a ZooKeeper connection string to connect with as minimum.

### Compile and Install
Execute ```mvn clean install``` in the project directory.

### Add the dependecy ```pom.xml```

    <dependencies>
      ...
      <dependency>
        <groupId>com.github.felixoldenburg</groupId>
        <artifactId>jones-repository</artifactId>
        <version>1.0.1</version>
      </dependency>
    </dependencies>
    
### application.yml
This is an example for using the JonesEnvironmentRepository in combination with the default Git one:

    spring:
      profiles:
        active: git, jones
      application:
        name: configserver
      cloud:
        config:
          server:
            git:
              uri: ssh://git@stash.intapps.it:7999/con/{application}.git
              order: 1
            jones:
              order: 2
              zookeeper:
                connectionString: zookeeper.address
       
## Dependenices
Next to Spring Cloud Config Server this library depends on...
* Curator Framework
* Gson
* JUnit(test) and obviously on the Spring Cloud Config Server 
depdendency.
