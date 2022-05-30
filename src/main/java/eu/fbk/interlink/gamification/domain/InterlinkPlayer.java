package eu.fbk.interlink.gamification.domain;

import java.util.Objects;

import org.springframework.data.annotation.Id;

public class InterlinkPlayer {
	
	
	private @Id String id;
	
	private String name;
	
	private int management; 
	
	private int development;
	
	private int exploitation;
	
	public InterlinkPlayer() {
		
	}
	
	public InterlinkPlayer(String id, String name, int management, int development, int  exploitation) {
		this.id = id;
		this.name = name;
		this.development = development;
		this.management = management;
		this.exploitation = exploitation;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getManagement() {
		return management;
	}

	public void setManagement(int management) {
		this.management = management;
	}

	public int getDevelopment() {
		return development;
	}

	public void setDevelopment(int development) {
		this.development = development;
	}

	public int getExploitation() {
		return exploitation;
	}

	public void setExploitation(int exploitation) {
		this.exploitation = exploitation;
	}
	
	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (!(o instanceof InterlinkPlayer))
			return false;
		InterlinkPlayer player = (InterlinkPlayer) o;
		return Objects.equals(this.id, player.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public String toString() {
		return "{" + "id='" + this.id 
				+ "', name ='" + name
				+ "', management=" + this.management 
				+ ", development=" + this.development 
				+ ", exploitation=" + this.exploitation 
				
				+ "}";
	}

}
