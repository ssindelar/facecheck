package de.facecheck.cloudapis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.protobuf.ByteString;

import de.facecheck.model.ImageLabel;
import de.facecheck.model.MatchingWebsite;
import de.facecheck.model.ModerationIssue;
import de.facecheck.model.Profile;

public class GoogleCloudVision {

	private static final double CONFIDENCE_FACTOR = 20;

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudVision.class);

	private final ImageAnnotatorClient client;

	private AnnotateImageResponse response;

	public GoogleCloudVision() throws IOException {
		super();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("gcvision-credentials.json");
		Credentials myCredentials = ServiceAccountCredentials
				.fromStream(stream);

		ImageAnnotatorSettings imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(myCredentials)).build();
		client = ImageAnnotatorClient.create(imageAnnotatorSettings);
	}

	public void request(Profile profile, byte[] imageBytes) {
		ByteString imgBytes = ByteString.copyFrom(imageBytes);
		Image img = Image.newBuilder().setContent(imgBytes).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				// .addFeatures(Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION))
				.addFeatures(Feature.newBuilder().setType(Type.LABEL_DETECTION))
				.addFeatures(Feature.newBuilder().setType(Type.TEXT_DETECTION))
				.addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION))
				// .addFeatures(Feature.newBuilder().setType(Type.FACE_DETECTION))
				.setImage(img)
				.build();

		BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(Arrays.asList(request));
		List<AnnotateImageResponse> respones = batchResponse.getResponsesList();
		if (respones.isEmpty()) {
			LOGGER.error("");
			throw new IllegalStateException(
					"Error while requesting information from Google Vision. No Result returned.");
		}
		
		this.response = respones.get(0);
		if (response.hasError()) {
			String errorMessage = response.getError().getMessage();
			LOGGER.info("Error: {}\n", errorMessage);
			throw new IllegalStateException(
					"Error while requesting information from Google Vision. Fehler in Antwort: " + errorMessage);
		}
	}

	public List<ModerationIssue> getModerationIssues() {
		
		// Ergebnis mappen
		SafeSearchAnnotation annotation = response.getSafeSearchAnnotation();
		List<ModerationIssue> issues = new ArrayList<>();
		LOGGER.debug("Ergebnis: adult: {} medical: {} spoofed: {} violence: {} racy: {} ", annotation.getAdult(),
				annotation.getMedical(), annotation.getSpoof(), annotation.getViolence(), annotation.getRacy());
		if (annotation.getAdultValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new ModerationIssue("Explicit", annotation.getAdultValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getMedicalValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new ModerationIssue("Medical", annotation.getMedicalValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getSpoofValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new ModerationIssue("Spoof", annotation.getSpoofValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getViolenceValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new ModerationIssue("Violence", annotation.getViolenceValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getRacyValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new ModerationIssue("Suggestive", annotation.getRacyValue() * CONFIDENCE_FACTOR));
		}
		
		return issues;
	}

	public List<ImageLabel> getLabels() {
		return response.getLabelAnnotationsList()
				.stream()
				.map(e -> new ImageLabel(e.getDescription().toLowerCase(), e.getScore() * 100))
				.collect(Collectors.toList());
	}

	public List<String> getTexts() {
		return response.getTextAnnotationsList()
				.stream()
				.map(EntityAnnotation::getDescription)
				.collect(Collectors.toList());
	}

	public List<MatchingWebsite> getWebDetect() {
		WebDetection webDetection = response.getWebDetection();
		return webDetection.getPagesWithMatchingImagesList().stream().map(w -> {
			String websiteUrl = w.getUrl();
			String imageUrl = Stream
					.concat(w.getFullMatchingImagesList().stream(), w.getPartialMatchingImagesList().stream())
					.findFirst()
					.orElseThrow(
							() -> new IllegalStateException("Webseite mit Duplikat gefunden aber keine URL zum Bild"))
					.getUrl();
			return new MatchingWebsite(websiteUrl, w.getPageTitle(), imageUrl);
		}).collect(Collectors.toList());
	}

}
