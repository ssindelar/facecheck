package de.imagecheck.backend.profile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import de.imagecheck.model.CloudServices;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class ProfileService {

	private static final String DEFAULT_PROFILE_ID = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);

	private final Map<String, Profile> profiles = new HashMap<>();

	void onStart(@Observes StartupEvent ev) {
		LOGGER.info("The application is starting...");
		// default
		ModerationProfile moderation = ModerationProfile.builder().build();
		LabelProfile label = LabelProfile.builder().build();
		FaceProfile face = FaceProfile.builder().build();
		CelebrityProfile celebrity = CelebrityProfile.builder().build();
		TextProfile text = TextProfile.builder()
				.cloudServices(Arrays.asList(CloudServices.GOOGLE_CLOUD_VISION))
				.build();
		WebProfile web = WebProfile.builder().cloudServices(Arrays.asList(CloudServices.GOOGLE_CLOUD_VISION)).build();
		Profile defaultProfile = Profile.builder()
				.id(DEFAULT_PROFILE_ID)
				.moderation(moderation)
				.label(label)
				.face(face)
				.celebrity(celebrity)
				.text(text)
				.web(web)
				.build();
		save(defaultProfile);

		// gallery (Beliebig viele Gesicher erlaubt)
		FaceProfile galleryFace = FaceProfile.builder().minFaces(0).maxFaces(Integer.MAX_VALUE).build();
		Profile galleryProfile = defaultProfile.toBuilder().id("gallery").face(galleryFace).build();
		save(galleryProfile);
	}

	void onStop(@Observes ShutdownEvent ev) {
		LOGGER.info("The application is stopping...");
	}

	private void save(Profile profile) {
		profiles.put(profile.getId(), profile);
	}

	public Profile getById(String id) {
		Profile profile = profiles.get(Strings.nullToEmpty(id));
		if (profile == null) {
			profile = profiles.get(DEFAULT_PROFILE_ID);
		}
		return profile;
	}

}
