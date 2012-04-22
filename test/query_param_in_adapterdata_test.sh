#!/bin/sh
ping -t 192.168.11.1 | grep --line-buffered ttl | java -jar setsuna.jar -debug true  -trigger "column4 > 0" -query "select * from pipe where column1 = %Column1% and column2 = %COLUMN2% and to_number(column4) > to_number(%coLuMn4%)" -debug on