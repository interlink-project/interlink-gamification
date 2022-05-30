package eu.fbk.interlink.gamification.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class InterlinkTask {
	
	private String id;
	private double management = 0;
	private double development = 0;
	private double exploitation = 0;
	private boolean completed = false;
	private List<InterlinkPlayer> playerList;
	private List<InterlinkTask> subtaskList; 
	
	
	public InterlinkTask() {
		this.playerList = new ArrayList<InterlinkPlayer> ();
		this.subtaskList = new ArrayList<InterlinkTask> ();
	
	}
	
	public InterlinkTask (String id, int management, int development, int exploitation, boolean completed, List<InterlinkTask> subtaskList, List<InterlinkPlayer> playerList) {
		this.id = id;
		this.management = management;
		this.development = development;
		this.exploitation = exploitation;
		this.completed = completed;
		if (subtaskList == null)
			this.subtaskList = new ArrayList<InterlinkTask>();
		else 
			this.subtaskList = subtaskList;
		if (playerList == null)
			this.playerList = new ArrayList<InterlinkPlayer>();
		else 
			this.playerList = playerList;
		
	}
	
	public InterlinkTask (String id) {
		this(id, 0, 0, 0, false, null, null);
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getManagement() {
		return management;
	}

	public void setManagement(double management) {
		this.management = management;
	}

	public double getDevelopment() {
		return development;
	}

	public void setDevelopment(double development) {
		this.development = development;
	}

	public double getExploitation() {
		return exploitation;
	}

	public void setExploitation(double exploitation) {
		this.exploitation = exploitation;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	public List<InterlinkPlayer> getPlayers() {
		return playerList;
	}

	public void setPlayers(List<InterlinkPlayer> playerList) {
		this.playerList = playerList;
	}
	
	public void addPlayer(InterlinkPlayer playerId) {
		this.playerList.add(playerId);
	}
	
	public void removePlayer(InterlinkPlayer playerId) {
		InterlinkPlayer toRemove = null;
		for (InterlinkPlayer player : this.playerList) {
		    if (player.getId().equals(id)) {
		    	toRemove = player;
		    	break;
		    }
		}
		this.playerList.remove(toRemove);
	}
	
	public List<InterlinkTask> getSubtaskList() {
		return subtaskList;
	}

	public void setSubtaskList(List<InterlinkTask> subtaskList) {
		this.subtaskList = subtaskList;
	}

	public void addSubtask(InterlinkTask asset) {
		if (this.subtaskList == null) {
			this.subtaskList = new ArrayList<InterlinkTask>();
		}
		this.subtaskList.add(asset);
	}
	
	public void removeSubtask(String subtaskId) {
		InterlinkTask toRemove = null;
		for (InterlinkTask asset : this.subtaskList) {
		    if (asset.getId().equals(subtaskId)) {
		    	toRemove = asset;
		    	break;
		    }
		}
		this.subtaskList.remove(toRemove);
	}
	
	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (!(o instanceof InterlinkTask))
			return false;
		InterlinkTask task = (InterlinkTask) o;
		return Objects.equals(this.id, task.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public String toString() {
		return "{" + "id='" + this.id 
				+ "', management=" + this.management 
				+ ", development=" + this.development 
				+ ", exploitation=" + this.exploitation 
				+ ", completed='" + this.completed + "'"
				+ "', players = ["+ this.playerList.toString()
				+ "], subtasks = [" + this.subtaskList.toString()+ "]}";
	}
	
	
}
