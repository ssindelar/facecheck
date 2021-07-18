package de.imagecheck.backend.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisText {

	private String text;
	private AnalysisBoundingBox boundingBox;

}
