# LinkedIn Employer Resolver

This project contains a service and a utility for identifying changes in Hadoop contributors current employer.  It expects a formatted input file and produces an output file reporting employer changes.  There are two classes to note: `EmployerResolver` and `LinkedInEmployerResolverService`.  The `EmployerResolver` class is the utility that takes an input file of users, and uses the `LinkedInEmployerResolverService` to query LinkedIn for changes.  The `LinkedInEmployerResolverService` is the service class that provides the interaction with the LinkedIn API using the [LinkedIn-J](http://code.google.com/p/linkedin-j/) java library.

Here is a note regarding the input file:

The input should be a tab delimited file containing the user's name, a tab, and the last company that we have recorded for them. If no company is provided, we will search and return the top 10 results of potential users. The results will be recorded for each user as either not found, or found with the strength of the result identified as POSITIVE, NEGATIVE, or WEAK. A POSITIVE match identifies that we have found 1 result with a match between the user's current employer and our data set's recorded employer. A WEAK match identifies that 1 user has been found, but we cannot identify by the user's headline if they work at the expected company or not. A NEGATIVE result identifies that 1 user has been found, but their headline indicates they work at a different employer. If there are more than one match for a user, the top 10 results will be listed in the output file.

## Running the app

The app requires java and maven to run, and can be executed as follows:

	$ mvn clean package
	$ ./run.sh <your API key> <your Secret key> /path/to/contributors.txt /path/to/changesReport.txt

The `contributors.txt` file in this case is the tab-delimited input file, and the `changesReport.txt` will be created by the program and will contain the output of the search/match/scoring process.