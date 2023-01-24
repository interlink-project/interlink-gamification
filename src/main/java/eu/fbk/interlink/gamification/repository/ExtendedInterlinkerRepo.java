package eu.fbk.interlink.gamification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.trentorise.game.repo.StatePersistence;

public interface ExtendedInterlinkerRepo  {

	public Page<StatePersistence> search(String gameId, String pcName, String period, Pageable pageable);
}
