#!/usr/bin/env bash

sed -ie "s/\${DB_HOST}/${DB_HOSTNAME}/g" jenkins-dbm-config.yml; \
sed -ie "s/\${DB_NAME}/${DB_NAME}/g" jenkins-dbm-config.yml; \
sed -ie "s/\${DB_USERNAME}/${DB_USERNAME}/g" jenkins-dbm-config.yml; \
sed -ie "s/\${DB_PASSWORD}/${DB_PASSWORD}/g" jenkins-dbm-config.yml; \
./gradlew -Plocal.config.location=jenkins-dbm-config.yml clean dbmUpdate
