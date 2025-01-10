# GCP Cloud Stream Bug (?)

## Prerequisites
 - Java 17
 - Maven
 - Docker

## Description

This project attempts to reproduce an issue when running `spring-cloud-stream` with PubSub with Spring Boot `3.4.0` or later.

To reproduce the error, first make sure the tests pass when using the default `spring-boot-starter-parent` version `3.3.7` by running:
   
    mvn package

After upgrading to version `3.4.0` the tests don't pass anymore. The function binding doesn't seem to work anymore.