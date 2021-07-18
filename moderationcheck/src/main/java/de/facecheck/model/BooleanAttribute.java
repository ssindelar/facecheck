package de.facecheck.model;

import lombok.Value;

@Value
public class BooleanAttribute {
	boolean value;
	double confidence;
}