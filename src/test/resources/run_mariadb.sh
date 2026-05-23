#!/bin/bash
DIRECTORY=`dirname $0`
DIRECTORY=$(realpath $DIRECTORY)
# --mount type=tmpfs,destination=/var/lib/mysql \

docker run --name jdbc-mariadb \
 -e 'MYSQL_ROOT_PASSWORD=Cent-Quick-Space-Bath-8' \
 -e 'MYSQL_USER=jdbc' \
 -e 'MYSQL_PASSWORD=Cent-Quick-Space-Bath-8' \
 -e 'MYSQL_DATABASE=jdbc' \
 -p 3307:3306 \
 -v ${DIRECTORY}/mariadb:/docker-entrypoint-initdb.d \
 -d mariadb:12.2.2-ubi10
