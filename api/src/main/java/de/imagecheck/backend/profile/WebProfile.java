package de.imagecheck.backend.profile;

import de.imagecheck.model.Recommendation;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class WebProfile extends AbstractCloudServiceAware {

	@Builder.Default
	private Recommendation foundRecommendation = Recommendation.CHECK;
}