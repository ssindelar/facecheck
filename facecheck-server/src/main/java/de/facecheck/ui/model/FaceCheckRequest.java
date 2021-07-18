package de.facecheck.ui.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FaceCheckRequest {

	private String profileId;
	private String image;
	private String link;

}
