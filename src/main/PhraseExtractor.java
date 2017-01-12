package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;

import collections.Counter;
import io.TextReader;
import mainUtils.InputChecker;
import models.Segmenter;

public class PhraseExtractor {

	public static void main(String[] args) {

		String outFile = "out.txt";
		String usage = "java -jar PhraseExtractor.jar ";
		JSAP commandLineParser = new JSAP();
		List<String> possibleSegments = Arrays.asList("string", "char");

		try {
			FlaggedOption atoms = new FlaggedOption("atoms").setStringParser(JSAP.STRING_PARSER).setDefault("String").setLongFlag("atoms")
					.setShortFlag('s');
			atoms.setHelp("Determine the atoms of the segments. Needs to be one of " + possibleSegments + ". If the atoms are "
					+ "strings, the systems segments sentences into phrases, if the atoms are chars it segments into words.");
			commandLineParser.registerParameter(atoms);

			FlaggedOption concentration = new FlaggedOption("concentration").setStringParser(JSAP.DOUBLE_PARSER).setDefault("1").setLongFlag("concentration").setShortFlag('c');
			concentration.setHelp("Set the initial concentration parameter for the DP.");
			commandLineParser.registerParameter(concentration);
			
			UnflaggedOption inputFile = new UnflaggedOption("inputFile").setStringParser(JSAP.STRING_PARSER).setRequired(true)
					.setGreedy(true);
			inputFile.setHelp("The input file from which to extract the phrases.");
			commandLineParser.registerParameter(inputFile);
		} catch (JSAPException e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}

		JSAPResult commandLine = commandLineParser.parse(args);
		InputChecker.CheckCommandLineInput(commandLine, commandLineParser, usage);

		String inputFile = commandLine.getString("inputFile");
		String atoms = commandLine.getString("atoms").toLowerCase();
		double concentration = commandLine.getDouble("concentration");

		List<String[]> corpus = new ArrayList<String[]>();
		if (atoms.equals("string")) {

			try (TextReader reader = new TextReader(inputFile)) {
				String[] line;
				while ((line = reader.nextLine()) != null) {
					corpus.add(line);
				}
			} catch (IOException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		} else if (atoms.equals("char")) {
			try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					corpus.add(line.replaceAll("\\s", "").split(""));
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}

		Segmenter<String> segmenter = new Segmenter<String>(concentration);
		try {
			segmenter.segment(corpus, 100, outFile);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			System.out.println(eval(inputFile, outFile));
		} catch (Exception e) {
			System.out.println("Error during evaluation");
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println("Finished");
	}

	/**
	 * Evaluate the model output using F1-score where precision and recall are computed on the segment level
	 * 
	 * @param pathToGoldFile
	 *            The file containing the gold segments
	 * @param pathToOutputFile
	 *            The file containing the predicted segments
	 * @return A string containing precision, recall and F1
	 * @throws FileNotFoundException
	 *             If one of the input files does not exist
	 * @throws IOException
	 *             If something goes wrong while reading the files
	 */
	public static String eval(String pathToGoldFile, String pathToOutputFile) throws FileNotFoundException, IOException {
		double truePositives = 0;
		double falsePositives = 0;
		double falseNegatives = 0;

		try (TextReader goldReader = new TextReader(pathToGoldFile); TextReader outReader = new TextReader(pathToOutputFile)) {
			String[] outLine;
			
			while ((outLine = outReader.nextLine()) != null) {
				Counter<String> goldItems = new Counter<String>();
				Counter<String> predictedItems = new Counter<String>();
				goldItems.putAll(goldReader.nextLine(), 1.0);
				predictedItems.putAll(outLine, 1.0);
				double localTruePositives = 0;
				
				for (Map.Entry<String, Double> entry : predictedItems.entrySet()) {
					double goldCount = goldItems.get(entry.getKey());
					double predictedCount = entry.getValue();


					if (predictedCount > goldCount) {
						truePositives += goldCount;
						localTruePositives += goldCount;
						falsePositives += predictedCount - goldCount;
					} else {
						truePositives += predictedCount;
						localTruePositives += predictedCount;
					}
				}
				falseNegatives += goldItems.getTotal() - localTruePositives;
			}
		}

		double precision = truePositives / (truePositives + falsePositives);
		double recall = truePositives / (truePositives + falseNegatives);
		double f1 = 2 * precision * recall / (precision + recall);

		return String.format("Precision =%f, Recall = %f, F1 = %f", precision, recall, f1);
	}

}
