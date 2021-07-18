package de.facecheck.ui.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class NamedAttributeRest {
		String name;
		double confidence;

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class BooleanAttributeRest {
		boolean value;
		double confidence;

	}


	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class AgeRangeRest {

		Integer low;
		Integer high;

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class TextRest {
		Recommendation recommendation;

		List<String> texts;

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class CelebritiesRest {
		Recommendation recommendation;

		double confidence;

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
