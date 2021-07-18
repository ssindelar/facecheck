package de.facecheck.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FacesInfo {

	@Builder.Default
	List<Face> faces = new ArrayList<>();

	@Value
	@Builder
	public static class Face {

		AgeRange ageRange;

		BooleanAttribute smile;

		NamedAttribute gender;

		BooleanAttribute glasses;

		BooleanAttribute sunGlasses;

		BooleanAttribute eyesOpen;

		BoundingBox boudingBox;

		double confidence;

		@Value
		public static class AgeRange {
			Integer low;
			Integer high;
		}

		@Value
		@Builder
		public static class BoundingBox {
			double width;
			double height;

			double left;
			double top;
		}
	}

}
