-- SQL Server only: run in SSMS / Azure Data Studio, not phpMyAdmin.
-- STOP if f6s_shop already exists; do not overwrite an existing database.
USE master;
GO
CREATE DATABASE f6s_shop COLLATE Vietnamese_100_CI_AS;
GO
