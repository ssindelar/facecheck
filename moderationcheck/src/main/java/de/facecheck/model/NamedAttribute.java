package de.facecheck.model;

import lombok.Value;

@Value
public class NamedAttribute{
	String name;
	double confidence;
}