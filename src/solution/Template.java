package solution;

import solution.Simulator.Car;
import solution.Simulator.Intersection;
import solution.Simulator.Stats;
import solution.Simulator.Street;
import solution.Simulator.StreetAndDuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Template {
	public void solve(File input) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(input));

		int[] tokens = ints(in);

		int duration = tokens[0];
		int nIntersections = tokens[1];
		int nStreets = tokens[2];
		int nCars = tokens[3];
		int bonus = tokens[4];

		List<Intersection> intersections = new ArrayList<>();
		for (int i = 0; i < nIntersections; i++) {
			intersections.add(new Intersection(i));
		}

		Map<String, Street> nameToStreet = new HashMap<>();
		List<Street> streets = new ArrayList<>();
		for (int i = 0; i < nStreets; i++) {
			String[] toks = tokens(in);
			Street street = new Street(Integer.parseInt(toks[3]), toks[2], Integer.parseInt(toks[0]), Integer.parseInt(toks[1]));
			intersections.get(street.start).out.add(street.name);
			intersections.get(street.end).in.add(street.name);
			streets.add(street);
			nameToStreet.put(street.name, street);
		}

		List<Car> cars = new ArrayList<>();
		for (int i = 0; i < nCars; i++) {
			Car car = new Car();
			String[] toks = tokens(in);
			car.streets.addAll(Arrays.asList(toks).subList(1, toks.length));
			cars.add(car);
		}

		in.close();

		int opt = 0;
		for (Car car : cars) {
			int time = 0;
			List<String> strings = car.streets;
			for (int i = 1; i < strings.size(); i++) {
				String street = strings.get(i);
				time += nameToStreet.get(street).length;
			}

			if (time <= duration) {
				opt += bonus + (duration - time);
			}
		}
		System.out.println("Optimum: " + opt);

		// Solution
		Map<String, Street> unused = new HashMap<>(nameToStreet);
		for (Car car : cars) {
			for (String street : car.streets) {
				unused.remove(street);
			}
		}
		// Prune unused streets
		streets.removeAll(unused.values());
		for (Intersection intersection : intersections) {
			intersection.in.removeAll(unused.keySet());
		}

		// Solution
		for (Intersection intersection : intersections) {
			for (String s : intersection.in) {
				intersection.schedule.add(new StreetAndDuration(s, 1));
			}
		}

		Simulator simulator = new Simulator();
//		int score = simulator.runIterations(intersections, nameToStreet, streets, cars, duration, bonus);
		Stats stats = simulator.run(intersections, nameToStreet, streets, cars, duration, bonus);
		int score = stats.score;
		System.out.printf("%d (%.1f%%)%n", score, 100f*score/opt);

		String name = input.getName();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("output/" + name.charAt(0) + "-" + score + ".txt"))) {
			writer.write(generateOutput(intersections));
		}
	}

	public int[] ints(BufferedReader in) throws IOException {
		return Arrays.stream(tokens(in)).mapToInt(Integer::parseInt).toArray();
	}

	private String[] tokens(BufferedReader in) throws IOException {
		return in.readLine().split(" ");
	}

	private String generateOutput(List<Intersection> intersections) {
		List<Intersection> valid = intersections.stream().filter(i -> !i.schedule.isEmpty()).collect(Collectors.toList());
		StringBuilder out = new StringBuilder();
		out.append(valid.size()).append('\n');
		for (Intersection intersection : valid) {
			out.append(intersection.id).append('\n');
			out.append(intersection.schedule.size()).append('\n');
			for (StreetAndDuration sad : intersection.schedule) {
				out.append(sad.street).append(' ').append((int)sad.onDuration).append('\n');
			}
		}
		return out.toString();
	}

	public static void main(String[] args) throws IOException {
//		String[] inputs = { "a", "b", "c", "d", "e", "f" };
		String[] inputs = { "e" };

//		String file = "b";
		for (String input : inputs) {
			System.out.println("Running " + input);
			new Template().solve(new File(String.format("input/%s.txt", input)));
		}
	}
}
