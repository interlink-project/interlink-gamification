package eu.fbk.interlink.gamification.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.MongoException;

import eu.fbk.interlink.gamification.domain.PlayerScore;
import eu.fbk.interlink.gamification.util.ControllerUtils;
import eu.trentorise.game.core.LogHub;
import eu.trentorise.game.managers.ClassificationUtils;
import eu.trentorise.game.model.PointConcept.PeriodInstance;
import eu.trentorise.game.repo.GamePersistence;
import eu.trentorise.game.repo.StatePersistence;

@Component
public class InterLinkerRepository {

	private static final Logger logger = LoggerFactory.getLogger(InterLinkerRepository.class);

	@Autowired
	private MongoTemplate mongo;

	@Autowired
	private eu.trentorise.game.repo.GameRepo gameRepo;

	public Page<PlayerScore> search(String gameId, String pcName, String period, Pageable pageable) {
		List<PlayerScore> result = new ArrayList<PlayerScore>();
		List<StatePersistence> states = null;
		long totalSize = 0;
		GamePersistence g = gameRepo.findById(gameId).get();
		try {
			Query q = new Query();
			q.addCriteria(Criteria.where("gameId").is(gameId));
			q.fields().include("playerId");
			/**
			 * db.playerState.find( { "gameId":"57ac710fd4c6ac7872b0e7a1" }, {
			 * "concepts.PointConcept.green
			 * leaves.obj.periods.weekly.instances.2016-09-03T00:00:00.score" : 1,
			 * "playerId": 1 } ).sort( { "concepts.PointConcept.green
			 * leaves.obj.periods.weekly.instances.2016-09-10T00:00:00.score": -1 } );
			 **/
			if (period.equalsIgnoreCase("global")) {
				q.fields().include("concepts.PointConcept." + pcName + ".obj.score");
				q.with(Sort.by(Sort.Order.desc("concepts.PointConcept." + pcName + ".obj.score")));
				states = mongo.find(q, StatePersistence.class);
				totalSize = mongo.count(q, StatePersistence.class);
				for (StatePersistence sp : states) {
					result.add(ControllerUtils.convertPlayerState(sp, pcName, period, null));
				}
			} else if (period.equalsIgnoreCase("currentWeek")) {
				Calendar cal = Calendar.getInstance();
				long moment = cal.getTimeInMillis();
				PeriodInstance periodInstance = ClassificationUtils.retrieveWindow(g.toGame(), "weekly", pcName, moment,
						-1);
				if (periodInstance != null) {
					String key = ClassificationUtils.generateKey(periodInstance);
					q.fields().include(
							"concepts.PointConcept." + pcName + ".obj.periods.weekly.instances." + key + ".score");
					q.with(Sort.by(Sort.Order.desc(
							"concepts.PointConcept." + pcName + ".obj.periods.weekly.instances." + key + ".score")));
					states = mongo.find(q, StatePersistence.class);
					totalSize = mongo.count(q, StatePersistence.class);
					for (StatePersistence sp : states) {
						result.add(ControllerUtils.convertPlayerState(sp, pcName, period, key));
					}
				}
			} else if (period.equalsIgnoreCase("previousWeek")) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_WEEK, -7);
				long moment = cal.getTimeInMillis();
				PeriodInstance periodInstance = ClassificationUtils.retrieveWindow(g.toGame(), "weekly", pcName, moment,
						-1);
				if (periodInstance != null) {
					String key = ClassificationUtils.generateKey(periodInstance);
					q.fields().include(
							"concepts.PointConcept." + pcName + ".obj.periods.weekly.instances." + key + ".score");
					q.with(Sort.by(Sort.Order.desc(
							"concepts.PointConcept." + pcName + ".obj.periods.weekly.instances." + key + ".score")));
					states = mongo.find(q, StatePersistence.class);
					totalSize = mongo.count(q, StatePersistence.class);
					for (StatePersistence sp : states) {
						result.add(ControllerUtils.convertPlayerState(sp, pcName, period, key));
					}
				}
			}
		} catch (UncategorizedMongoDbException | MongoException e) {
			exceptionHandler(gameId, e);
		}
		return new PageImpl<>(result, pageable, totalSize);
	}

	private void exceptionHandler(String gameId, Exception e) {
		LogHub.error(gameId, logger, "Exception running mongo query in search", e);
		throw new IllegalArgumentException("Query seems to be not valid");
	}
}
