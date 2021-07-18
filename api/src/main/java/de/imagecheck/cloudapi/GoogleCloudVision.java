package de.imagecheck.cloudapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageRequest.Builder;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;

import de.imagecheck.backend.profile.FaceProfile;
import de.imagecheck.backend.profile.LabelProfile;
import de.imagecheck.backend.profile.Profile;
import de.imagecheck.backend.profile.TextProfile;
import de.imagecheck.backend.profile.WebProfile;
import de.imagecheck.model.CloudServices;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class GoogleCloudVision {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudVision.class);

	private final ImageAnnotatorClient client;

	private ExecutorService executor;

	public GoogleCloudVision() throws IOException {
		super();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("gcvision-credentials.json");
		Credentials myCredentials = ServiceAccountCredentials.fromStream(stream);

		ImageAnnotatorSettings imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
				.build();
		client = ImageAnnotatorClient.create(imageAnnotatorSettings);
	}

	void onStart(@Observes StartupEvent ev) {
		LOGGER.info("State GoogleCloudVision");
		executor = Executors.newFixedThreadPool(4,
				new ThreadFactoryBuilder().setDaemon(true).setNameFormat("googlevision-%d").build());
	}

	void onStop(@Observes ShutdownEvent ev) {
		executor.shutdownNow();
	}
	public GoogleCloudVisionResponse request(Profile profile, byte[] imageBytes) {
		ByteString imgBytes = ByteString.copyFrom(imageBytes);
		Image img = Image.newBuilder().setContent(imgBytes).build();

		// Moderation
		Builder requestBuilder = AnnotateImageRequest.newBuilder();
		if (profile.getModeration().isActive(CloudServices.GOOGLE_CLOUD_VISION)) {
			requestBuilder.addFeatures(Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION));
		}

		// Label
		LabelProfile label = profile.getLabel();
		if (label.isActive(CloudServices.GOOGLE_CLOUD_VISION)) {
			requestBuilder
					.addFeatures(Feature.newBuilder().setType(Type.LABEL_DETECTION).setMaxResults(label.getMaxLabel()));
		}

		// Face
		FaceProfile face = profile.getFace();
		if (face.isActive(CloudServices.GOOGLE_CLOUD_VISION)) {
			requestBuilder.addFeatures(Feature.newBuilder().setType(Type.FACE_DETECTION));
		}

		// Text
		TextProfile text = profile.getText();
		if (text.isActive(CloudServices.GOOGLE_CLOUD_VISION)) {
			requestBuilder.addFeatures(Feature.newBuilder().setType(Type.TEXT_DETECTION));
		}

		// Web
		WebProfile web = profile.getWeb();
		if (web.isActive(CloudServices.GOOGLE_CLOUD_VISION)) {
			requestBuilder.addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION));
		}

		AnnotateImageRequest request = requestBuilder.setImage(img).build();

		Future<AnnotateImageResponse> job = executor.submit(() -> request(request));

		return new GoogleCloudVisionResponse(job);
	}

	private AnnotateImageResponse request(AnnotateImageRequest request) {
		try {
			BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(Arrays.asList(request));
			List<AnnotateImageResponse> respones = batchResponse.getResponsesList();
			if (respones.isEmpty()) {
				LOGGER.error("");
				throw new IllegalStateException(
						"Error while requesting information from Google Vision. No Result returned.");
			}

			AnnotateImageResponse response = respones.get(0);
			if (response.hasError()) {
				String errorMessage = response.getError().getMessage();
				LOGGER.info("Error: {}\n", errorMessage);
				throw new IllegalStateException(
						"Error while requesting information from Google Vision. Fehler in Antwort: " + errorMessage);
			}
			return response;
		} catch (Exception e) {
			LOGGER.error("Fehler in Anfrage.", e);
			return null;
		}
	}

}
