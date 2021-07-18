package de.imagecheck.backend.analysis;

import java.util.List;

import de.imagecheck.model.BooleanAttribute;
import de.imagecheck.model.NamedAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisFace {

	private AnalysisBoundingBox boundingBox;
	private AgeRange ageRange;
	private BooleanAttribute smile;
	private BooleanAttribute glasses;
	private BooleanAttribute sunGlasses;
	private NamedAttribute gender;
	private BooleanAttribute beard;
	private BooleanAttribute mustache;
	private BooleanAttribute eyesOpen;
	private BooleanAttribute mouthOpen;
	private List<NamedAttribute> emotions;
	private Pose pose;
	private Quality quality;
	private double confidence;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Quality {
		private Float brightness;
		private Float sharpness;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Pose {
		private Float roll;
		private Float yaw;
		private Float pitch;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AgeRange {
		private Integer low;
		private Integer high;
	}
}
