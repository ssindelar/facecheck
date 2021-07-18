package de.imagecheck.resources.v1.model;

import lombok.Data;

@Data
public class ImageCheckRequest {
	private String profileId;
	private String image;
	private String link;
}
