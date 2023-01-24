package eu.fbk.interlink.gamification.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.fbk.interlink.gamification.domain.InterlinkPlayer;
import eu.fbk.interlink.gamification.domain.InterlinkTask;
import eu.fbk.interlink.gamification.sec.IdentityLookupComponent;
import eu.fbk.interlink.gamification.util.JsonDB;
import eu.trentorise.game.managers.NotificationManager;
import eu.trentorise.game.model.GameStatistics;
import eu.trentorise.game.model.PlayerState;
import eu.trentorise.game.repo.RuleRepo;
import eu.trentorise.game.services.GameService;
import eu.trentorise.game.services.PlayerService;
import eu.trentorise.game.services.Workflow;

@Component
public class GamificationEngineFacadeComponent {

	Logger logger = LoggerFactory.getLogger(GamificationEngineFacadeComponent.class);

	@Autowired
	Workflow workflow;

	@Autowired
	PlayerService playerSrv;

	@Autowired
	GameService gameSrv;

	@Autowired
	NotificationManager notificationSrv;

	@Autowired
	IdentityLookupComponent identityLookup;

	@Autowired
	RuleRepo ruleRepo;

	@Lazy
	@Autowired
	private JsonDB jsonDB;

	public GamificationEngineFacadeComponent() {
	}

	/**
	 * Create a game from file
	 * 
	 * @param template
	 * @param processId
	 * @throws Exception
	 */
	public void instanceAndConfigureGame(String processId, InterlinkGameTemplate template) throws Exception {
		String user = identityLookup.getName();

		this.jsonDB.importGameDB(this.getGameId(processId, template.getName()), template.getFilename());

	}

	/**
	 * Get game id from process id and game name
	 * 
	 * @param processId
	 * @param name
	 * @return
	 */
	private String getGameId(String processId, String name) {
		return processId.concat("-").concat(name);
	}

	/**
	 * Trigger the user action in the gamification engine
	 * @param task 
	 * 
	 * @param gameId
	 * @param actionId
	 * @param playerId
	 * @param data
	 */

	public void triggerAction(String processId, String name, String action, InterlinkPlayer player, InterlinkTask task) {

		Map<String, Object> data = new HashMap<String, Object>();

		// contribution of players.
		data.put("development", Double.valueOf(player.getDevelopment()));
		data.put("management", (double) player.getManagement());
		data.put("exploitation", (double) player.getExploitation());
		
		// complexity
		data.put("devComplexity", task.getDevelopment());
		data.put("manageComplexity", task.getManagement());
		data.put("exploitComplexity", task.getExploitation());

		workflow.apply(getGameId(processId, name), action, player.getId(), data, null);

	}

	/**
	 * Return a specific player state in a game
	 * 
	 * @param processId
	 * @param name
	 * @param playerId
	 * @return
	 */
	public PlayerState getPlayerState(String processId, String name, String playerId) {
		return this.playerSrv.loadState(getGameId(processId, name), playerId, false, false, false);
	}

	/**
	 * return the list of the player in a game
	 * 
	 * @param processId
	 * @param name
	 * @return
	 */
	public List<String> getPlayers(String processId, String name) {
		// TODO Auto-generated method stub
		return this.playerSrv.readPlayers(getGameId(processId, name));
	}

	/**
	 * Return game statistic
	 * 
	 * @param processId
	 * @param name
	 * @return
	 */
	public List<GameStatistics> getGameStats(String processId, String name, String pointConcept) {

		return gameSrv.loadGameStats(getGameId(processId, name), pointConcept, null, null, null, null);
	}

}
