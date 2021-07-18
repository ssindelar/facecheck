package de.imagecheck.resources.v1.model;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.imagecheck.model.Recommendation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ImageCheckResponse {

	String id;

	Recommendation recommendation;

	ModerationRest moderation;

	LabelsRest labels;

	FacesRest face;

	TextRest texts;

	WebDetectRest webDetect;

	CelebritiesRest celebrities;

	// Misc
	Boolean animalVisible;

	Boolean goodProfilePicture;

	@Builder
	public ImageCheckResponse(String id, ModerationRest moderation, LabelsRest labels, FacesRest face,
			TextRest texts, WebDetectRest webDetect, CelebritiesRest celebrities) {
		super();
		this.id = id;
		this.moderation = moderation;
		this.labels = labels;
		this.face = face;
		this.texts = texts;
		this.webDetect = webDetect;
		this.celebrities = celebrities;

		// Gesamt Recommendation
		Set<Recommendation> allRecommendations = Sets.newHashSet(moderation.getRecommendation(),
				labels.getRecommendation(), face.getRecommandation(), texts.getRecommendation(),
				webDetect.getRecommendation(), celebrities.getRecommendation());
		if (allRecommendations.contains(Recommendation.DENY)) {
			recommendation = Recommendation.DENY;
		} else if (allRecommendations.contains(Recommendation.CHECK)) {
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
		Double confidence;
		List<ModerationIssueRest> issues;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ModerationIssueRest {
		String name;
		double confidence;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NamedAttributeRest {
		String name;
		double confidence;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BooleanAttributeRest {
		boolean value;
		double confidence;
	}


	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AgeRangeRest {
		Integer low;
		Integer high;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class TextRest {
		Recommendation recommendation;

		String text;
	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CelebritiesRest {
		Recommendation recommendation;

		Double confidence;

		List<NamedAttributeRest> celebreties;
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
