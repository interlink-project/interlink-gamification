package eu.fbk.interlink.gamification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;



@Repository
public interface GameTemplateRepository extends MongoRepository<InterlinkGameTemplate, String> {
	
	Optional<InterlinkGameTemplate> findById(String id);
	
	Optional<InterlinkGameTemplate> findByProcessIdAndName(String processId, String name);
	
	List<InterlinkGameTemplate> findByTagList(List<String> tagList);

	
}


