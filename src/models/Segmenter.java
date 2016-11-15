package models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;

import collections.Counter;
import io.TextReader;
import stochasticProcesses.DP;

public class Segmenter {

	private DP<String> segments;
	private List<Integer[]> boundaries;
	private Counter<String> atoms;
	private MersenneTwister randomGenerator;
	private Counter<String> samples;
	
	public static String delimiter = " ";
	
	public Segmenter(double concentration) {
		segments = new DP<String>(concentration);
		boundaries = new ArrayList<Integer[]>();
		atoms = new Counter<String>();
		randomGenerator = new MersenneTwister();
		samples = new Counter<String>();
	}
	
	public void segment(String pathToFile) throws FileNotFoundException, IOException {
		List<String[]> corpus = new ArrayList<String[]>();
		
		try (TextReader reader = new TextReader(pathToFile)) {
			String[] line;
			while ((line = reader.nextLine()) != null) {
				corpus.add(line);
			}
		}
		
		initialiseState(corpus);
		
		trainModel(corpus);
	}
	
	public void initialiseState(List<String[]> corpus) {
		for (String[] sent : corpus) {
			segments.addObservation(String.join(delimiter, sent));
			boundaries.add(new Integer[]{sent.length});
			atoms.putAll(sent, 1.0);
		}
	}
	
	public void train(List<String[]> corpus, int iter, int samples) {
		for (int i = 1; i <= iter; i++) {
			trainModel(corpus);
			if (iter % samples == 0) {
				this.samples.putAll(segments.getCurrentObservations().toMap());
			}
		}
	}
	
	public void trainModel(List<String[]> corpus) {
		int sentNum = 0;
		for (String[] sent : corpus) {
			sampleBoundaries(sent, boundaries.get(sentNum));
			sentNum++;
		}
	}
	
	protected Integer[] sampleBoundaries(String[] sent, Integer[] boundaries) {
		List<Integer> newBoundaries = new ArrayList<Integer>();
		int prevBoundary = 0;
		int nextBoundary = boundaries[0];
		int boundaryNum = 1;
		String currentSegment = String.join(delimiter, Arrays.copyOfRange(sent, 0, nextBoundary));
		double currentSegmentProb = segments.probability(currentSegment);
		
		for (int i = 1; i < sent.length; i++) {
			if (i != nextBoundary) {
				String firstSegment = String.join(delimiter, Arrays.copyOfRange(sent, prevBoundary, i+1));
				String secondSegment = String.join(delimiter, Arrays.copyOfRange(sent, i, nextBoundary+1));
				double splitProb = segments.probability(firstSegment) + segments.probability(secondSegment);
				if (splitProb >= randomGenerator.nextDouble()*(splitProb + currentSegmentProb)) {
					newBoundaries.add(i);
					segments.addObservation(firstSegment);
					segments.addObservation(secondSegment);
					segments.removeObservation(currentSegment);
					currentSegment = secondSegment;
					currentSegmentProb = segments.probability(secondSegment);
				}
			} else {
				nextBoundary = boundaries[boundaryNum];
				boundaryNum++;
				// TODO check whether the variable assignments work out here
				String firstSegment = currentSegment;
				currentSegment = String.join(delimiter, Arrays.copyOfRange(sent, i, nextBoundary+1));
				String secondSegment = currentSegment;
				String expandedSegment = firstSegment + delimiter + secondSegment;
				double splitProb = segments.probability(firstSegment) + segments.probability(secondSegment);
				double expandProb = segments.probability(expandedSegment);
				if (expandProb >= randomGenerator.nextDouble()*(splitProb + expandProb)) {
					segments.removeObservation(firstSegment);
					segments.removeObservation(secondSegment);
					segments.addObservation(expandedSegment);
				} else {
					prevBoundary = i;
					newBoundaries.add(i);
				}
			}
		}
		return newBoundaries.toArray(new Integer[1]);
	}
	
}
