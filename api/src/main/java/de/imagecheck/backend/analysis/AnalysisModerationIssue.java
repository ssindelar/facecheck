package de.imagecheck.backend.analysis;

import de.imagecheck.model.NameWithConfidence;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalysisModerationIssue implements NameWithConfidence {
	String name;
	double confidence;
	String parent;

	public AnalysisModerationIssue(String name, double confidence) {
		super();
		this.name = name;
		this.confidence = confidence;
		this.parent = null;
	}

}
