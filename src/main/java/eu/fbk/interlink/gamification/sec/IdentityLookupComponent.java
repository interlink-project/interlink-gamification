package eu.fbk.interlink.gamification.sec;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.stereotype.Component;

@Component
public class IdentityLookupComponent {
	
	public String getName() {
		return "long-rovereto";
	}

	public String getDomain() {
		return "";
	}

	public Authentication getAuthentication() {
		return null;
	}

}
