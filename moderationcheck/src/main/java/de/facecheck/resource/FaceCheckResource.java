package de.facecheck.resource;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

import de.facecheck.cloudapis.AwsRekognition;
import de.facecheck.cloudapis.GoogleCloudVision;
import de.facecheck.model.BooleanAttribute;
import de.facecheck.model.FacesInfo;
import de.facecheck.model.FacesInfo.Face;
import de.facecheck.model.FacesInfo.Face.AgeRange;
import de.facecheck.model.FacesInfo.Face.BoundingBox;
import de.facecheck.model.ImageLabel;
import de.facecheck.model.MatchingWebsite;
import de.facecheck.model.ModerationIssue;
import de.facecheck.model.NameWithConfidence;
import de.facecheck.model.NamedAttribute;
import de.facecheck.model.Profile;
import de.facecheck.model.Profile.CriticalLabel;
import de.facecheck.model.Recommendation;
import de.facecheck.resource.FaceCheckResponse.FaceCheckResponseBuilder;
import de.facecheck.resource.FaceCheckResponse.LabelsRest;
import de.facecheck.resource.FaceCheckResponse.ModerationIssueRest;
import de.facecheck.resource.FaceCheckResponse.ModerationRest;
import de.facecheck.resource.FaceCheckResponse.NamedAttributeRest;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;

@Path("/facecheck")
public class FaceCheckResource {

	private static final String DEFAULT_PROFILE = "";

	private static final int IMAGE_SIZE_LIMIT = 5000000;

	private static final Logger LOGGER = LoggerFactory.getLogger(FaceCheckResource.class);

	private final MessageDigest digest = DigestUtils.getSha256Digest();

	private final Map<String, Profile> profiles = new HashMap<>();

	private GoogleCloudVision googleCloudVision = new GoogleCloudVision();
	private AwsRekognition awsRekognition = new AwsRekognition();

	public FaceCheckResource() throws IOException {
		super();
		LOGGER.info("Resource created");
		buildProfiles();
	}

	private void buildProfiles() {
		CriticalLabel military = CriticalLabel.builder().name("military").checkThreshold(60).build();
		CriticalLabel soldier = CriticalLabel.builder().name("soldier").checkThreshold(60).build();
		CriticalLabel weaponry = CriticalLabel.builder().name("weaponry").checkThreshold(60).build();
		CriticalLabel weapon = CriticalLabel.builder().name("weapon").checkThreshold(60).build();
		Profile gallery = Profile.builder()
				.moderationCheckLevel(30)
				.moderationDenyLevel(85)
				.criticalLabels(Arrays.asList(military, soldier, weaponry, weapon))
				.build();
		profiles.put("gallery", gallery);

		Profile profile = Profile.builder()
				.moderationCheckLevel(30)
				.moderationDenyLevel(85)
				.allowedFaceCount(1)
				.criticalLabels(Arrays.asList(military, soldier, weaponry, weapon))
				.build();
		profiles.put(DEFAULT_PROFILE, profile);
	}

