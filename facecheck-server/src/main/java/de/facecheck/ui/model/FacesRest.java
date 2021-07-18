package de.facecheck.ui.model;

import de.facecheck.ui.model.FaceCheckResponse.AgeRangeRest;
import de.facecheck.ui.model.FaceCheckResponse.BooleanAttributeRest;
import de.facecheck.ui.model.FaceCheckResponse.NamedAttributeRest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class FacesRest {

	Recommendation recommandation;
	int faceCount;
	FacesRest.FaceRest mainFace;


	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class FaceRest {
		AgeRangeRest ageRange;

		BooleanAttributeRest smile;

		NamedAttributeRest gender;

		BooleanAttributeRest glasses;

		BooleanAttributeRest sunGlasses;

		BooleanAttributeRest eyesOpen;

		double confidence;
	}
}