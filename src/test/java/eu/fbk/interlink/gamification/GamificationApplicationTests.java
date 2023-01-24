package eu.fbk.interlink.gamification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import eu.trentorise.game.config.AppConfig;
import eu.trentorise.game.config.MongoConfig;
import eu.trentorise.game.core.config.TestCoreConfiguration;
import eu.trentorise.game.managers.GameManager;
import eu.trentorise.game.managers.GameWorkflow;
import eu.trentorise.game.model.Game;
import eu.trentorise.game.model.PlayerState;
import eu.trentorise.game.model.PointConcept;
import eu.trentorise.game.model.core.ClasspathRule;
import eu.trentorise.game.model.core.GameConcept;
import eu.trentorise.game.model.core.GameTask;
import eu.trentorise.game.repo.GamePersistence;
import eu.trentorise.game.repo.NotificationPersistence;
import eu.trentorise.game.repo.StatePersistence;
import eu.trentorise.game.services.PlayerService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AppConfig.class, MongoConfig.class,
		TestCoreConfiguration.class }, loader = AnnotationConfigContextLoader.class)
public class GamificationApplicationTests {

	private static final String GAME = "interLinkGameTest";
	private static final String ACTION = "update_player_points";
	private static final String DOMAIN = "my-domain";

	@Autowired
	private GameManager gameManager;

	@Autowired
	private GameWorkflow workflow;

	@Autowired
	private PlayerService playerSrv;

	@Autowired
	private MongoTemplate mongo;

	private static final String PLAYER = "25";
	private static final String OWNER = "chewbecca";

	@Before
	public void cleanDB() {
		// clean mongo
		mongo.dropCollection(StatePersistence.class);
		mongo.dropCollection(GamePersistence.class);
		mongo.dropCollection(NotificationPersistence.class);
	}

	private void initClasspathRuleGame() {
		gameManager.saveGameDefinition(defineGame().toGame());
		// add rules
		gameManager.addRule(new ClasspathRule(GAME, "rules/" + GAME + "/update_exploitation_points.drl"));
		gameManager.addRule(new ClasspathRule(GAME, "rules/" + GAME + "/update_management_points.drl"));
		gameManager.addRule(new ClasspathRule(GAME, "rules/" + GAME + "/update_development_points.drl"));

	}

	private GamePersistence defineGame() {
		Game game = new Game();

		game.setId(GAME);
		game.setName(GAME);
		game.setOwner(OWNER);
		game.setDomain(DOMAIN);

		game.setActions(new HashSet<String>());
		game.getActions().add(ACTION);

		game.setConcepts(new HashSet<GameConcept>());
		game.getConcepts().add(new PointConcept("management"));
		game.getConcepts().add(new PointConcept("development"));
		game.getConcepts().add(new PointConcept("exploitation"));

		game.setTasks(new HashSet<GameTask>());

		return new GamePersistence(game);

	}

	@Test
	public void loadGame() {
		initClasspathRuleGame();
		Assert.assertEquals(GAME, gameManager.getGameIdByAction(ACTION));
	}

	@Test
	public void execution() throws InterruptedException {
		initClasspathRuleGame();
		PlayerState p = playerSrv.loadState(GAME, PLAYER, true, false);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("development", 8.43);
		params.put("management", 2.0);
		params.put("exploitation", 1.0);
		workflow.apply(GAME, ACTION, PLAYER, params, null);
		p = playerSrv.loadState(GAME, PLAYER, false, false);
		// expected 8.43 development points.
		boolean found = false;
		for (GameConcept gc : p.getState()) {
			if (gc instanceof PointConcept && gc.getName().equals("development")) {
				found = true;
				Assert.assertEquals(8.43d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}

		}
		if (!found) {
			Assert.fail("gameconcepts not found");
		}
	}

}
