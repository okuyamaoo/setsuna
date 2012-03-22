#!/bin/sh

sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -stream sar

