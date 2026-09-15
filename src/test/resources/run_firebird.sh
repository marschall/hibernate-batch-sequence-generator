# https://hub.docker.com/r/firebirdsql/firebird
# enter container console
# docker exec -i -t firebird3 /bin/bash
# --mount type=tmpfs,destination=/var/lib/firebird/data \

docker run --name jdbc-firebird \
 -e 'FIREBIRD_USER=jdbc' \
 -e 'FIREBIRD_PASSWORD=Cent-Quick-Space-Bath-8' \
 -e 'FIREBIRD_DATABASE=jdbc.fdb' \
 -p 3050:3050 \
 -d firebirdsql/firebird:5.0.4-trixie