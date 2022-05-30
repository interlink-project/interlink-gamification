package eu.fbk.interlink.gamification.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Document;
import eu.trentorise.game.model.core.GameConcept;
import eu.trentorise.game.model.Action;
import eu.trentorise.game.model.ChallengeConcept;
import eu.trentorise.game.model.Level;
import eu.trentorise.game.model.PointConcept;
import eu.trentorise.game.model.core.DBRule;

@Document(collection = "templates")
public class InterlinkGameTemplate extends InterlinkGame{
	
	protected String filename;
	protected String description; 
	
	
	
	
	public InterlinkGameTemplate() {

	}

	public InterlinkGameTemplate (String id, String name, String fileName, String description, List<String> tags, List<InterlinkTask> tasks) {
		tagList = new ArrayList<String>();
		taskList = new ArrayList<InterlinkTask>();
		if (tags != null) 
			tagList = tags;
		if (tasks != null) 
			taskList = tasks;
		this.id = id;
		this.name = name;
		this.filename = fileName;
		this.description = description;
	}
	

	public String getDescription() {
		return description;
	}




	public void setDescription(String description) {
		this.description = description;
	}




	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	


	@Override
	public String toString() {
		return "{" 
				+ "id=" + this.getId() 
				+ ", name='" + this.getName() + '\'' 
				+ ", processId='" + this.getProcessId() + '\'' 
				+", tagList=[" + this.getTagList()  
				+", taskList=[" + this.getTaskList()   
				+ "]}";
	}

}
