package de.imagecheck.resources.v1.model;

import java.util.List;

import de.imagecheck.model.Recommendation;
import de.imagecheck.resources.v1.model.ImageCheckResponse.AgeRangeRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.BooleanAttributeRest;
import de.imagecheck.resources.v1.model.ImageCheckResponse.NamedAttributeRest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacesRest {

	Recommendation recommandation;
	int faceCount;
	FacesRest.FaceRest mainFace;


	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class FaceRest {
		private AgeRangeRest ageRange;
		private BooleanAttributeRest smile;
		private BooleanAttributeRest glasses;
		private BooleanAttributeRest sunGlasses;
		private NamedAttributeRest gender;
		private BooleanAttributeRest beard;
		private BooleanAttributeRest mustache;
		private BooleanAttributeRest eyesOpen;
		private BooleanAttributeRest mouthOpen;
		private List<NamedAttributeRest> emotions;
		double confidence;

	}
}