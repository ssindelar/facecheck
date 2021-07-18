package de.imagecheck.resources.v1.model;

import java.util.List;

import de.imagecheck.model.Recommendation;
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
public class WebDetectRest {

	Recommendation recommendation;

	List<WebDetectRest.MatchingWebsiteRest> websites;

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MatchingWebsiteRest {
		String websiteURL;
		String pageTitle;
		String imageURL;

	}
}