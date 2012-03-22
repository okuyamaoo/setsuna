#!/bin/sh

java -classpath ./:../lib/msgpack/*:../setsuna.jar setsuna.core.SetsunaMain -server true -stream svrtbl

