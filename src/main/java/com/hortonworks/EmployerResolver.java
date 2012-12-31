package com.hortonworks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.code.linkedinapi.schema.People;
import com.google.code.linkedinapi.schema.Person;
import com.hortonworks.service.LinkedInEmployerResolverService;

/**
 * Used to resolve the current employer of a user based on their first, last,
 * and name of company. The name of the company is the last company that we have
 * recorded for them.
 * 
 * The input should be a tab delimited file containing the user's name, a tab,
 * and the last company that we have recorded for them. If no company is
 * provided, we will search and return the top 10 results of potentional users.
 * The results will be recorded for each user as either not found, or found with
 * the strength of the result identified as POSITIVE, NEGATIVE, or WEAK. A
 * POSOTIVE match identifies that we have found 1 result with a match between
 * the user's current employer and our data set's recorded employer. A WEAK
 * match identifies that 1 user has been found, but we cannot identify by the
 * user's headline if they work at the expected company or not. A NEGATIVE
 * result identifies that 1 user has been found, but their headline indicates
 * they work at a different employer. If there are more than one match for a
 * user, the top 10 results will be listed in the output file.
 * 
 * @author Paul Codding paul@hortonworks.com
 * 
 */
public class EmployerResolver {
	private static Logger logger = Logger.getLogger(EmployerResolver.class);
	private static final String OUTPUT_DELIM = "|";

	public static void main(String[] args) {
		if (args != null && args.length == 4) {
			LinkedInEmployerResolverService service = new LinkedInEmployerResolverService(
					args[0], args[1]);
			String inputFilePath = args[2];
			String outputFilePath = args[3];
			BufferedWriter output = null;
			logger.info("Starting EmployerResolver with input file: "
					+ inputFilePath + " and output file: " + outputFilePath);

			try {
				/*
				 * Read the input file and process each line containing name and
				 * company
				 */
				BufferedReader input = new BufferedReader(new FileReader(
						inputFilePath));
				output = new BufferedWriter(new FileWriter(outputFilePath));
				String line = null;
				while ((line = input.readLine()) != null) {
					logger.debug(line);
					String[] pieces = line.split("\\t");
					String name = null;
					String company = null;
					try {
						name = pieces[0];
						company = pieces[1];
					} catch (ArrayIndexOutOfBoundsException e) {
						company = "Unknown";
					}
					String firstName = null;
					String lastName = null;
					String[] namePieces = name.split(" ");
					if (namePieces.length == 2) {
						firstName = namePieces[0];
						lastName = namePieces[1];
					} else if (namePieces.length == 3) {
						firstName = namePieces[0];
						lastName = namePieces[2];
					}
					// If we have a valid user, search for them
					if (firstName != null && lastName != null) {
						logger.info("Looking for user with full name: '" + name
								+ "', firstName: '" + firstName
								+ " ', lastName: '" + lastName + "'"
								+ " and validating company: '" + company + "'");
						People people = null;
						/*
						 * If the company is not set or unknown only search by
						 * first and last name
						 */
						if (company == null || company.equals("Unknown"))
							people = service.searchForUserByName(firstName,
									lastName);
						// otherwise include the company in the search for a
						// better match
						else
							people = service.searchForUserByName(firstName,
									lastName, company);
						/*
						 * If we actually have results process them looking for
						 * POSITIVE, WEAK, and NEGATIVE matches, and write the
						 * results to the output file.
						 */
						if (people != null && people.getPersonList().size() > 0) {
							// Check for a single match and process the changes
							// in employer
							if (people.getPersonList().size() == 1) {
								Person person = people.getPersonList().get(0);
								// Check for a headline like
								// "Solutions Engineer at (Hortonworks)"
								if (person.getHeadline() != null) {
									Pattern pattern = Pattern
											.compile(".*at(.*)");
									Matcher matcher = pattern.matcher(person
											.getHeadline());
									if (matcher.find()) {
										/*
										 * If found see if the headline contains
										 * the company we expect, or contains a
										 * close spelling
										 */
										if (matcher.group(1) != null) {
											int editDistance = calculateEditDistance(
													company, matcher.group(1));
											// If we have an close match on
											// employer, mark it is POSITIVE
											if (matcher.group(1).contains(
													company)
													|| editDistance < 5)
												output.write("Search for name: '"
														+ name
														+ "' with expected employer '"
														+ company
														+ "' returned 1 result with a POSITIVE match on current employer:"
														+ company + "\n");
											// Otherwise the user may have
											// switched jobs, mark as NEGATIVE
											else
												output.write("Search for name: '"
														+ name
														+ "' with expected employer '"
														+ company
														+ "' returned 1 result with a NEGATIVE match on current employer:"
														+ company + "\n");
											;
										}
										/*
										 * If the headline does not contain an
										 * "at (Employer)", we can only provide
										 * a WEAK association
										 */
									} else
										output.write("Search for name: '"
												+ name
												+ "' with expected employer '"
												+ company
												+ "' returned 1 result with a WEAK match on current employer:"
												+ company + "\n");
								}
							} else
								output.write("Search for name: '" + name
										+ "' with expected employer '"
										+ company
										+ "' returned multiple results:\n");
							// List all results of the search for manual
							// inspection
							for (Person person : people.getPersonList()) {
								output.write("\t" + person.getId()
										+ OUTPUT_DELIM
										+ person.getPublicProfileUrl()
										+ OUTPUT_DELIM + person.getFirstName()
										+ " " + person.getLastName()
										+ OUTPUT_DELIM + person.getHeadline()
										+ "\n");
							}

						} else {
							output.write("Could not find name: " + name + "\n");
							logger.warn("Search yielded no results for name: "
									+ name);
						}
					} else
						logger.warn("Could not figure out what to do with this name: "
								+ name);
					output.write("-----------------------------------------------------------\n");
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (output != null)
					try {
						// Close the output file
						output.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
			}
		} else
			printUsage();
	}

	// Thanks to: http://www.javalobby.org/java/forums/t15908.html for the LD
	// implementation
	private static int calculateEditDistance(String s, String t) {
		int n = s.length();
		int m = t.length();

		if (n == 0)
			return m;
		if (m == 0)
			return n;

		int[][] d = new int[n + 1][m + 1];

		for (int i = 0; i <= n; d[i][0] = i++)
			;
		for (int j = 1; j <= m; d[0][j] = j++)
			;

		for (int i = 1; i <= n; i++) {
			char sc = s.charAt(i - 1);
			for (int j = 1; j <= m; j++) {
				int v = d[i - 1][j - 1];
				if (t.charAt(j - 1) != sc)
					v++;
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
						v);
			}
		}
		return d[n][m];
	}

	public static void printUsage() {
		System.err
				.println("./run.sh <consumerKey> <consumerSecret> <path to tab delimited input file> <path to output file>");
	}
}