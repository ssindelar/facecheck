package de.imagecheck.backend.analysis;

import lombok.Value;

@Value
public class AnalysisWebDetect {
	String websiteURL;
	String pageTitle;
	String imageURL;
}