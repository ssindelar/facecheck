package de.facecheck.ui.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class WebDetectRest {

	boolean foundOnWeb;

	Recommendation recommendation;

	List<WebDetectRest.MatchingWebsiteRest> websites;

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class MatchingWebsiteRest {
		String websiteURL;
		String pageTitle;
		String imageURL;

	}
}