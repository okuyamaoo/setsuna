#!/bin/sh
ping -t 192.168.1.1 | grep --line-buffered TTL | java -jar setsuna.jar -easyquery "last_range_avg_over(pipe, column5, 10, 15, 1.2)"