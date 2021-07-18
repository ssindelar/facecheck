package de.facecheck.resource;

import java.util.Optional;

import de.facecheck.model.FacesInfo;
import de.facecheck.model.FacesInfo.Face;
import de.facecheck.model.Profile;
import de.facecheck.model.Recommendation;
import de.facecheck.resource.FaceCheckResponse.AgeRangeRest;
import de.facecheck.resource.FaceCheckResponse.BooleanAttributeRest;
import de.facecheck.resource.FaceCheckResponse.NamedAttributeRest;
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

	public FacesRest(FacesInfo faces, Profile profile) {

		faceCount = faces.getFaces().size();

		Optional<Face> optLargestFace = faces.getFaces().stream().reduce((f1, f2) -> {
			double size1 = f1.getBoudingBox().getWidth() * f1.getBoudingBox().getHeight();
			double size2 = f2.getBoudingBox().getWidth() * f2.getBoudingBox().getHeight();
			if (size1 > size2) {
				return f1;
			} else {
				return f2;
			}
		});
		if (optLargestFace.isPresent()) {
			mainFace = new FaceRest(optLargestFace.get());
		} else {
			mainFace = null;
		}

		if (profile.getAllowedFaceCount() == null || faceCount == profile.getAllowedFaceCount()) {
			recommandation = Recommendation.ALLOW;
		} else {
			recommandation = Recommendation.CHECK;
		}
	}


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

		public FaceRest(FacesInfo.Face face) {
			ageRange = new AgeRangeRest(face.getAgeRange());
			smile = new BooleanAttributeRest(face.getSmile());
			gender = new NamedAttributeRest(face.getGender());
			glasses = new BooleanAttributeRest(face.getGlasses());
			sunGlasses = new BooleanAttributeRest(face.getSunGlasses());
			eyesOpen = new BooleanAttributeRest(face.getEyesOpen());
			confidence = face.getConfidence();
		}
	}
}