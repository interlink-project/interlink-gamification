package eu.fbk.interlink.gamification.util;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.fbk.interlink.gamification.component.GameTemplateComponent;
import eu.fbk.interlink.gamification.controller.GameTemplateRestController;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.trentorise.game.managers.GameManager;
import eu.trentorise.game.model.ChallengeModel;
import eu.trentorise.game.model.Game;
import eu.trentorise.game.model.core.DBRule;
import eu.trentorise.game.model.core.Rule;
import eu.trentorise.game.repo.ChallengeConceptRepo;
import eu.trentorise.game.repo.ChallengeModelRepo;
import eu.trentorise.game.repo.GamePersistence;
import eu.trentorise.game.repo.GameRepo;
import eu.trentorise.game.repo.GroupChallengeRepo;
import eu.trentorise.game.repo.RuleRepo;

@Component
@Transactional
@Repository
public class JsonDB {

	Logger logger = LoggerFactory.getLogger(JsonDB.class);

	@Autowired
	private GameTemplateComponent gameTemplateComponent;

	@Autowired
	@Value("${import.dir}")
	private String importPath;

	@Autowired
	@Value("${export.dir}")
	private String exportPath;

	@Autowired
	private GameManager gameManager;

	@Autowired
	private GameRepo gameRepo;

	@Autowired
	private ChallengeModelRepo challengeModelRepo;

	ObjectMapper mapper = new ObjectMapper();

	/**
	 * Export DB GamePersistence, ChallengeModel, Rule
	 * 
	 * @throws Exception
	 */
	public void exportGameDB() throws Exception {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		List<GamePersistence> games = (List<GamePersistence>) gameRepo.findAll();
		Map<String, Map<String, Object>> gameJson = new HashMap<String, Map<String, Object>>();
		for (GamePersistence game : games) {
			Set<ChallengeModel> gameChallengeModels = challengeModelRepo.findByGameId(game.getId());
			Set<DBRule> rules = new HashSet<DBRule>();
			for (String rule : game.getRules()) {
				rules.add((DBRule) gameManager.loadRule(game.getId(), rule));
			}
			String gameId = game.getId();
			Map<String, Object> mapTemp = gameJson.get(gameId);
			if (mapTemp == null) {
				mapTemp = new HashMap<String, Object>();
				gameJson.put(gameId, mapTemp);
			}
			nullifyIds(game, rules, gameChallengeModels);
			mapTemp.put("game", game);
			mapTemp.put("rules", rules);
			mapTemp.put("challengeModels", gameChallengeModels);
		}
		File f = new File(exportPath, "db.json");
		mapper.writerWithDefaultPrettyPrinter().writeValue(f, gameJson);
	}

	private void nullifyIds(GamePersistence game, Set<DBRule> rules, Set<ChallengeModel> gameChallengeModels) {
		game.setId(null);
		game.setRules(null);
		for (ChallengeModel model : gameChallengeModels) {
			model.setId(null);
		}
		for (DBRule r : rules) {
			r.setGameId(null);
			r.setId(null);
		}

	}

	/**
	 * Import JSON DB GamePersistence, ChallengeModel, Rule
	 * 
	 * @throws Exception
	 */
	public void importGameDB(String gameId, String filename) throws Exception {
		try {
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			File f = new File(importPath, filename);
			Map<?, ?> map = mapper.readValue(f, Map.class);
			map.entrySet().forEach(entry -> {
				// import the game
				Map<String, Object> gameData = (Map<String, Object>) entry.getValue();
				GamePersistence g = mapper.convertValue(gameData.get("game"), GamePersistence.class);
				g.setId(gameId);
				List<?> r = (List<?>) gameData.get("rules");
				List<?> chModels = (List<?>) gameData.get("challengeModels");
				persistGame(g, r, chModels);
				
				
				// import the template if any
				InterlinkGameTemplate template = mapper.convertValue(gameData.get("template"),
						InterlinkGameTemplate.class);
				if (template != null) {

					Optional<InterlinkGameTemplate> tempTemplate = this.gameTemplateComponent
							.findByProcessIdAndName(template.getProcessId(), template.getName());
					if (tempTemplate.isPresent()) {
						this.logger.debug("delete template " + tempTemplate.get().getId());
						this.gameTemplateComponent.deleteById(tempTemplate.get().getId());
					}
					this.gameTemplateComponent.saveOrUpdateGame(template);
				}
			});
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	private void persistGame(GamePersistence g, List<?> rules, List<?> chModels) {
		GamePersistence pg = gameRepo.save(g);
		for (Object cmObj : chModels) {
			
			ChallengeModel cm = mapper.convertValue(cmObj, ChallengeModel.class);
			cm.setGameId(pg.getId());
			gameManager.saveChallengeModel(pg.getId(), cm);
			
			
			
		}
		for (Object rObj : rules) {
			DBRule r = mapper.convertValue(rObj, DBRule.class);
			r.setGameId(pg.getId());
			gameManager.addRule(r);
		}
	}

	/**
	 * Import JSON DB GamePersistence, ChallengeModel, Rule
	 * 
	 * @throws Exception
	 */
	public void importGameTemplateDB() throws Exception {

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.logger.debug("import template from  " + importPath);
		File folder = new File(importPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				logger.info("load template from file " + file.getName());
				try {
				Map<?, ?> map = mapper.readValue(file, Map.class);
				map.entrySet().forEach(entry -> {
					Map<String, Object> gameData = (Map<String, Object>) entry.getValue();
					InterlinkGameTemplate template = mapper.convertValue(gameData.get("template"),
							InterlinkGameTemplate.class);
					if (template != null) {

						Optional<InterlinkGameTemplate> tempTemplate = this.gameTemplateComponent
								.findByProcessIdAndName(template.getProcessId(), template.getName());
						if (tempTemplate.isPresent()) {
							this.logger.debug("delete template " + tempTemplate.get().getId());
							this.gameTemplateComponent.deleteById(tempTemplate.get().getId());
						}
						this.gameTemplateComponent.saveOrUpdateGame(template);
						
					}
				});
				} catch (java.io.CharConversionException ex) {
					this.logger.info("file " + file.getName() + "not valid");
				}

			}
		}
	}

}
