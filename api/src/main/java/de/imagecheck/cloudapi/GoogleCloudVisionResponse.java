package de.imagecheck.cloudapi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.WebDetection;

import de.imagecheck.backend.analysis.AnalysisBoundingBox;
import de.imagecheck.backend.analysis.AnalysisLabel;
import de.imagecheck.backend.analysis.AnalysisModerationIssue;
import de.imagecheck.backend.analysis.AnalysisText;
import de.imagecheck.backend.analysis.AnalysisWebDetect;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleCloudVisionResponse {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudVisionResponse.class);
	private static final double CONFIDENCE_FACTOR = 20;

	private final Future<AnnotateImageResponse> requestJob;

	public List<AnalysisModerationIssue> getModerationIssues() {
		// Ergebnis mappen
		SafeSearchAnnotation annotation = getResponse().getSafeSearchAnnotation();
		List<AnalysisModerationIssue> issues = new ArrayList<>();
		LOGGER.debug("Ergebnis: adult: {} medical: {} spoofed: {} violence: {} racy: {} ", annotation.getAdult(),
				annotation.getMedical(), annotation.getSpoof(), annotation.getViolence(), annotation.getRacy());
		if (annotation.getAdultValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new AnalysisModerationIssue("Explicit", annotation.getAdultValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getMedicalValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new AnalysisModerationIssue("Medical", annotation.getMedicalValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getSpoofValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new AnalysisModerationIssue("Spoof", annotation.getSpoofValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getViolenceValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new AnalysisModerationIssue("Violence", annotation.getViolenceValue() * CONFIDENCE_FACTOR));
		}
		if (annotation.getRacyValue() > Likelihood.UNKNOWN_VALUE) {
			issues.add(new AnalysisModerationIssue("Suggestive", annotation.getRacyValue() * CONFIDENCE_FACTOR));
		}

		return issues;
	}

	public List<AnalysisLabel> getLabels() {
		return getResponse().getLabelAnnotationsList()
				.stream()
				.map(e -> new AnalysisLabel(e.getDescription().toLowerCase(), e.getScore() * 100))
				.collect(Collectors.toList());
	}

	public List<AnalysisText> getTexts() {
		return getResponse().getTextAnnotationsList()
				.stream()
				.map(i -> AnalysisText.builder()
						.text(i.getDescription())
						.boundingBox(mapBoundingBox(i.getBoundingPoly()))
						.build())
				.collect(Collectors.toList());
	}

	public List<AnalysisWebDetect> getWebDetect() {
		WebDetection webDetection = getResponse().getWebDetection();
		return webDetection.getPagesWithMatchingImagesList().stream().map(w -> {
			String websiteUrl = w.getUrl();
			String imageUrl = Stream
					.concat(w.getFullMatchingImagesList().stream(), w.getPartialMatchingImagesList().stream())
					.findFirst()
					.orElseThrow(
							() -> new IllegalStateException("Webseite mit Duplikat gefunden aber keine URL zum Bild"))
					.getUrl();
			return new AnalysisWebDetect(websiteUrl, w.getPageTitle(), imageUrl);
		}).collect(Collectors.toList());
	}

	private AnnotateImageResponse getResponse() {
		try {
			AnnotateImageResponse response = this.requestJob.get(CloudConstants.TIME, CloudConstants.TIMEUNIT);
			if(response == null) {
				throw new IllegalStateException("No Response was returned. Error occured during request.");
			}
			return response;
		} catch (ExecutionException | TimeoutException e) {
			throw new IllegalStateException("Fehler in Abfrage von Ergebnissen", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Warten unterbrochen", e);
		}
	}

	private AnalysisBoundingBox mapBoundingBox(BoundingPoly boundingPoly) {
		// TODO: Umrechnen in relative Koordinaten
		return null;
	}
}
