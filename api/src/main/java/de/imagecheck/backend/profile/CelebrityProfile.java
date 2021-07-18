package de.imagecheck.backend.profile;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CelebrityProfile extends AbstractCloudServiceAware {

	@Builder.Default
	private int detectLevel = 50;
	@Builder.Default
	private int checkLevel = 85;
	@Builder.Default
	private int denyLevel = 95;
}