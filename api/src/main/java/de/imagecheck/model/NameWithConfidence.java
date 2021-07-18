package de.imagecheck.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface NameWithConfidence extends Comparable<NameWithConfidence> {

	String getName();

	double getConfidence();

	default int compareTo(NameWithConfidence other) {
		if (getConfidence() > other.getConfidence()) {
			return 1;
		} else if (getConfidence() == other.getConfidence()) {
			return 0;
		} else {
			return -1;
		}
	}

	static <T extends NameWithConfidence> List<T> mergeAndOrder(List<T> list) {
		Map<String, T> map = list.stream()
				.collect(Collectors.toMap(NameWithConfidence::getName, e -> e,
						(a, b) -> a.getConfidence() > b.getConfidence() ? a : b));
		ArrayList<T> filteredList = new ArrayList<>(map.values());
		Collections.sort(filteredList);
		Collections.reverse(filteredList);
		return filteredList;

	}

	@SafeVarargs
	static <T extends NameWithConfidence> List<T> mergeAndOrder(List<T>... lists) {
		Map<String, T> map = Arrays.stream(lists)
				.flatMap(List::stream)
				.collect(Collectors.toMap(NameWithConfidence::getName, e -> e,
						(a, b) -> a.getConfidence() > b.getConfidence() ? a : b));
		ArrayList<T> filteredList = new ArrayList<>(map.values());
		Collections.sort(filteredList);
		Collections.reverse(filteredList);
		return filteredList;

	}

	static <T extends NameWithConfidence> List<T> filter(List<T> list, int minConfidence) {
		return list.stream().filter(i -> i.getConfidence() >= minConfidence).collect(Collectors.toList());
	}

	static <T extends NameWithConfidence> Predicate<T> filter(int minConfidence) {
		return i -> i.getConfidence() >= minConfidence;
	}

}
