package de.imagecheck.businesslogic;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.imagecheck.backend.analysis.Analysis;
import de.imagecheck.backend.analysis.Analysis.AnalysisBuilder;
import de.imagecheck.backend.analysis.AnalysisCelebrity;
import de.imagecheck.backend.analysis.AnalysisFace;
import de.imagecheck.backend.analysis.AnalysisLabel;
import de.imagecheck.backend.analysis.AnalysisModerationIssue;
import de.imagecheck.backend.analysis.AnalysisService;
import de.imagecheck.backend.analysis.AnalysisText;
import de.imagecheck.backend.analysis.AnalysisWebDetect;
import de.imagecheck.backend.profile.Profile;
import de.imagecheck.cloudapi.AwsRekognition;
import de.imagecheck.cloudapi.AwsRekognitionResponse;
import de.imagecheck.cloudapi.GoogleCloudVision;
import de.imagecheck.cloudapi.GoogleCloudVisionResponse;
import de.imagecheck.model.NameWithConfidence;

@Singleton
public class AnalysisController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisController.class);
	private final MessageDigest digest = DigestUtils.getSha256Digest();

	@Inject
	AwsRekognition awsRekognition;

	@Inject
	GoogleCloudVision googleCloudVision;

	@Inject
	AnalysisService service;

	public Analysis analyze(byte[] image, String url, Profile profile) throws AnalysisException {

		AnalysisBuilder builder = Analysis.builder().profileId(profile.getId()).imageUrl(url);
		LOGGER.debug("Starte analyse von url: '{}' mit Profile: {}", url, profile);
		try {
			AwsRekognitionResponse awsResponse = awsRekognition.request(profile, image);
			GoogleCloudVisionResponse googleResponse = googleCloudVision.request(profile, image);

			// ImageId für Duplikaterkennung
			String imageId = Base64.getEncoder().encodeToString(digest.digest(image));
			builder.imageId(imageId);

			// Moderation
			if (profile.getModeration().isActive()) {
				List<AnalysisModerationIssue> moderationIssues = moderation(awsResponse, googleResponse);
				builder.moderation(moderationIssues);
			}

			// Labels
			if (profile.getLabel().isActive()) {
				List<AnalysisLabel> labels = labels(awsResponse, googleResponse);
				builder.labels(labels);
			}

			// Gesichererkennung
			if (profile.getFace().isActive()) {
				List<AnalysisFace> faces = faces(awsResponse);
				builder.faces(faces);
			}

			// Promis
			if (profile.getCelebrity().isActive()) {
				List<AnalysisCelebrity> celebrities = celebrities(awsResponse);
				builder.celebrities(celebrities);
			}

			// Text
			if (profile.getText().isActive()) {
				List<AnalysisText> texts = text(googleResponse);
				builder.texts(texts);
			}

			// Web
			if (profile.getWeb().isActive()) {
				List<AnalysisWebDetect> web = web(googleResponse);
				builder.webDetect(web);
			}

			Analysis analysis = builder.build();
			service.insert(analysis);
			LOGGER.debug("Analyse von url '{}' abgeschlossen", url);
			return analysis;
		} catch (Exception e) {
			throw new AnalysisException("Error while analyzing image.", e);
		}
	}

	private List<AnalysisModerationIssue> moderation(AwsRekognitionResponse awsResponse,
			GoogleCloudVisionResponse googleResponse)
			throws InterruptedException, ExecutionException, TimeoutException {
		return NameWithConfidence.mergeAndOrder(awsResponse.getModerationLabels(),
				googleResponse.getModerationIssues());
	}

	private List<AnalysisLabel> labels(AwsRekognitionResponse awsResponse, GoogleCloudVisionResponse googleResponse)
			throws InterruptedException, ExecutionException, TimeoutException {
		return NameWithConfidence.mergeAndOrder(awsResponse.getLabels(), googleResponse.getLabels());
	}

	private List<AnalysisFace> faces(AwsRekognitionResponse awsResponse)
			throws InterruptedException, ExecutionException, TimeoutException {
		return awsResponse.getFaces();
	}

	private List<AnalysisCelebrity> celebrities(AwsRekognitionResponse awsResponse)
			throws InterruptedException, ExecutionException, TimeoutException {
		return awsResponse.getCelebrities();
	}

	private List<AnalysisText> text(GoogleCloudVisionResponse gcvResponse) {
		return gcvResponse.getTexts();
	}

	private List<AnalysisWebDetect> web(GoogleCloudVisionResponse googleResponse) {
		return googleResponse.getWebDetect();
	}

	public static class AnalysisException extends Exception {
		private static final long serialVersionUID = -7186985795101529194L;

		public AnalysisException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
