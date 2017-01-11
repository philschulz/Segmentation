package main;

import java.io.IOException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;

import mainUtils.InputChecker;
import models.Segmenter;

public class PhraseExtractor {

	public static void main(String[] args) {

		String usage = "java -jar PhraseExtractor.jar ";
		JSAP commandLineParser = new JSAP();

		try {

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
		
		Segmenter segmenter = new Segmenter(1);
		
		try {
			segmenter.segment(inputFile, 100);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		
		System.out.println("Finished");
	}

}
