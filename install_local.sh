#!/usr/bin/env bash

mvn install:install-file -Dfile=lib/protobuf-java-3.3.0-custom.jar -DgroupId=com.google.protobuf -DartifactId=protobuf-java -Dversion=3.3.0-custom -Dpackaging=jar
mvn install