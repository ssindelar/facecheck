package de.imagecheck.backend.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Profile {

	private String id;

	private ModerationProfile moderation;

	private LabelProfile label;

	private FaceProfile face;

	private CelebrityProfile celebrity;

	private TextProfile text;

	private WebProfile web;

}
