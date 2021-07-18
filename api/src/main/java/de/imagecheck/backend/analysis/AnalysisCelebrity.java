package de.imagecheck.backend.analysis;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCelebrity {
	private String name;
	private String awsId;
	private AnalysisBoundingBox boundingBox;
	private List<String> urls;
	private double confidence;
}
