package de.imagecheck.backend.profile;

import java.util.List;

import com.google.common.collect.Lists;

import de.imagecheck.model.CloudServices;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@RequiredArgsConstructor
@SuperBuilder
public abstract class AbstractCloudServiceAware {

	@Builder.Default
	private final List<CloudServices> cloudServices = Lists.newArrayList(CloudServices.AWS_REKOGNITION);

	public boolean isActive() {
		return !cloudServices.isEmpty();
	}

	public boolean isActive(CloudServices service) {
		return cloudServices.contains(service);
	}

}