package de.imagecheck.backend.profile;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ModerationProfile extends AbstractCloudServiceAware {

	@Builder.Default
	private int detectLevel = 50;
	@Builder.Default
	private int checkLevel = 50;
	@Builder.Default
	private int denyLevel = 90;
	

}