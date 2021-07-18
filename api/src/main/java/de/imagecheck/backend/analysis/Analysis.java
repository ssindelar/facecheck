package de.imagecheck.backend.analysis;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Analysis {

	@Builder.Default
	private String id = UUID.randomUUID().toString();
	private String imageId;
	private String profileId;
	private String imageUrl;

	List<AnalysisModerationIssue> moderation;

	List<AnalysisLabel> labels;

	List<AnalysisFace> faces;

	List<AnalysisCelebrity> celebrities;

	List<AnalysisText> texts;

	List<AnalysisWebDetect> webDetect;

}
