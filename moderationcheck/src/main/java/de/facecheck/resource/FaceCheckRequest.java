package de.facecheck.resource;

import lombok.Data;

@Data
public class FaceCheckRequest {

	private String profileId;
	private String image;
	private String link;

}
