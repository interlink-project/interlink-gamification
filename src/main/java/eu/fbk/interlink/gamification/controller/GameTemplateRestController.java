package eu.fbk.interlink.gamification.controller;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.fbk.interlink.gamification.component.GameTemplateComponent;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.fbk.interlink.gamification.util.ControllerUtils;

@RestController
@RequestMapping("/interlink")
@Profile({ "no-sec", "sec", "default" })
public class GameTemplateRestController {

	private static final Logger logger = LogManager.getLogger(GameTemplateRestController.class);

	@Autowired
	private GameTemplateComponent gameTemplateComponent;

	/**
	 * Create a gametemplate
	 * 
	 * @param game
	 * @return Message
	 */
	@PostMapping(value = "/gametemplate")
	public ResponseEntity<?> newGametemplate(@RequestBody InterlinkGameTemplate gametemplate) {

		if ((gametemplate.getId() != null) && (gameTemplateComponent.findById(gametemplate.getId()).isPresent())) {
			return new ResponseEntity("Gametemplate is already present", HttpStatus.PRECONDITION_FAILED);
		}

		// hook to instatiate the new game in FBK gamification engine.
		if (logger.isInfoEnabled()) {
			logger.info("New gametemplate " + gametemplate.getName() + " of processId " + gametemplate.getProcessId()
					+ "has been created");
		}

		gameTemplateComponent.saveOrUpdateGame(gametemplate);
		return new ResponseEntity("Game updated successfully", HttpStatus.OK);
	}

	/**
	 * Return the game template with a specific gameId
	 * 
	 * @param gameId
	 * @return GameTemplate
	 */
	@GetMapping(value = "/gametemplate/{gameTemplateId}")
	public Optional<InterlinkGameTemplate> getGameTemplate(@PathVariable String gameTemplateId) {
		gameTemplateId = ControllerUtils.decodePathVariable(gameTemplateId);
		return gameTemplateComponent.findById(gameTemplateId);
	}

	/**
	 * Return all the games template with specific tags present in the DB
	 * 
	 * @return List of GameTemplate
	 */
	@GetMapping(value = { "/gametemplate/list", "/gametemplate/tagList/list" })
	public List<InterlinkGameTemplate> findGameTemplateByTags(@RequestParam(required = false) List<String> tagList) {
		if (tagList == null)
			return gameTemplateComponent.findAll();
		return gameTemplateComponent.findByTags(tagList);
	}

	/**
	 * Refresh template from files
	 * 
	 * @return List of GameTemplate
	 */
	@GetMapping(value = "/gametemplate/refresh")
	public ResponseEntity<?> refresh() {
		try {
			gameTemplateComponent.refresh();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
			new ResponseEntity("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity("Template has been refreshed", HttpStatus.OK);
	}

}
