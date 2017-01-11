package models;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;

import collections.Counter;
import stochasticProcesses.DP;
import stochasticProcesses.PredictiveDirichletDistribution;
import utilities.Segment;

public class Segmenter<A> {

	private DP<Segment<A>> segments;
	private List<int[]> boundaries;
	private MersenneTwister randomGenerator;
	private Counter<Segment<A>> samples;

	public String delimiter = " ";

	public Segmenter(double concentration, String delimiter) {
		segments = new DP<Segment<A>>(concentration);
		// use linked list for frequent insertions
		boundaries = new LinkedList<int[]>();
		randomGenerator = new MersenneTwister();
		samples = new Counter<Segment<A>>();
		this.delimiter = delimiter;
	}

	public Segmenter(double concentration) {
		this(concentration, " ");
	}

	public void segment(List<A[]> corpus, int iter) throws FileNotFoundException, IOException {
		/**
		 * Segment an input file.
		 * 
		 * @param pathToFile
		 *            The path to the input file
		 */

		initialiseState(corpus);
		// TODO change number of samples later
		train(corpus, iter);
		writeSegmentation(corpus, "out.txt");
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Initialise the state of the sampler with no boundaries except the sentence boundaries.
	 * 
	 * @param corpus
	 *            The corpus from which to initialise the state
	 */
	public void initialiseState(List<A[]> corpus) {
		HashSet<A> unique = new HashSet<A>();

		corpus.forEach(sent -> {
			for (A atom : sent) {
				unique.add(atom);
			}
		});

		this.segments.setBaseDistribution(new PredictiveDirichletDistribution<Segment<A>>(1, unique.size()));

		corpus.forEach(sent -> {
			segments.addObservation(new Segment<A>(sent));
			boundaries.add(new int[] { sent.length });
		});
	}

	/**
	 * Train the model without taking samples
	 * 
	 * @param corpus
	 *            The corpus to segment
	 * @param iter
	 *            The number of iterations for the Gibbs Sampler
	 */
	public void train(List<A[]> corpus, int iter) {
		for (int i = 1; i <= iter; i++) {
			trainModel(corpus);
		}
	}

	/**
	 * Train the model and take samples in the process
	 * 
	 * @param corpus
	 *            The corpus to segment
	 * @param iter
	 *            The number of iterations for the Gibbs Sampler
	 * @param samples
	 *            The number of samples to be taken
	 */
	public void train(List<A[]> corpus, int iter, int samples) {
		for (int i = 1; i <= iter; i++) {
			trainModel(corpus);
			if (iter % samples == 0) {
				this.samples.putAll(segments.getCurrentObservations().toMap());
			}
		}
	}

	/**
	 * Run one sampling iteration on the corpus
	 * 
	 * @param corpus
	 *            The corpus to segment
	 */
	public void trainModel(List<A[]> corpus) {
		int sentNum = 0;
		for (A[] sent : corpus) {
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
	protected int[] sampleBoundaries(A[] sent, int[] boundaries) {

		// do nothing if the segement has length 1
		if (sent.length == 1) {
			return boundaries;
		}

		int[] newBoundaries = new int[sent.length];
		int prevBoundary = 0;
		int nextBoundary = boundaries[0];
		int boundaryNum = 0;
		int newBoundaryNum = 0;
		Segment<A> currentSegment = new Segment<A>(Arrays.copyOfRange(sent, 0, nextBoundary));
		double currentSegmentProb = segments.probability(currentSegment);

		for (int i = 1; i < sent.length; i++) {
			// System.out.printf("prevBoundary = %d, nextBoundary = %d, i = %d%n", prevBoundary, nextBoundary, i);
			if (i != nextBoundary) {
				Segment<A> firstSegment = new Segment<A>(Arrays.copyOfRange(sent, prevBoundary, i));
				Segment<A> secondSegment = new Segment<A>(Arrays.copyOfRange(sent, i, nextBoundary));
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
				boundaryNum++;
				nextBoundary = boundaries[boundaryNum];
				// TODO check whether the variable assignments work out here
				Segment<A> firstSegment = currentSegment;
				Segment<A> secondSegment = new Segment<A>(Arrays.copyOfRange(sent, i, nextBoundary));
				Segment<A> expandedSegment = firstSegment.compose(secondSegment);
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
					newBoundaryNum++;
				}
			}
		}
		// make sure sentence end is always included
		newBoundaries[newBoundaryNum] = sent.length;
		return newBoundaries;
	}

	/**
	 * Write the segmentation resulting from the current state to disk
	 * 
	 * @param corpus
	 *            The corpus to segment
	 * @param pathToFile
	 *            The path to which the output shall be written
	 * @throws IOException
	 *             if the output path is not accessible
	 */
	public void writeSegmentation(List<A[]> corpus, String pathToFile) throws IOException {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToFile))) {
			int sentNum = 0;
			for (A[] sent : corpus) {
				String output = "";
				int prevBoundary = 0;

				for (int boundary : this.boundaries.get(sentNum)) {
					if (boundary == 0)
						break;
					else {
						while (prevBoundary < boundary) {
							output += sent[prevBoundary];
							prevBoundary++;
						}
						output += this.delimiter;
					}
				}
				writer.write(output);
				writer.newLine();

				sentNum++;
			}
		}
	}
}
