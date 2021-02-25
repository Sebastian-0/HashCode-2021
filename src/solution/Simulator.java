package solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Simulator {
	public int runIterations(List<Intersection> intersections, Map<String, Street> nameToStreet,
							 List<Street> streets, List<Car> cars, int duration, int bonus) {
		int best = 0;
		int n = 1000;
		for (int iter = 0; iter < n; iter++) {
			Stats stats = run(intersections, nameToStreet, streets, cars, duration, bonus);

			if (stats.score > best) {
				System.out.println("BEST Score: " + stats.score);
				best = stats.score;
			} else {
				System.out.printf("Score: %d (%.1f%%)%n", stats.score, 100f*stats.score/best);
			}

			for (Intersection inter : intersections) {
				long totalC = 0;
				for (String s : inter.in) {
					totalC += stats.congestion.get(s);
				}
				long totalT = 0;
				for (StreetAndDuration sad : inter.schedule) {
					totalT += sad.onDuration;
				}

				for (StreetAndDuration sad : inter.schedule) {
					double timeFrac = sad.onDuration / (double) totalT;
					double conFrac = stats.congestion.get(sad.street) / (double) totalC;

					if (timeFrac < conFrac) {
						sad.onDuration += 1;
						sad.onDuration *= 0.8;
					}
				}

				long totalT2 = 0;
				for (StreetAndDuration sad : inter.schedule) {
					totalT2 += sad.onDuration;
				}

				if (totalT2 > duration) {
					double factor = duration / (double) totalT2;

					for (StreetAndDuration sad : inter.schedule) {
						sad.onDuration *= factor;
					}
				}
			}
		}
		return best;
	}


	public Stats run(List<Intersection> intersections, Map<String, Street> nameToStreet,
					 List<Street> streets, List<Car> cars, int duration, int bonus) {
		List<Intersection> newIntersections = new ArrayList<>();
		for (Intersection intersection : intersections) {
			newIntersections.add(intersection.copy());
		}

		Map<String, Street> newNameToStreet = new HashMap<>();
		List<Street> newStreets = new ArrayList<>();
		for (Street street : streets) {
			Street newStreet = street.copy();
			newStreets.add(newStreet);
			newNameToStreet.put(street.name, newStreet);
		}

		List<Car> newCars = new ArrayList<>();
		for (Car car : cars) {
			newCars.add(car.copy());
		}

		return doRun(newIntersections, newNameToStreet, newStreets, newCars, duration, bonus);
	}

	private Stats doRun(List<Intersection> intersections, Map<String, Street> nameToStreet,
						List<Street> streets, List<Car> cars, int duration, int bonus) {
		Stats stats = new Stats();

		// Setup
		for (Car car : cars) {
			Street target = nameToStreet.get(car.streets.get(0));
			target.cars.add(car);
		}

		List<Car> carsToMove = new ArrayList<>();

		// Run simulation
		for (int time = 0; time < duration + 1; time++) {
			for (Street street : streets) {

				int congest = 0;
				Iterator<Car> itr = street.cars.iterator();
				while (itr.hasNext()) {
					Car car = itr.next();
					if (car.distLeft == 0) {
						congest++;
					}

					if (car.distLeft > 0) {
						car.distLeft--;
					}

					if (car.distLeft == 0 && car.streetIdx == car.streets.size() - 1) {
						itr.remove();
						stats.score += bonus + (duration - time);
					}
				}

				stats.congestion.put(street.name, congest + stats.congestion.getOrDefault(street.name, 0L));

				Intersection intersection = intersections.get(street.end);
				boolean isGreen = intersection.isGreen(street.name, time);
				if (isGreen && !street.cars.isEmpty() && street.cars.peek().distLeft == 0) {
					Car first = street.cars.pop();
					carsToMove.add(first);
				}
			}

			for (Car car : carsToMove) {
				car.streetIdx++;
				Street newStreet = nameToStreet.get(car.streets.get(car.streetIdx));
				newStreet.cars.offer(car);
				car.distLeft = newStreet.length;
			}
			carsToMove.clear();
		}

		return stats;
	}


	public static class Stats {
		Map<String, Long> congestion = new HashMap<>();
		int score;
	}


	public static class StreetAndDuration {
		public String street;
		public double onDuration;

		public StreetAndDuration(String street, double onDuration) {
			this.street = street;
			this.onDuration = onDuration;
		}

		public int onDuration() {
			return (int) Math.ceil(onDuration);
		}

		public StreetAndDuration copy() {
			return new StreetAndDuration(street, onDuration);
		}
	}


	public static class Intersection {
		public int id;
		public List<String> in = new ArrayList<>();
		public List<String> out = new ArrayList<>();

		public List<StreetAndDuration> schedule = new ArrayList<>();

		// Simulator
		int totalTime = -1;

		public Intersection(int i) {
			this.id = i;
		}

		public boolean isGreen(String inStreet, int time) {
			if (schedule.isEmpty()) {
				return false;
			}

			if (totalTime == -1) {
				totalTime = schedule.stream().mapToInt(s -> s.onDuration()).sum();
			}

			time = time % totalTime;

			for (StreetAndDuration sad : schedule) {
				time -= sad.onDuration();
				if (time < 0) {
					return sad.street.equals(inStreet);
				}
			}

			throw new IllegalStateException("Ran out of streets...");
		}

		public Intersection copy() {
			Intersection copy = new Intersection(id);
			copy.in.addAll(in);
			copy.out.addAll(out);
			for (StreetAndDuration sad : schedule) {
				copy.schedule.add(sad.copy());
			}
			return copy;
		}

		@Override
		public String toString() {
			return String.format("%d - in: %d, out: %d", id, in.size(), out.size());
		}
	}

	public static class Street {
		public int length;
		public String name;

		public int start;
		public int end;

		// For simulation
		public LinkedList<Car> cars = new LinkedList<>();

		public Street(int length, String name, int start, int end) {
			this.length = length;
			this.name = name;
			this.start = start;
			this.end = end;
		}

		public Street copy() {
			return new Street(length, name, start, end);
		}

		@Override
		public String toString() {
			return String.format("%s, L: %d, start: %d, stop: %d", name, length, start, end);
		}
	}

	public static class Car {
		public List<String> streets = new ArrayList<>();

		// For simulation
		int streetIdx;
		int distLeft;

		public Car copy() {
			Car car = new Car();
			car.streets.addAll(streets);
			return car;
		}

		@Override
		public String toString() {
			return String.format("Stops: %d", streets.size());
		}
	}
}
