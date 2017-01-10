package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;

import io.TextReader;
import mainUtils.InputChecker;
import models.Segmenter;

public class PhraseExtractor {

	public static void main(String[] args) {

		String usage = "java -jar PhraseExtractor.jar ";
		JSAP commandLineParser = new JSAP();
		List<String> possibleSegments = Arrays.asList("string", "char");

		try {
			FlaggedOption atoms = new FlaggedOption("atoms").setStringParser(JSAP.STRING_PARSER).setDefault("String").setLongFlag("atoms")
					.setShortFlag('s');
			atoms.setHelp("Determine the atoms of the segments. Needs to be one of " + possibleSegments + ". If the atoms are "
					+ "strings, the systems segments sentences into phrases, if the atoms are chars it segments into words.");
			commandLineParser.registerParameter(atoms);

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

		Segmenter<String> segmenter = new Segmenter<String>(1);
		try {
			segmenter.segment(corpus, 100);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Finished");
	}

}
