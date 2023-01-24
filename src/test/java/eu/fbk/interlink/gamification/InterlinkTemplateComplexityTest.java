package eu.fbk.interlink.gamification;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import eu.fbk.interlink.gamification.component.GameComponent;
import eu.fbk.interlink.gamification.component.GamificationEngineFacadeComponent;
import eu.fbk.interlink.gamification.domain.InterlinkGame;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.fbk.interlink.gamification.domain.InterlinkPlayer;
import eu.fbk.interlink.gamification.domain.InterlinkTask;
import eu.fbk.interlink.gamification.sec.IdentityLookupComponent;
import eu.trentorise.game.config.AppConfig;
import eu.trentorise.game.config.MongoConfig;
import eu.trentorise.game.core.config.TestCoreConfiguration;
import eu.trentorise.game.managers.GameManager;
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
@ContextConfiguration(classes = { AppConfig.class, TestCoreConfiguration.class, GameComponent.class,
		GamificationEngineFacadeComponent.class,
		IdentityLookupComponent.class }, loader = AnnotationConfigContextLoader.class)
@EnableMongoRepositories("eu.fbk.interlink.gamification.repository")
public class InterlinkTemplateComplexityTest {

	private static final String GAME = "interLinkGameTest";
	private static final String ACTION = "update_player_points";
	private static final String DOMAIN = "my-domain";

	@Autowired
	private GameManager gameManager;

	@Autowired
	private GameComponent gameComponent;

	@Autowired
	private GamificationEngineFacadeComponent gamificationComponent;

	@Autowired
	private PlayerService playerSrv;

	@Autowired
	private MongoTemplate mongo;

	private static final String PLAYER_ID = "123";
	private static final String PLAYER_NAME = "chewbecca_developer";
	private static final String TEMPLATE_ID = "complexityProcess";
	private static final String TEMPLATE_NAME = "complexityGame";
	private static final String TASK_ID = "complexityTask";

	@Before
	public void cleanDB() {
		// clean mongo
		mongo.dropCollection(StatePersistence.class);
		mongo.dropCollection(GamePersistence.class);
		mongo.dropCollection(NotificationPersistence.class);
		mongo.dropCollection(InterlinkGame.class);
		mongo.dropCollection(InterlinkGameTemplate.class);

	}

	private void initClasspathRuleGame() {
		gameManager.saveGameDefinition(defineGame().toGame());
		// add rules
		gameManager.addRule(new ClasspathRule(TEMPLATE_ID + "-" + TEMPLATE_NAME,
				"rules/" + GAME + "/update_exploitation_points_complexity.drl"));
		gameManager.addRule(new ClasspathRule(TEMPLATE_ID + "-" + TEMPLATE_NAME,
				"rules/" + GAME + "/update_management_points_complexity.drl"));
		gameManager.addRule(new ClasspathRule(TEMPLATE_ID + "-" + TEMPLATE_NAME,
				"rules/" + GAME + "/update_development_points_complexity.drl"));
	}

	private GamePersistence defineGame() {
		Game game = new Game();

		game.setId(TEMPLATE_ID + "-" + TEMPLATE_NAME);
		game.setName(GAME);
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
	public void executionTask() throws InterruptedException {
		initClasspathRuleGame();
		InterlinkGame template = new InterlinkGame();
		template.setProcessId(TEMPLATE_ID);
		template.setName(TEMPLATE_NAME);
		InterlinkTask task = new InterlinkTask();
		// complexities.
		task.setDevelopment(4);
		task.setExploitation(3);
		task.setManagement(5);
		task.setId(TASK_ID);
		template.addTask(task);
		// add player contribution.
		InterlinkPlayer player = new InterlinkPlayer();
		player.setId(PLAYER_ID);
		player.setName(PLAYER_NAME);
		player.setDevelopment(3);
		player.setManagement(2);
		player.setExploitation(1);
		task.addPlayer(player);
		gameComponent.saveOrUpdateGame(template);

		// complete task.
		gamificationComponent.triggerAction(TEMPLATE_ID, TEMPLATE_NAME, "update_player_points", player, task);

		PlayerState p = playerSrv.loadState(TEMPLATE_ID + "-" + TEMPLATE_NAME, PLAYER_ID, false, false);
		// expected 8.43 development points.
		boolean found = false;
		for (GameConcept gc : p.getState()) {
			if (gc instanceof PointConcept && gc.getName().equals("development")) {
				found = true;
				Assert.assertEquals(12.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
			if (gc instanceof PointConcept && gc.getName().equals("management")) {
				found = true;
				Assert.assertEquals(10.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
			if (gc instanceof PointConcept && gc.getName().equals("exploitation")) {
				found = true;
				Assert.assertEquals(3.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
		}
		if (!found) {
			Assert.fail("gameconcepts not found");
		}
	}
	
	@Test
	public void executionSubTask() throws InterruptedException {
		initClasspathRuleGame();
		InterlinkGame template = new InterlinkGame();
		template.setProcessId(TEMPLATE_ID);
		template.setName(TEMPLATE_NAME);
		InterlinkTask task = new InterlinkTask();
		task.setId(TASK_ID);
		InterlinkTask subTask = new InterlinkTask();
		// complexities.
		subTask.setDevelopment(4);
		subTask.setExploitation(3);
		subTask.setManagement(5);
		// add player contribution.
		InterlinkPlayer player = new InterlinkPlayer();
		player.setId(PLAYER_ID);
		player.setName(PLAYER_NAME);
		player.setDevelopment(3);
		player.setManagement(2);
		player.setExploitation(1);
		subTask.addPlayer(player);
		task.addSubtask(subTask);
		template.addTask(task);
		gameComponent.saveOrUpdateGame(template);

		// complete task.
		gamificationComponent.triggerAction(TEMPLATE_ID, TEMPLATE_NAME, "update_player_points", player, subTask);

		PlayerState p = playerSrv.loadState(TEMPLATE_ID + "-" + TEMPLATE_NAME, PLAYER_ID, false, false);
		// expected 8.43 development points.
		boolean found = false;
		for (GameConcept gc : p.getState()) {
			if (gc instanceof PointConcept && gc.getName().equals("development")) {
				found = true;
				Assert.assertEquals(12.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
			if (gc instanceof PointConcept && gc.getName().equals("management")) {
				found = true;
				Assert.assertEquals(10.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
			if (gc instanceof PointConcept && gc.getName().equals("exploitation")) {
				found = true;
				Assert.assertEquals(3.0d, ((PointConcept) gc).getScore().doubleValue(), 0);
			}
		}
		if (!found) {
			Assert.fail("gameconcepts not found");
		}
	}

}
