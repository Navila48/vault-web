SELECT 'CREATE DATABASE "cloud-db"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cloud-db')\gexec

