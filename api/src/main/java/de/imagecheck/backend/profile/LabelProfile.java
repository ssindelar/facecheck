package de.imagecheck.backend.profile;

import java.util.ArrayList;
import java.util.List;

import de.imagecheck.model.Recommendation;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class LabelProfile extends AbstractCloudServiceAware {

	@Builder.Default
	private int detectLevel = 50;

	@Builder.Default
	private int maxLabel = 30;

	@Builder.Default
	private List<CriticalLabel> criticalLabels = new ArrayList();

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