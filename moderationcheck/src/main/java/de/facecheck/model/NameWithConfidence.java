package de.facecheck.model;

public interface NameWithConfidence extends Comparable<NameWithConfidence> {

	String getName();

	double getConfidence();

	default int compareTo(NameWithConfidence other) {
		if (getConfidence() > other.getConfidence()) {
			return 1;
		} else if (getConfidence() == other.getConfidence()) {
			return 0;
		} else {
			return -1;
		}
	}

}
