package de.imagecheck.resources.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import de.imagecheck.backend.analysis.Analysis;
import de.imagecheck.backend.analysis.AnalysisCelebrity;
import de.imagecheck.backend.analysis.AnalysisFace;
import de.imagecheck.backend.analysis.AnalysisFace.AgeRange;
import de.imagecheck.backend.analysis.AnalysisLabel;
import de.imagecheck.backend.analysis.AnalysisModerationIssue;
import de.imagecheck.backend.analysis.AnalysisText;
import de.imagecheck.backend.analysis.AnalysisWebDetect;
import de.imagecheck.backend.profile.CelebrityProfile;
import de.imagecheck.backend.profile.FaceProfile;
import de.imagecheck.backend.profile.LabelProfile;
import de.imagecheck.backend.profile.LabelProfile.CriticalLabel;
import de.imagecheck.backend.profile.ModerationProfile;
import de.imagecheck.backend.profile.Profile;
import de.imagecheck.backend.profile.ProfileService;
import de.imagecheck.backend.profile.TextProfile;
import de.imagecheck.backend.profile.WebProfile;
import de.imagecheck.businesslogic.AnalysisController;
import de.imagecheck.model.BooleanAttribute;
import de.imagecheck.model.NameWithConfidence;
import de.imagecheck.model.NamedAttribute;
import de.imagecheck.model.Recommendation;
import de.imagecheck.resources.v1.model.FacesRest;
import de.imagecheck.resources.v1.model.FacesRest.FaceRest;
import de.imagecheck.resources.v1.model.FacesRest.FacesRestBuilder;
import de.imagecheck.resources.v1.model.ImageCheckRequest;
import de.imagecheck.resources.v1.model.ImageCheckResponse;
import de.imagecheck.resources.v1.model.ImageCheckResponse.AgeRangeRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.BooleanAttributeRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.CelebritiesRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.CelebritiesRest.CelebritiesRestBuilder;
import de.imagecheck.resources.v1.model.ImageCheckResponse.ImageCheckResponseBuilder;
import de.imagecheck.resources.v1.model.ImageCheckResponse.LabelsRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.LabelsRest.LabelsRestBuilder;
import de.imagecheck.resources.v1.model.ImageCheckResponse.ModerationIssueRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.ModerationRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.ModerationRest.ModerationRestBuilder;
import de.imagecheck.resources.v1.model.ImageCheckResponse.NamedAttributeRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.TextRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.TextRest.TextRestBuilder;
import de.imagecheck.resources.v1.model.WebDetectRest;
import de.imagecheck.resources.v1.model.WebDetectRest.MatchingWebsiteRest;
import de.imagecheck.resources.v1.model.WebDetectRest.WebDetectRestBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Path("facecheck")
public class ImageCheckResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageCheckResource.class);
	private static final int IMAGE_SIZE_LIMIT = 5000000;

	@Inject
	ProfileService profileService;

	@Inject
	AnalysisController analysisController;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("")
	public Response check(ImageCheckRequest request) {

		// Prüfen ob source angeben wurde
		if (request.getImage() == null && request.getLink() == null) {
			return error(Status.BAD_REQUEST, "No source provided");
		}

		// Profile auslesen
		Profile profile = profileService.getById(request.getProfileId());
		if (profile == null) {
			return error(Status.BAD_REQUEST, "Profile unknown");
		}

		// Bild runterladen/dekodieren
		byte[] image;
		try {
			image = getImage(request);
		} catch (IOException e) {
			LOGGER.error("Fehler beim Runerladen des Bildes von: '{}'", request.getLink(), e);
			return error(Status.SERVICE_UNAVAILABLE, "Couldn't download image");
		}

		// Bildgröße prüfen
		if (image.length > IMAGE_SIZE_LIMIT) {
			return error(Status.REQUEST_ENTITY_TOO_LARGE, "Image to large");
		}

		try {
			Analysis analysis = analysisController.analyze(image, request.getLink(), profile);

			ImageCheckResponseBuilder responseBuilder = ImageCheckResponse.builder().id(analysis.getId());
			responseBuilder.moderation(mapModeration(analysis.getModeration(), profile.getModeration()));
			responseBuilder.labels(mapLabels(analysis.getLabels(), profile.getLabel()));
			responseBuilder.face(mapFaces(analysis.getFaces(), profile.getFace()));
			responseBuilder.texts(mapTexts(analysis.getTexts(), profile.getText()));
			responseBuilder.webDetect(mapWebDetect(analysis.getWebDetect(), profile.getWeb()));
			responseBuilder.celebrities(mapCelebrities(analysis.getCelebrities(), profile.getCelebrity()));

			return Response.ok(responseBuilder.build()).build();

		} catch (Exception e) {
			LOGGER.error("Fehler beim Abfragen der Daten aus den Cloud APIs.", e);
			return error(Status.INTERNAL_SERVER_ERROR, "Error while retrieving Data from cloud APIs.");
		}
	}

	private byte[] getImage(ImageCheckRequest request) throws IOException {
		String base64Image = request.getImage();
		if (base64Image != null) {
			return Base64.getDecoder().decode(base64Image);
		} else if (request.getLink() != null) {
			return readLink(request.getLink());
		} else {
			throw new IllegalArgumentException("No source provided");
		}
	}

	private byte[] readLink(String link) throws IOException {
		URL url = new URL(link);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try (InputStream inputStream = url.openStream()) {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (-1 != (n = inputStream.read(buffer))) {
				output.write(buffer, 0, n);
			}
		}

		return output.toByteArray();
	}


	private ModerationRest mapModeration(List<AnalysisModerationIssue> issues, ModerationProfile profile) {
		ModerationRestBuilder moderationRest = ModerationRest.builder().recommendation(Recommendation.ALLOW);
		if (issues == null) {
			return moderationRest.build();
		}

		issues = issues.stream()
				.filter(i -> i.getParent() == null)
				.filter(NameWithConfidence.filter(profile.getDetectLevel()))
				.collect(Collectors.toList());
		if (issues.isEmpty()) {
			return moderationRest.build();
		}
		double maxConfidence = issues.get(0).getConfidence();

		Recommendation recommendation = Recommendation.ALLOW;
		if (maxConfidence > profile.getDenyLevel()) {
			recommendation = Recommendation.DENY;
		} else if (maxConfidence > profile.getCheckLevel()) {
			recommendation = Recommendation.CHECK;
		}

		List<ModerationIssueRest> restIssues = issues.stream()
				.map(issue -> new ModerationIssueRest(issue.getName(), issue.getConfidence()))
				.collect(Collectors.toList());
		return moderationRest
				.recommendation(recommendation)
				.confidence(maxConfidence)
				.issues(restIssues)
				.build();
	}

	private LabelsRest mapLabels(List<AnalysisLabel> labels, LabelProfile profile) {
		LabelsRestBuilder labelsRest = LabelsRest.builder().recommendation(Recommendation.ALLOW);
		if (labels == null) {
			return labelsRest.build();
		}

		labels = NameWithConfidence.filter(labels, profile.getDetectLevel());
		if (labels.isEmpty()) {
			return labelsRest.build();
		}

		List<NamedAttributeRest> restLabels = labels.stream()
				.map(l -> new NamedAttributeRest(l.getName(), l.getConfidence()))
				.collect(Collectors.toList());

		Map<String, CriticalLabel> criticals = profile.getCriticalLabels()
				.stream()
				.collect(Collectors.toMap(CriticalLabel::getName, Function.identity()));
		List<NamedAttributeRest> criticalLabels = restLabels.stream()
				.filter(l -> criticals.containsKey(l.getName()))
				.filter(l -> {
					CriticalLabel critical = criticals.get(l.getName());
					return !Recommendation.ALLOW.equals(critical.getRecommendation(l.getConfidence()));
				})
				.collect(Collectors.toList());

		Recommendation recommendation = criticalLabels.stream()
				.map(l -> criticals.get(l.getName()).getRecommendation(l.getConfidence()))
				.filter(r -> !Recommendation.ALLOW.equals(r))
				.reduce(Recommendation.ALLOW, Recommendation::worse);

		return labelsRest
				.recommendation(recommendation)
				.labels(restLabels)
				.criticalLabels(criticalLabels)
				.build();
	}

	private FacesRest mapFaces(List<AnalysisFace> faces, FaceProfile profile) {
		FacesRestBuilder facesBuilder = FacesRest.builder().recommandation(Recommendation.ALLOW);
		if (faces == null) {
			return facesBuilder.build();
		}

		int faceCount = faces.size();
		facesBuilder.faceCount(faceCount);

		int minFaces = MoreObjects.firstNonNull(profile.getMinFaces(), 0);
		int maxFaces = MoreObjects.firstNonNull(profile.getMaxFaces(), Integer.MAX_VALUE);

		if (faceCount >= minFaces && faceCount <= maxFaces) {
			facesBuilder.recommandation(profile.getMatchingRecommendation());
		} else {
			facesBuilder.recommandation(profile.getNotMatchingRecommendation());
		}

		if (faceCount == 0) {
			return facesBuilder.build();
		}

		Comparator<AnalysisFace> comp = Comparator.comparing(f -> f.getBoundingBox().getSize());
		List<AnalysisFace> sortedFaces = faces.stream()
				.sorted(comp.reversed())
				.collect(Collectors.toList());

		AnalysisFace largestFace = sortedFaces.get(0);
		FaceRest faceRest = FaceRest.builder()
				.ageRange(map(largestFace.getAgeRange()))
				.smile(map(largestFace.getSmile()))
				.glasses(map(largestFace.getGlasses()))
				.sunGlasses(map(largestFace.getSunGlasses()))
				.gender(map(largestFace.getGender()))
				.beard(map(largestFace.getBeard()))
				.mustache(map(largestFace.getMustache()))
				.eyesOpen(map(largestFace.getEyesOpen()))
				.mouthOpen(map(largestFace.getMouthOpen()))
				.build();
		return facesBuilder.mainFace(faceRest).build();
	}

	private AgeRangeRest map(AgeRange ageRange) {
		return new AgeRangeRest(ageRange.getLow(), ageRange.getHigh());
	}

	private BooleanAttributeRest map(BooleanAttribute attribute) {
		return new BooleanAttributeRest(attribute.isValue(), attribute.getConfidence());
	}

	private NamedAttributeRest map(NamedAttribute attribute) {
		return new NamedAttributeRest(attribute.getName(), attribute.getConfidence());
	}

	private Response error(Status status, String message) {
		return Response.status(status).entity(message).header("Content-Type", "text/plain").build();
	}

	private TextRest mapTexts(List<AnalysisText> texts, TextProfile profile) {
		TextRestBuilder textRest = TextRest.builder().recommendation(Recommendation.ALLOW);
		if (texts == null || texts.isEmpty()) {
			return textRest.build();
		}

		return textRest.recommendation(profile.getFoundRecommendation())
				.text(texts.get(0).getText().replaceAll("\n", " ").trim())
				.build();
	}

	private WebDetectRest mapWebDetect(List<AnalysisWebDetect> websites, WebProfile profile) {
		WebDetectRestBuilder webDetect = WebDetectRest.builder().recommendation(Recommendation.ALLOW);
		if (websites == null || websites.isEmpty()) {
			return webDetect.build();
		}
		List<MatchingWebsiteRest> websitesRest = websites.stream().map(this::mapWebsite).collect(Collectors.toList());
		return webDetect.websites(websitesRest).recommendation(profile.getFoundRecommendation()).build();
	}

	private MatchingWebsiteRest mapWebsite(AnalysisWebDetect web) {
		return MatchingWebsiteRest.builder()
				.websiteURL(web.getWebsiteURL())
				.imageURL(web.getImageURL())
				.pageTitle(web.getPageTitle())
				.build();
	}

	private CelebritiesRest mapCelebrities(List<AnalysisCelebrity> celebrities, CelebrityProfile profile) {
		CelebritiesRestBuilder celebritiesRest = CelebritiesRest.builder().recommendation(Recommendation.ALLOW);
		if (celebrities == null || celebrities.isEmpty()) {
			return celebritiesRest.build();
		}

		List<NamedAttributeRest> mappedCelebrities = celebrities.stream()
				.filter(c -> c.getConfidence() >= profile.getDetectLevel())
				.map(c -> new NamedAttributeRest(c.getName(), c.getConfidence()))
				.collect(Collectors.toList());
		// nach filtern nichts mehr übrig
		if (mappedCelebrities.isEmpty()) {
			return celebritiesRest.build();
		}

		double confidence = mappedCelebrities.stream().mapToDouble(NamedAttributeRest::getConfidence).max().orElse(0);
		Recommendation recommendation = Recommendation.ALLOW;
		if (confidence >= profile.getDenyLevel()) {
			recommendation = Recommendation.DENY;
		} else if (confidence >= profile.getCheckLevel()) {
			recommendation = Recommendation.CHECK;
		}

		return celebritiesRest.recommendation(recommendation)
				.celebreties(mappedCelebrities)
				.confidence(confidence)
				.build();
	}

}
