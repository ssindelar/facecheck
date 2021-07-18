package de.facecheck.resource;

import java.util.List;
import java.util.stream.Collectors;

import de.facecheck.model.MatchingWebsite;
import de.facecheck.model.Recommendation;
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

	public WebDetectRest(List<MatchingWebsite> websites) {
		this.websites = websites.stream().map(MatchingWebsiteRest::new).collect(Collectors.toList());
		if (websites.isEmpty()) {
			foundOnWeb = false;
			recommendation = Recommendation.ALLOW;
		} else {
			foundOnWeb = true;
			recommendation = Recommendation.CHECK;
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	public static class MatchingWebsiteRest {
		String websiteURL;
		String pageTitle;
		String imageURL;

		public MatchingWebsiteRest(MatchingWebsite website) {
			super();
			this.websiteURL = website.getWebsiteURL();
			this.pageTitle = website.getPageTitle();
			this.imageURL = website.getImageURL();
		}
	}
}