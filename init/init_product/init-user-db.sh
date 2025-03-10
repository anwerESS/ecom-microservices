#!/usr/bin/env bash
set -e


echo "****** CREATE DATABASE PRODUCT ******"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
		CREATE USER docker;
	CREATE DATABASE product;
  	GRANT ALL PRIVILEGES ON DATABASE docker TO docker;
EOSQL