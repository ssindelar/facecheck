package de.imagecheck.backend.profile;

import de.imagecheck.model.Recommendation;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class FaceProfile extends AbstractCloudServiceAware {

	@Builder.Default
	private Integer minFaces = 1;

	@Builder.Default
	private Integer maxFaces = 1;

	@Builder.Default
	private Recommendation matchingRecommendation = Recommendation.ALLOW;

	@Builder.Default
	private Recommendation notMatchingRecommendation = Recommendation.CHECK;

}