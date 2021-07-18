package de.imagecheck.cloudapi;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import de.imagecheck.backend.analysis.AnalysisBoundingBox;
import de.imagecheck.backend.analysis.AnalysisCelebrity;
import de.imagecheck.backend.analysis.AnalysisFace;
import de.imagecheck.backend.analysis.AnalysisFace.AgeRange;
import de.imagecheck.backend.analysis.AnalysisFace.Pose;
import de.imagecheck.backend.analysis.AnalysisFace.Quality;
import de.imagecheck.backend.analysis.AnalysisLabel;
import de.imagecheck.backend.analysis.AnalysisLabel.LabelInstance;
import de.imagecheck.backend.analysis.AnalysisModerationIssue;
import de.imagecheck.model.BooleanAttribute;
import de.imagecheck.model.NamedAttribute;
import lombok.Builder;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.Celebrity;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;
import software.amazon.awssdk.services.rekognition.model.Instance;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.Parent;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesResponse;

@Builder
public class AwsRekognitionResponse {

	private CompletableFuture<DetectModerationLabelsResponse> moderationResponse;
	private CompletableFuture<DetectLabelsResponse> labelResponse;
	private CompletableFuture<DetectFacesResponse> facesResponse;
	private CompletableFuture<RecognizeCelebritiesResponse> celebrityResponse;

	/**
	 * Problematischer Inhalt
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public List<AnalysisModerationIssue> getModerationLabels()
			throws InterruptedException, ExecutionException, TimeoutException {
		if (moderationResponse == null) {
			return Collections.emptyList();
		}
		List<ModerationLabel> labels = moderationResponse.get(CloudConstants.TIME, CloudConstants.TIMEUNIT)
				.moderationLabels();
		return labels.stream()
				.map(l -> new AnalysisModerationIssue(l.name(), l.confidence(), Strings.emptyToNull(l.parentName())))
				.collect(Collectors.toList());
	}

	/**
	 * Labels
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public List<AnalysisLabel> getLabels() throws InterruptedException, ExecutionException, TimeoutException {
		if (labelResponse == null) {
			return Collections.emptyList();
		}
		DetectLabelsResponse response = labelResponse.get(CloudConstants.TIME, CloudConstants.TIMEUNIT);
		return response.labels().stream().map(this::mapLabel).collect(Collectors.toList());
	}

	private AnalysisLabel mapLabel(Label label) {
		List<String> parents = label.parents().stream().map(Parent::name).collect(Collectors.toList());
		List<LabelInstance> instances = label.instances()
				.stream()
				.map(this::mapLabelInstance)
				.collect(Collectors.toList());
		return AnalysisLabel.builder()
				.name(label.name().toLowerCase())
				.confidence(label.confidence())
				.instances(instances)
				.parents(parents)
				.build();
	}

	private LabelInstance mapLabelInstance(Instance instance) {
		BoundingBox boundingBox = instance.boundingBox();
		return new LabelInstance(instance.confidence(), mapBoundingBox(boundingBox));
	}


	public List<AnalysisFace> getFaces() throws InterruptedException, ExecutionException, TimeoutException {
		DetectFacesResponse response = facesResponse.get(CloudConstants.TIME, CloudConstants.TIMEUNIT);
		List<FaceDetail> faces = response.faceDetails();
		return faces.stream().map(this::mapFace).collect(Collectors.toList());
	}

	private AnalysisFace mapFace(FaceDetail face) {
		AgeRange ageRange = AgeRange.builder().low(face.ageRange().low()).high(face.ageRange().high()).build();
		BoundingBox boundingBox = face.boundingBox();
		BooleanAttribute smile = new BooleanAttribute(face.smile().value(), face.smile().confidence());
		BooleanAttribute glasses = new BooleanAttribute(face.eyeglasses().value(), face.eyeglasses().confidence());
		BooleanAttribute sunGlasses = new BooleanAttribute(face.sunglasses().value(), face.sunglasses().confidence());
		NamedAttribute gender = new NamedAttribute(face.gender().valueAsString(), face.gender().confidence());
		BooleanAttribute beard = new BooleanAttribute(face.beard().value(), face.beard().confidence());
		BooleanAttribute mustache = new BooleanAttribute(face.mustache().value(), face.mustache().confidence());
		BooleanAttribute eyesOpen = new BooleanAttribute(face.eyesOpen().value(), face.eyesOpen().confidence());
		BooleanAttribute mouthOpen = new BooleanAttribute(face.mouthOpen().value(), face.mouthOpen().confidence());
		List<NamedAttribute> emotions = face.emotions()
				.stream()
				.map(e -> new NamedAttribute(e.typeAsString(), e.confidence()))
				.collect(Collectors.toList());
		Pose pose = Pose.builder().roll(face.pose().roll()).yaw(face.pose().yaw()).pitch(face.pose().pitch()).build();
		Quality quality = Quality.builder()
				.brightness(face.quality().brightness())
				.sharpness(face.quality().sharpness())
				.build();
		return AnalysisFace.builder()
				.ageRange(ageRange)
				.boundingBox(mapBoundingBox(boundingBox))
				.smile(smile)
				.glasses(glasses)
				.sunGlasses(sunGlasses)
				.gender(gender)
				.beard(beard)
				.mustache(mustache)
				.eyesOpen(eyesOpen)
				.mouthOpen(mouthOpen)
				.emotions(emotions)
				.pose(pose)
				.quality(quality)
				.confidence(face.confidence())
				.build();
	}

	private AnalysisBoundingBox mapBoundingBox(BoundingBox boundingBox) {
		return AnalysisBoundingBox.builder()
				.top(boundingBox.top())
				.left(boundingBox.left())
				.width(boundingBox.width())
				.height(boundingBox.height())
				.build();
	}

	public List<AnalysisCelebrity> getCelebrities() throws InterruptedException, ExecutionException, TimeoutException {
		RecognizeCelebritiesResponse response = celebrityResponse.get(CloudConstants.TIME, CloudConstants.TIMEUNIT);
		return response.celebrityFaces().stream().map(this::mapCelebrity).collect(Collectors.toList());
	}

	private AnalysisCelebrity mapCelebrity(Celebrity celebrity) {
		return AnalysisCelebrity.builder()
				.name(celebrity.name())
				.awsId(celebrity.id())
				.boundingBox(mapBoundingBox(celebrity.face().boundingBox()))
				.urls(celebrity.urls())
				.confidence(celebrity.matchConfidence())
				.build();
	}
}
