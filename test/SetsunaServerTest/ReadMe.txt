javac -encoding utf-8 -cp ./;../../classes *.java
java -Xmx1024m -Xms1024m -server -XX:+UseG1GC -cp ./;../../classes;..\..\lib\h2-1.3.164.jar SetsunaServerTest
java -Xmx1024m -Xms1024m -server -XX:+UseG1GC -cp ./;../../classes;..\..\lib\h2-1.3.164.jar SetsunaServerTopTest

