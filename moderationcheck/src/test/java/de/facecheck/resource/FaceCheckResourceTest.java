package de.facecheck.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import de.facecheck.model.ModerationIssue;

public class FaceCheckResourceTest {

	@Test
	public void mergeAndOrderTest() throws IOException {
		// given
		FaceCheckResource ressource = new FaceCheckResource();

		ModerationIssue issue1b = new ModerationIssue("b", 5);
		ModerationIssue issue1a = new ModerationIssue("a", 5);
		ModerationIssue issue2a = new ModerationIssue("a", 10);
		
		// when
		List<ModerationIssue> merged = ressource.mergeAndOrder(Arrays.asList(issue1b, issue1a, issue2a));

		// then
		Assertions.assertThat(merged).containsExactly(issue2a, issue1b);
	}

}
