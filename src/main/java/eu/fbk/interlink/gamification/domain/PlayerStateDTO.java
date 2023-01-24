package eu.fbk.interlink.gamification.domain;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.trentorise.game.model.CustomData;
import eu.trentorise.game.model.Inventory;
import eu.trentorise.game.model.PlayerLevel;
import eu.trentorise.game.model.core.GameConcept;
import eu.trentorise.game.repo.ChallengeConceptPersistence;

@JsonInclude(Include.NON_NULL)
public class PlayerStateDTO {
	private String id;
	private String playerId;
	private String gameId;

	private Map<String, Set<GameConcept>> state = new HashMap<String, Set<GameConcept>>();
    private List<PlayerLevel> levels = new ArrayList<>();

    private Inventory inventory;

	private CustomData customData = new CustomData();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public Map<String, Set<GameConcept>> getState() {
		return state;
	}

	public void setState(Map<String, Set<GameConcept>> state) {
		this.state = state;
	}

	public CustomData getCustomData() {
		return customData;
	}

	public void setCustomData(CustomData customData) {
		this.customData = customData;
	}

    public List<PlayerLevel> getLevels() {
        return levels;
    }

    public void setLevels(List<PlayerLevel> levels) {
        this.levels = levels;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

	public void loadChallengeConcepts(List<ChallengeConceptPersistence> listCcps) {
		listCcps.forEach(ccp -> {
			String conceptType = ccp.getConcept().getClass().getSimpleName();
			Set<GameConcept> gcSet = state.get(conceptType);
			if (gcSet == null) {
				gcSet = new HashSet<GameConcept>();
				state.put(conceptType, gcSet);
			}
			gcSet.add(ccp.getConcept());
		});
	}

}
