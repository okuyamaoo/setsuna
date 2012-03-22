#!/bin/sh

top -b -d 1 | grep --line-buffered ^top | java -jar setsuna.jar -stream top -trigger "COLUMN11 > 1" -query "select * from (select avg(to_number(column5)) as avgio from (select column5 from sar order by column0 desc limit 5) as t1)  where avgio > 10" -event /home/setsuna/WarningIOWait.sh

