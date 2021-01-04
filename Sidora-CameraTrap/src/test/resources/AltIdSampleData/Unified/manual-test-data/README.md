Two test deployments for manually testing the InFlightConceptStatusPolling Route.
Make sure that no Project, SubProject, or deployment already exist and place the
two test deployments into the SMX CT Process directory.

Look for the onException retries from findObject where deployment information is
added to the cameraTrapStaticStore and InFlightConceptStatusPolling should remove the
deployment information from the cameraTrapStaticStore when the Project or Subproject
are found.

TODO: NEED A BETTER TEST FOR THIS