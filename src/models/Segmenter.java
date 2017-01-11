package models;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;

import collections.Counter;
import io.TextReader;
import stochasticProcesses.DP;

public class Segmenter {

	private DP<String> segments;
	private List<int[]> boundaries;
	private Counter<String> atoms;
	private MersenneTwister randomGenerator;
	private Counter<String> samples;

	public static String delimiter = " ";

	public Segmenter(double concentration) {
		segments = new DP<String>(concentration);
		// use linked list for frequent insertions
		boundaries = new LinkedList<int[]>();
		atoms = new Counter<String>();
		randomGenerator = new MersenneTwister();
		samples = new Counter<String>();
	}

	public void segment(String pathToFile, int iter) throws FileNotFoundException, IOException {
		/**
		 * Segment an input file.
		 * 
		 * @param pathToFile
		 *            The path to the input file
		 */
		List<String[]> corpus = new ArrayList<String[]>();

		try (TextReader reader = new TextReader(pathToFile)) {
			String[] line;
			while ((line = reader.nextLine()) != null) {
				corpus.add(line);
			}
		}

		initialiseState(corpus);
		// TODO change number of samples later
		train(corpus, iter, 0);
		writeSegmentation(corpus, "out.txt");
	}

	/**
	 * Initialise the state of the sampler with no boundaries except the sentence boundaries.
	 * 
	 * @param corpus
	 *            The corpus from which to initialise the state
	 */
	public void initialiseState(List<String[]> corpus) {
		HashSet<String> unique = new HashSet<String>();
		segments.setSizeOfBaseSupport(0);
		
		for (String[] sent : corpus) {
			segments.addObservation(String.join(delimiter, sent));
			boundaries.add(new int[] { sent.length });
			atoms.putAll(sent, 1.0);
			for (String word : sent) {
				unique.add(word);
			}
		}
		
		segments.setSizeOfBaseSupport(unique.size());
	}

	public void train(List<String[]> corpus, int iter, int samples) {
		for (int i = 1; i <= iter; i++) {
			trainModel(corpus);
			// TODO make this two methods, one which takes samples and one which doesn't
//			if (iter % samples == 0) {
//				this.samples.putAll(segments.getCurrentObservations().toMap());
//			}
		}
	}

	public void trainModel(List<String[]> corpus) {
		int sentNum = 0;
		for (String[] sent : corpus) {
			boundaries.set(sentNum, sampleBoundaries(sent, boundaries.get(sentNum)));
			sentNum++;
		}
	}

	/**
	 * Sample the segment boundaries for an input sentence.
	 * 
	 * @param sent
	 *            The input sentence word-by-word
	 * @param boundaries
	 *            The existing boundaries for that sentence
	 */
	protected int[] sampleBoundaries(String[] sent, int[] boundaries) {

		// do nothing if the segement has length 1
		if (sent.length == 1) {
			return boundaries;
		}
		
		int[] newBoundaries = new int[sent.length];
		int prevBoundary = 0;
		int nextBoundary = boundaries[0];
		int boundaryNum = 1;
		int newBoundaryNum = 0;
		String currentSegment = String.join(delimiter, Arrays.copyOfRange(sent, 0, nextBoundary));
		double currentSegmentProb = segments.probability(currentSegment);

		for (int i = 1; i < sent.length; i++) {
			//System.out.printf("prevBoundary = %d, nextBoundary = %d, i = %d%n", prevBoundary, nextBoundary, i);
			if (i != nextBoundary) {
				String firstSegment = String.join(delimiter, Arrays.copyOfRange(sent, prevBoundary, i));
				String secondSegment = String.join(delimiter, Arrays.copyOfRange(sent, i, nextBoundary));
				double splitProb = segments.probability(firstSegment) + segments.probability(secondSegment);
				if (splitProb >= randomGenerator.nextDouble() * (splitProb + currentSegmentProb)) {
					newBoundaries[newBoundaryNum] = i;
					newBoundaryNum++;
					segments.addObservation(firstSegment);
					segments.addObservation(secondSegment);
					segments.removeObservation(currentSegment);
					currentSegment = secondSegment;
					currentSegmentProb = segments.probability(secondSegment);
					prevBoundary = i;
				}
			} else {
				nextBoundary = boundaries[boundaryNum];
				boundaryNum++;
				// TODO check whether the variable assignments work out here
				String firstSegment = currentSegment;
				String secondSegment = String.join(delimiter, Arrays.copyOfRange(sent, i, nextBoundary));
				String expandedSegment = firstSegment + delimiter + secondSegment;
				// TODO System.out.printf("First segment = %s, Second segment = %s, index = %d, boundary = %d, sent = %s%n", firstSegment, secondSegment, i, nextBoundary, Arrays.toString(sent));
				// in this case, currentSegmentProb == firstSegmentProb
				double secondSegmentProb = segments.probability(secondSegment);
				double splitProb = currentSegmentProb + secondSegmentProb;
				double expandProb = segments.probability(expandedSegment);
				if (expandProb >= randomGenerator.nextDouble() * (splitProb + expandProb)) {
					segments.removeObservation(firstSegment);
					segments.removeObservation(secondSegment);
					segments.addObservation(expandedSegment);
					currentSegment = expandedSegment;
					currentSegmentProb = expandProb;
				} else {
					prevBoundary = i;
					newBoundaries[newBoundaryNum] = i;
					currentSegment = secondSegment;
					currentSegmentProb = secondSegmentProb;
					newBoundaryNum++;
				}
			}
		}
		// make sure sentence end is always included
		newBoundaries[newBoundaryNum] = sent.length;
		return newBoundaries;
	}

	public void writeSegmentation(List<String[]> corpus, String pathToFile) throws IOException {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile))) {
			int sentNum = 0;
			for (String[] sent : corpus) {
				String output = "";
				int wordPos = 0;
				for (int boundary : this.boundaries.get(sentNum)) {
					if (boundary == 0) break;
					else {
						while (wordPos < boundary) {
							output += sent[wordPos];
							wordPos++;
						}
						output += Segmenter.delimiter;
					}
				}
				writer.write(output);
				writer.newLine();
				
				sentNum++;
			}
		}
	}
}
