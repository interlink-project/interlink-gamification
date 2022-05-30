package eu.fbk.interlink.gamification.component;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import eu.fbk.interlink.gamification.domain.InterlinkGame;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.fbk.interlink.gamification.repository.GameTemplateRepository;
import eu.fbk.interlink.gamification.util.JsonDB;

@Component
public class GameTemplateComponent {

	@Autowired
	private GameTemplateRepository gameTemplateRepository;

	@Lazy
	@Autowired
	private JsonDB jsonDB;

	public List<InterlinkGameTemplate> findAll() {
		return this.gameTemplateRepository.findAll();

	}

	public Optional<InterlinkGameTemplate> findById(String id) {
		return this.gameTemplateRepository.findById(id);

	}

	public Optional<InterlinkGameTemplate> findByProcessIdAndName(String processId, String name) {
		return this.gameTemplateRepository.findByProcessIdAndName(processId, name);

	}

	public List<InterlinkGameTemplate> findByTags(List<String> tagList) {
		return this.gameTemplateRepository.findByTagList(tagList);

	}

	public InterlinkGame saveOrUpdateGame(InterlinkGameTemplate gametemplate) {
		return this.gameTemplateRepository.save(gametemplate);

	}

	public void refresh() throws Exception {
		jsonDB.importGameTemplateDB();
	}
	// GameTemplate saveOrUpdateGame(Game game);

	public void deleteById(String gametempleteId) {
		this.gameTemplateRepository.deleteById(gametempleteId);
	}

}
