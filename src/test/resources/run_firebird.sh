# https://hub.docker.com/r/firebirdsql/firebird
# enter container console
# docker exec -i -t firebird3 /bin/bash
# -e 'ISC_PASSWORD=masterkey' \
# --mount type=tmpfs,destination=/var/lib/firebird/data \
# -e 'FIREBIRD_DATABASE=jdbc' \

docker run --name jdbc-firebird \
 -e 'FIREBIRD_USER=jdbc' \
 -e 'FIREBIRD_PASSWORD=Cent-Quick-Space-Bath-8' \
 -e 'FIREBIRD_USE_LEGACY_AUTH=true' \
 -p 3050:3050 \
 -d firebirdsql/firebird:4.0.6