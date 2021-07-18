package de.facecheck.model;

import lombok.Value;

@Value
public class ImageLabel implements NameWithConfidence {
	String name;
	double confidence;
}
