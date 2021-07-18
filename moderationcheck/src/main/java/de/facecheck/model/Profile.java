package de.facecheck.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Profile {

	private int moderationCheckLevel;
	private int moderationDenyLevel;
	private List<CriticalLabel> criticalLabels;

	private Integer allowedFaceCount;

	@Data
	@Builder
	public static class CriticalLabel {
		private String name;
		private Integer checkThreshold;
		private Integer denyThreshold;

		public Recommendation getRecommendation(double confidence) {
			if (denyThreshold != null && confidence > denyThreshold) {
				return Recommendation.DENY;
			} else if (checkThreshold != null && confidence > checkThreshold) {
				return Recommendation.CHECK;
			} else {
				return Recommendation.ALLOW;
			}
		}
	}
}
