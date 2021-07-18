package de.imagecheck.backend.analysis;

import java.util.Collections;
import java.util.List;

import de.imagecheck.model.NameWithConfidence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisLabel implements NameWithConfidence {

	private String name;
	private double confidence;
	private List<String> parents;
	private List<LabelInstance> instances;

	public AnalysisLabel(String name, double confidence) {
		super();
		this.name = name;
		this.confidence = confidence;
		this.parents = Collections.emptyList();
		this.instances = Collections.emptyList();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class LabelInstance {
		private double confidence;
		private AnalysisBoundingBox boundingBox;
	}

}
