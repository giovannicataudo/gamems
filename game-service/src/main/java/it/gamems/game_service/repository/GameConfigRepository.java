package it.gamems.game_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.gamems.game_service.entity.GameConfig;

// Repo per settare active del gioco
@Repository
public interface GameConfigRepository extends JpaRepository<GameConfig, String>{}
