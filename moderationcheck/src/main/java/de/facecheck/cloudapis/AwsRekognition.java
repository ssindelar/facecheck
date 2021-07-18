package de.facecheck.cloudapis;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import de.facecheck.model.ImageLabel;
import de.facecheck.model.ModerationIssue;
import de.facecheck.model.NamedAttribute;
import de.facecheck.model.Profile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.rekognition.RekognitionAsyncClient;
import software.amazon.awssdk.services.rekognition.model.Attribute;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesRequest;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesResponse;
import software.amazon.awssdk.utils.StringUtils;

public class AwsRekognition {

	private final RekognitionAsyncClient asyncClinet = RekognitionAsyncClient.builder()
			.httpClient(NettyNioAsyncHttpClient.builder().build()).build();
	private CompletableFuture<DetectModerationLabelsResponse> moderationResponse;

	private CompletableFuture<DetectLabelsResponse> labelResponse;
	private CompletableFuture<DetectFacesResponse> facesResponse;
	private CompletableFuture<RecognizeCelebritiesResponse> celebrityResponse;

	public void request(Profile profile, byte[] image) {
		Image awsImage = Image.builder().bytes(SdkBytes.fromByteArray(image)).build();

		// Moderation
		DetectModerationLabelsRequest moderationRequest = DetectModerationLabelsRequest.builder()
				.minConfidence((float) profile.getModerationCheckLevel())
				.image(awsImage)
				.build();
		moderationResponse = asyncClinet
				.detectModerationLabels(moderationRequest);

		// Label
		DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder().image(awsImage).maxLabels(20).build();
		labelResponse = asyncClinet.detectLabels(labelsRequest);

		// Faces
		DetectFacesRequest facesRequest = DetectFacesRequest.builder().image(awsImage).attributes(Attribute.ALL).build();
		facesResponse = asyncClinet.detectFaces(facesRequest);
		
		// Celebrities
		RecognizeCelebritiesRequest request = RecognizeCelebritiesRequest.builder().image(awsImage).build();
		celebrityResponse = asyncClinet.recognizeCelebrities(request);
	}

	public List<ModerationIssue> getModerationLabels() throws InterruptedException, ExecutionException {
			List<ModerationLabel> labels = moderationResponse.get().moderationLabels();
			return labels.stream().filter(l -> StringUtils.isEmpty(l.parentName()))
					.map(l -> new ModerationIssue(l.name(), l.confidence())).collect(Collectors.toList());
	}

	public List<ImageLabel> getLabels() throws InterruptedException, ExecutionException {
		DetectLabelsResponse response = labelResponse.get();
		return response.labels()
				.stream()
				.map(l -> new ImageLabel(l.name().toLowerCase(), l.confidence()))
				.collect(Collectors.toList());
	}

	public List<FaceDetail> getFaces() throws InterruptedException, ExecutionException {
		DetectFacesResponse response = facesResponse.get();
		return response.faceDetails();
	}

	public List<NamedAttribute> getCelebrities() throws InterruptedException, ExecutionException {
		RecognizeCelebritiesResponse response = celebrityResponse.get();
		return response.celebrityFaces()
				.stream()
				.map(c -> new NamedAttribute(c.name(), c.matchConfidence()))
				.collect(Collectors.toList());
	}
}
