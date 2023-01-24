package eu.fbk.interlink.gamification.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import eu.fbk.interlink.gamification.domain.PlayerScore;
import eu.fbk.interlink.gamification.domain.PlayerStateDTO;
import eu.trentorise.game.model.PlayerState;
import eu.trentorise.game.model.core.GameConcept;
import eu.trentorise.game.repo.StatePersistence;

public class ControllerUtils {

	public static String decodePathVariable(String variable) {
		try {
			variable = URLDecoder.decode(variable, "UTF-8");
			return variable;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(String.format("%s is not UTF-8 encoded", variable));
		}
	}

	public static boolean isEmpty(String value) {
		boolean result = true;
		if ((value != null) && (!value.isEmpty())) {
			result = false;
		}
		return result;
	}

	public static PlayerStateDTO convertPlayerState(PlayerState ps) {
		PlayerStateDTO res = null;
		if (ps != null) {
			res = new PlayerStateDTO();
			res.setGameId(ps.getGameId());
			res.setPlayerId(ps.getPlayerId());
			res.setState(new HashMap<String, Set<GameConcept>>());
			if (ps.getState() != null) {
				for (GameConcept gc : ps.getState()) {
					String conceptType = gc.getClass().getSimpleName();
					Set<GameConcept> gcSet = res.getState().get(conceptType);
					if (gcSet == null) {
						gcSet = new HashSet<GameConcept>();
						res.getState().put(conceptType, gcSet);
					}
					gcSet.add(gc);
				}
			}
		}
		return res;
	}

	/**
	 * Get game id from process id and game name
	 * 
	 * @param processId
	 * @param name
	 * @return
	 */
	public static String getGameId(String processId, String name) {
		return processId.concat("-").concat(name);
	}

	public static PlayerScore convertPlayerState(StatePersistence state, String pcName, String period, String key) {
		PlayerScore res = null;
		if (state != null) {
			res = new PlayerScore();
			res.setPlayerId(state.getPlayerId());
			if (period.equalsIgnoreCase("global")) {
				if (state.getConcepts().get("PointConcept").containsKey(pcName)) {
					res.setScore(state.getGeneralItemScore(pcName));
				}
			} else {
				res.setScore(state.getIncrementalScore(pcName, "weekly", key));
			}
		}
		return res;
	}

}
