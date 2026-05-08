-- 1. Creazione degli utenti con le password definite nei tuoi application.yml
CREATE USER user_user WITH PASSWORD 'user_pass';
CREATE USER game_user WITH PASSWORD 'game_pass';
CREATE USER wallet_user WITH PASSWORD 'wallet_pass';

-- 2. Creazione dei database logici assegnandone la proprietà ai rispettivi utenti
CREATE DATABASE user_db OWNER user_user;
CREATE DATABASE game_db OWNER game_user;
CREATE DATABASE wallet_db OWNER wallet_user;