	@POST
    @Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response moderation(FaceCheckRequest request) throws IOException {

		Stopwatch stopwatch = Stopwatch.createStarted();

		// Prüfen ob source angeben wurde
		if (request.getImage() == null && request.getLink() == null) {
			return Response.status(400).entity("No source provided").build();
		}

		byte[] image = getImage(request);
		// Maximalgröße prüfen
		if (image.length > IMAGE_SIZE_LIMIT) {
			return Response.status(400).entity("Image to large.").build();
		}

		try {
			Profile profile = getProfile(request);

			// Ein Request für alles bei Google
			awsRekognition.request(profile, image);
			googleCloudVision.request(profile, image);

			// ImageId für Duplikaterkennung
			String imageId = Base64.getEncoder().encodeToString(digest.digest(image));
			FaceCheckResponseBuilder responseBuilder = FaceCheckResponse.builder().imageId(imageId).profile(profile);

			// Moderation
			ModerationRest moderation = moderation(profile);
			responseBuilder.moderation(moderation);

			// Bild infos
			List<NamedAttributeRest> labels = labels();
			responseBuilder.labels(labelsRest(labels, profile));

			// Gesichererkennung
			FacesInfo faces = faces();
			responseBuilder.faces(faces);

			// Text
			List<String> texts = texts();
			responseBuilder.texts(texts);

			// Webdetect
			List<MatchingWebsite> websites = webDetect();
			responseBuilder.websites(websites);

			// Celebrities
			List<NamedAttribute> celebrities = celebrities();
			responseBuilder.celebrities(celebrities);

			FaceCheckResponse moderationResponse = responseBuilder.build();
			LOGGER.info("Request time: {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
			return Response.status(200).entity(moderationResponse).build();
		} catch (Exception e) {
			LOGGER.error("Fehler in Anfrage: {}", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.header("mesage", e.getMessage())
					.entity(e.getMessage())
					.build();
		}

    }

	private Profile getProfile(FaceCheckRequest request) {
		String profileId = Strings.nullToEmpty(request.getProfileId());
		Profile profile = profiles.get(profileId);
		if (profile == null) {
			LOGGER.info("unbekanntes Profil angefragt: {}", profileId);
			profile = profiles.get(DEFAULT_PROFILE);
		}
		return profile;
	}

	private byte[] getImage(FaceCheckRequest modRequest) throws IOException {
		String base64Image = modRequest.getImage();
		if (base64Image != null) {
			return Base64.getDecoder().decode(base64Image);
		} else if (modRequest.getLink() != null) {
			return readLink(modRequest.getLink());
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

	<T extends NameWithConfidence> List<T> mergeAndOrder(List<T> list) {
		Map<String, T> map = list.stream()
				.collect(Collectors.toMap(NameWithConfidence::getName, e -> e,
						(a, b) -> a.getConfidence() > b.getConfidence() ? a : b));
		ArrayList<T> filteredList = new ArrayList<>(map.values());
		Collections.sort(filteredList);
		Collections.reverse(filteredList);
		return filteredList;

	}

	/**
	 * Bild auf problematische Inhalte prüfen
	 * 
	 * @param profile
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private ModerationRest moderation(Profile profile) throws InterruptedException, ExecutionException {
		List<ModerationIssue> issues = new ArrayList<>();
		issues.addAll(googleCloudVision.getModerationIssues());
		issues.addAll(awsRekognition.getModerationLabels());
		List<ModerationIssue> mergedModeration = mergeAndOrder(issues);
		
		double maxConfidence = 0;
		if(!mergedModeration.isEmpty()) {
			maxConfidence = mergedModeration.get(0).getConfidence();
		}
		
		Recommendation recommendation = Recommendation.ALLOW;
		if (maxConfidence > profile.getModerationDenyLevel()) {
			recommendation = Recommendation.DENY;
		} else if (maxConfidence > profile.getModerationCheckLevel()) {
			recommendation = Recommendation.CHECK;
		}
		
		List<ModerationIssueRest> restIssues = mergedModeration.stream().map(ModerationIssueRest::new).collect(Collectors.toList());
		return ModerationRest.builder()
				.recommendation(recommendation)
				.confidence(maxConfidence)
				.issues(restIssues)
				.build();
	}
	
	private LabelsRest labelsRest(List<NamedAttributeRest> labels, Profile profile) {
		Map<String, CriticalLabel> criticals = profile.getCriticalLabels()
				.stream()
				.collect(Collectors.toMap(CriticalLabel::getName, Function.identity()));
		List<NamedAttributeRest> criticalLabels = labels.stream()
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
		
		return LabelsRest.builder()
				.recommendation(recommendation)
				.labels(labels)
				.criticalLabels(criticalLabels)
				.build();
	}

	private List<NamedAttributeRest> labels() throws InterruptedException, ExecutionException {
		List<ImageLabel> labels = new ArrayList<>();
		labels.addAll(googleCloudVision.getLabels());
		labels.addAll(awsRekognition.getLabels());
		List<ImageLabel> merged = mergeAndOrder(labels);
		return merged.stream().map(NamedAttributeRest::new).collect(Collectors.toList());
	}

	private FacesInfo faces() throws InterruptedException, ExecutionException {
		List<FaceDetail> awsFaces = awsRekognition.getFaces();
		List<Face> faces = awsFaces.stream().map(fd -> {
			AgeRange ageRange = new AgeRange(fd.ageRange().low(), fd.ageRange().high());
			NamedAttribute gender = new NamedAttribute(fd.gender().valueAsString(), fd.gender().confidence());
			BooleanAttribute smile = new BooleanAttribute(fd.smile().value(), fd.smile().confidence());
			BooleanAttribute glasses = new BooleanAttribute(fd.eyeglasses().value(), fd.eyeglasses().confidence());
			BooleanAttribute sunGlasses = new BooleanAttribute(fd.sunglasses().value(), fd.sunglasses().confidence());
			BoundingBox boundingBox = BoundingBox.builder()
					.top(fd.boundingBox().top())
					.left(fd.boundingBox().left())
					.width(fd.boundingBox().width())
					.height(fd.boundingBox().height())
					.build();
			BooleanAttribute eyesOpen = new BooleanAttribute(fd.eyesOpen().value(), fd.eyesOpen().confidence());
			return FacesInfo.Face.builder()
					.ageRange(ageRange)
					.gender(gender)
					.smile(smile)
					.glasses(glasses)
					.sunGlasses(sunGlasses)
					.eyesOpen(eyesOpen)
					.boudingBox(boundingBox)
					.confidence(fd.confidence())
					.build();
		}).collect(Collectors.toList());
		

		return FacesInfo.builder().faces(faces).build();
	}

	private List<String> texts() {
		return googleCloudVision.getTexts();
	}

	private List<MatchingWebsite> webDetect() {
		return googleCloudVision.getWebDetect();
	}

	private List<NamedAttribute> celebrities() throws InterruptedException, ExecutionException {
		return awsRekognition.getCelebrities();
	}
}
