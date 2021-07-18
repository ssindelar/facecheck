package de.facecheck.model;

import lombok.Value;

@Value
public class ModerationIssue implements NameWithConfidence {
	String name;
	double confidence;
}
