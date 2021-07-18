package de.imagecheck.model;

public enum Recommendation {
	DENY, CHECK, ALLOW;
	
	public Recommendation worse(Recommendation other) {
		if(this == DENY || other == DENY) {
			return DENY;
		} else if (this == CHECK || other == CHECK) {
			return CHECK;
		} else {
			return ALLOW;
		}
	}
}