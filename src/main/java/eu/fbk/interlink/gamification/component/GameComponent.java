package eu.fbk.interlink.gamification.component;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.fbk.interlink.gamification.domain.InterlinkGame;
import eu.fbk.interlink.gamification.repository.GameRepository;

@Component
public class GameComponent {

	@Autowired
	private GameRepository gameRepository;

	public List<InterlinkGame> findAll() {
		return (List<InterlinkGame>) this.gameRepository.findAll();

	}

	public Optional<InterlinkGame> findById(String id) {
		return this.gameRepository.findById(id);

	}

	public List<InterlinkGame> findByTags(List<String> tagList) {
		return this.gameRepository.findByTagList(tagList);

	}

	public List<InterlinkGame> findByProcessId(String processId) {
		return this.gameRepository.findByProcessId(processId);
	}

	public InterlinkGame saveOrUpdateGame(InterlinkGame game) {
		return this.gameRepository.save(game);

	}

	public void deleteGameById(String id) {
		this.gameRepository.deleteById(id);

	}

	public Optional<InterlinkGame> findByProcessIdAndName(String processId, String name) {
		return this.gameRepository.findByProcessIdAndName(processId, name);

	}

}
