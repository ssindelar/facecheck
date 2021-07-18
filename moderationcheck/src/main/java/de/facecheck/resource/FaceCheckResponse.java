package de.facecheck.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import de.facecheck.model.BooleanAttribute;
import de.facecheck.model.FacesInfo;
import de.facecheck.model.FacesInfo.Face.AgeRange;
import de.facecheck.model.ImageLabel;
import de.facecheck.model.MatchingWebsite;
import de.facecheck.model.ModerationIssue;
import de.facecheck.model.NamedAttribute;
import de.facecheck.model.Profile;
import de.facecheck.model.Recommendation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class FaceCheckResponse {

	String imageId;

	Recommendation recommendation;

	ModerationRest moderation;

	LabelsRest labels;

	FacesRest face;

	TextRest texts;

	WebDetectRest webDetect;

	CelebritiesRest celebrities;

	Boolean animalVisible;

	Boolean goodProfilePicture;

	@Builder
	public FaceCheckResponse(@NonNull String imageId, LabelsRest labels,
			FacesInfo faces, ModerationRest moderation, List<String> texts, List<MatchingWebsite> websites,
			List<NamedAttribute> celebrities, @NonNull Profile profile) {
		super();
		this.imageId = imageId;
		this.moderation = moderation;
		this.labels = labels;
		this.face = new FacesRest(faces, profile);
		this.webDetect = new WebDetectRest(websites);
		this.texts = new TextRest(texts);
		this.celebrities = new CelebritiesRest(celebrities);

		// Gesamt Recommendation berechnen
		Set<Recommendation> recommendations = Sets.newHashSet(moderation.getRecommendation(),
				webDetect.getRecommendation(), this.texts.getRecommendation(), face.getRecommandation(),
				this.celebrities.getRecommendation());
		if (recommendations.contains(Recommendation.DENY)) {
			recommendation = Recommendation.DENY;
		} else if (recommendations.contains(Recommendation.CHECK)) {
			recommendation = Recommendation.CHECK;
		} else {
			recommendation = Recommendation.ALLOW;
		}

		// Misc
		this.animalVisible = labels.getLabels().stream().anyMatch(i -> "Animal".equals(i.getName()));
		this.goodProfilePicture = face.getFaceCount() == 1
				&& face.getMainFace().getSmile().isValue()
				&& !face.getMainFace().getSunGlasses().isValue()
				&& face.getMainFace().getEyesOpen().isValue();

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ModerationRest {


		Recommendation recommendation;
		double confidence;
		List<ModerationIssueRest> issues;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class ModerationIssueRest {

		String name;
		double confidence;

		public ModerationIssueRest(ModerationIssue issue) {
			name = issue.getName();
			confidence = issue.getConfidence();
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class NamedAttributeRest {
		String name;
		double confidence;

		public NamedAttributeRest(ImageLabel label) {
			name = label.getName();
			confidence = label.getConfidence();
		}

		public NamedAttributeRest(NamedAttribute attribute) {
			name = attribute.getName();
			confidence = attribute.getConfidence();
		}
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class BooleanAttributeRest {
		boolean value;
		double confidence;

		public BooleanAttributeRest(BooleanAttribute attribute) {
			this.value = attribute.isValue();
			this.confidence = attribute.getConfidence();
		}
	}


	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class AgeRangeRest {

		Integer low;
		Integer high;

		public AgeRangeRest(AgeRange ageRange) {
			low = ageRange.getLow();
			high = ageRange.getHigh();
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class TextRest {
		Recommendation recommendation;

		List<String> texts;

		public TextRest(List<String> texts) {
			this.texts = texts;
			if(texts.isEmpty()) {
				recommendation = Recommendation.ALLOW;
			} else {
				recommendation = Recommendation.CHECK;
			}
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class CelebritiesRest {
		Recommendation recommendation;

		double confidence;

		List<NamedAttributeRest> celebreties;

		public CelebritiesRest(List<NamedAttribute> celebreties) {
			this.celebreties = celebreties.stream().map(NamedAttributeRest::new).collect(Collectors.toList());
			this.confidence = celebreties.stream().mapToDouble(NamedAttribute::getConfidence).max().orElse(0);
			if (confidence > 90) {
				recommendation = Recommendation.DENY;
			} else if (confidence > 30) {
				recommendation = Recommendation.CHECK;
			} else {
				recommendation = Recommendation.ALLOW;
			}
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class LabelsRest {
		Recommendation recommendation;

		List<NamedAttributeRest> criticalLabels;

		List<NamedAttributeRest> labels;


	}
}
