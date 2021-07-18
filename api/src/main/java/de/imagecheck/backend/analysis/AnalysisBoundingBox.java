package de.imagecheck.backend.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisBoundingBox {

	private double top;
	private double left;
	private double width;
	private double height;

	public double getSize() {
		return width * height;
	}

}
