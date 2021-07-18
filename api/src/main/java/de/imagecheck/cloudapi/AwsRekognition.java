package de.imagecheck.cloudapi;

import javax.inject.Singleton;

import de.imagecheck.backend.profile.CelebrityProfile;
import de.imagecheck.backend.profile.FaceProfile;
import de.imagecheck.backend.profile.LabelProfile;
import de.imagecheck.backend.profile.ModerationProfile;
import de.imagecheck.backend.profile.Profile;
import de.imagecheck.cloudapi.AwsRekognitionResponse.AwsRekognitionResponseBuilder;
import de.imagecheck.model.CloudServices;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.rekognition.RekognitionAsyncClient;
import software.amazon.awssdk.services.rekognition.model.Attribute;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.RecognizeCelebritiesRequest;

@Singleton
public class AwsRekognition {

	private final RekognitionAsyncClient asyncClinet = RekognitionAsyncClient.builder()
			.httpClient(NettyNioAsyncHttpClient.builder().build()).build();

	public AwsRekognitionResponse request(Profile profile, byte[] image) {
		Image awsImage = Image.builder().bytes(SdkBytes.fromByteArray(image)).build();

		AwsRekognitionResponseBuilder requestBuilder = AwsRekognitionResponse.builder();
		// Moderation
		ModerationProfile moderation = profile.getModeration();
		if (moderation.isActive(CloudServices.AWS_REKOGNITION)) {
			DetectModerationLabelsRequest moderationRequest = DetectModerationLabelsRequest.builder()
					.minConfidence((float) moderation.getDetectLevel())
					.image(awsImage)
					.build();
			requestBuilder.moderationResponse(asyncClinet.detectModerationLabels(moderationRequest));
		}

		// Label
		LabelProfile label = profile.getLabel();
		if (label.isActive(CloudServices.AWS_REKOGNITION)) {
			DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder()
					.image(awsImage)
					.maxLabels(label.getMaxLabel())
					.minConfidence((float) label.getDetectLevel())
					.build();
			requestBuilder.labelResponse(asyncClinet.detectLabels(labelsRequest));
		}

		// Faces
		FaceProfile face = profile.getFace();
		if (face.isActive(CloudServices.AWS_REKOGNITION)) {
			DetectFacesRequest facesRequest = DetectFacesRequest.builder()
					.image(awsImage)
					.attributes(Attribute.ALL)
					.build();
			requestBuilder.facesResponse(asyncClinet.detectFaces(facesRequest));
		}
		
		// Celebrities
		CelebrityProfile celebrity = profile.getCelebrity();
		if (celebrity.isActive(CloudServices.AWS_REKOGNITION)) {
			RecognizeCelebritiesRequest request = RecognizeCelebritiesRequest.builder().image(awsImage).build();
			requestBuilder.celebrityResponse(asyncClinet.recognizeCelebrities(request));
		}

		return requestBuilder.build();
	}
}
