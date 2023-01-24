package eu.fbk.interlink.gamification.domain;

public class PlayerScore {

	private String playerId;
	private Double score;

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

}
