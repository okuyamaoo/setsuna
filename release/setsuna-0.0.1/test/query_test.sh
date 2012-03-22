#!/bin/sh

sar -u 2 10000 | grep  --line-buffered all | java -jar setsuna.jar -trigger "COLUMN7 < 90" -query "select * from (select avg(to_number(column5)) as avgio from (select column5 from pipe order by column0 desc limit 5) as t1)  where avgio > 10"

