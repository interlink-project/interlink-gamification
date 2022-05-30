package eu.fbk.interlink.gamification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import eu.fbk.interlink.gamification.domain.InterlinkGame;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;

@Repository
public interface GameRepository extends MongoRepository<InterlinkGame, String>{
	
	Optional<InterlinkGame> findById(String id);

	List<InterlinkGame> findByTagList(List<String> tagList);
	
	List<InterlinkGame> findByProcessId(String processId);
	
	Optional<InterlinkGame> findByProcessIdAndName(String processId, String name);

}
