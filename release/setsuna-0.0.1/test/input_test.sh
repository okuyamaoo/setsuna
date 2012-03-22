#!/bin/sh

sar -u 2 1000 | grep  --line-buffered all | java -jar setsuna.jar

