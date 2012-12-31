package com.hortonworks.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.PeopleApiClient;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.enumeration.SearchParameter;
import com.google.code.linkedinapi.client.enumeration.SearchSortOrder;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.People;
import com.google.code.linkedinapi.schema.Person;

public class LinkedInEmployerResolverService {
	private Logger logger = Logger.getLogger(this.getClass());
	String consumerKeyValue;
	String consumerSecretValue;
	String OUTPUT_DELIM = "|";

	LinkedInApiClientFactory factory;
	PeopleApiClient client;
	LinkedInOAuthService oauthService;

	public LinkedInEmployerResolverService(String consumerKeyValue,
			String consumerSecretValue) {
		this.consumerKeyValue = consumerKeyValue;
		this.consumerSecretValue = consumerSecretValue;
		initApi();
	}

	public LinkedInEmployerResolverService() {
	}

	/**
	 * Initialize the API by authenticating, and initializing the LinkedIn-J
	 * factory and client objects. This method will prompt the user to navigate
	 * to LinkedIn and authorize this application to access their profile and
	 * network. The pin needs to be entered when prompted.
	 */
	public void initApi() {
		oauthService = LinkedInOAuthServiceFactory.getInstance()
				.createLinkedInOAuthService(consumerKeyValue,
						consumerSecretValue);
		LinkedInRequestToken requestToken = oauthService.getOAuthRequestToken();
		String authUrl = requestToken.getAuthorizationUrl();
		System.out.println("Please Navigate to the following URL: " + authUrl);
		System.out.print("Please enter the token: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String oauthVerifier = null;

		try {
			// Read the OAuth response pin
			oauthVerifier = br.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to read the oauthVerifier!");
			System.exit(1);
		}

		logger.info("Read oauthVerifier: " + oauthVerifier);
		// Construct the access token
		LinkedInAccessToken accessToken = oauthService.getOAuthAccessToken(
				requestToken, oauthVerifier);

		// Initialize the Factory and client
		factory = LinkedInApiClientFactory.newInstance(consumerKeyValue,
				consumerSecretValue);
		client = factory.createLinkedInApiClient(accessToken.getToken(),
				accessToken.getTokenSecret());
	}

	/**
	 * Search for users by first and last name. This method returns a collection
	 * of users matching the search criteria.
	 * 
	 * @param firstName
	 *            First name of the user
	 * @param lastName
	 *            Last name of the user
	 * @return Collection of Person objects matching the search criteria
	 * @throws Exception
	 */
	public People searchForUserByName(String firstName, String lastName)
			throws Exception {
		return searchForUserByName(firstName, lastName, null);
	}

	/**
	 * Search for users by first, last, and company name. This method returns a
	 * collection of users matching the search criteria. The company name will
	 * be used to match against the most recent 3 positions for the user.
	 * 
	 * @param firstName
	 *            First name of the user
	 * @param lastName
	 *            Last name of the user
	 * @param companyName
	 *            Name of the company to search for
	 * @return
	 * @throws Exception
	 */
	public People searchForUserByName(String firstName, String lastName,
			String companyName) throws Exception {
		logger.info("Searching for user with firstName: " + firstName
				+ " and lastName: " + lastName);
		Map<SearchParameter, String> searchParameters = new EnumMap<SearchParameter, String>(
				SearchParameter.class);
		searchParameters.put(SearchParameter.FIRST_NAME, firstName);
		searchParameters.put(SearchParameter.LAST_NAME, lastName);
		if (companyName != null)
			searchParameters.put(SearchParameter.COMPANY_NAME, companyName);

		/*
		 * Specify the profile attributes that should be returned by the People
		 * Search API
		 */
		Set<ProfileField> profileAttributes = new HashSet<ProfileField>();
		profileAttributes.add(ProfileField.ID);
		profileAttributes.add(ProfileField.FIRST_NAME);
		profileAttributes.add(ProfileField.POSITIONS_TITLE);
		profileAttributes.add(ProfileField.SUMMARY);
		profileAttributes.add(ProfileField.LAST_NAME);
		profileAttributes.add(ProfileField.HEADLINE);
		profileAttributes.add(ProfileField.INDUSTRY);
		// Nice to have, but seldom returned without null values
		profileAttributes.add(ProfileField.PICTURE_URL);
		profileAttributes.add(ProfileField.THREE_CURRENT_POSITIONS);
		profileAttributes.add(ProfileField.TWITTER_ACCOUNTS);
		profileAttributes.add(ProfileField.PUBLIC_PROFILE_URL);
		profileAttributes.add(ProfileField.IM_ACCOUNTS);
		profileAttributes.add(ProfileField.SKILLS);

		People people = null;
		try {
			// Search for people specifying that results should be ordered by
			// relevance
			people = client.searchPeople(searchParameters, profileAttributes,
					SearchSortOrder.RELEVANCE);
			if (people != null) {
				logger.info("Total search result:"
						+ people.getPersonList().size());
				for (Person person : people.getPersonList()) {
					try {
						logger.info(person.getId() + OUTPUT_DELIM
								+ person.getPublicProfileUrl() + OUTPUT_DELIM
								+ person.getFirstName() + " "
								+ person.getLastName() + OUTPUT_DELIM
								+ person.getHeadline());
					} catch (Exception e) {
						// Some profiles are protected
						logger.warn("Don't have access to this profile: "
								+ person.getId() + OUTPUT_DELIM
								+ person.getPublicProfileUrl());
					}
				}
			}
		} catch (Exception e) {
			// We have a limit of 400 requests per day for developers
			if (e.getMessage().contains("Throttle")) {
				logger.error("Throttle limit reached while processing user: "
						+ firstName + " " + lastName);
				throw e;
			}
			logger.error(e.getMessage(), e);
		}
		return people;
	}
}