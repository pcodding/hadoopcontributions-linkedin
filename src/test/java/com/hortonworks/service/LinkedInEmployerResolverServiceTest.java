package com.hortonworks.service;

import junit.framework.TestCase;

import com.google.code.linkedinapi.schema.People;

public class LinkedInEmployerResolverServiceTest extends TestCase {
	String consumerKeyValue = "your API Key";
	String consumerSecretValue = "your Secret Key";
	LinkedInEmployerResolverService service;

	protected void setUp() throws Exception {
		service = new LinkedInEmployerResolverService(consumerKeyValue,
				consumerSecretValue);
	}

	public void testSearch() {
		try {
			People people = service.searchForUserByName("Paul", "Codding");
			assertTrue(people != null && people.getCount() > 0);
			assertTrue(people.getPersonList().get(0) != null
					&& people.getPersonList().get(0).getHeadline() != null);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
