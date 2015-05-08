<?xml version="1.0" encoding="UTF-8"?>
<iso:schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:iso="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" schemaVersion="ISO19757-3">
    <iso:pattern id="required-fields-check">
        <iso:rule context="CameraTrapDeployment">
            <iso:assert
                test="string-length(normalize-space(ProjectId)) &gt; 0">A camera deployment must have a project Id</iso:assert>
            <iso:assert
                test="string-length(normalize-space(ProjectName)) &gt; 0">A camera deployment must have a project name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SubProjectId)) &gt; 0">A camera deployment must have a sub-project Id</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SubProjectName)) &gt; 0">A camera deployment must have a sub-project name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(CameraDeploymentID)) &gt; 0">A camera deployment must have a camera deployment id</iso:assert>
            <iso:assert
                test="string-length(normalize-space(CameraSiteName)) &gt; 0">A camera deployment must have a camera site name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(CameraDeploymentBeginDate)) &gt; 0">A camera deployment must have a Camera deployment begin date</iso:assert>
            <iso:assert
                test="string-length(normalize-space(CameraDeploymentEndDate)) &gt; 0">A camera deployment must have a camera deployment end date</iso:assert>
            <iso:assert 
                test="string-length(normalize-space(CameraFailureDetails)) &gt; 0">A camera deployment must have camera failure details</iso:assert>
            <iso:assert 
                test="string-length(normalize-space(ActualLatitude)) &gt; 0">A camera deployment must have an actual latitude</iso:assert>
            <iso:assert 
                test="string-length(normalize-space(ActualLongitude)) &gt; 0">A camera deployment must have an actual Longitude</iso:assert>
            <iso:assert
                test="string-length(normalize-space(Bait)) &gt; 0">A camera deployment must have a bait</iso:assert>
            <iso:assert
                test="string-length(normalize-space(Feature)) &gt; 0">A camera deployment must have a feature</iso:assert>
            <iso:assert
                test="string-length(normalize-space(CameraID)) &gt; 0">A camera deployment must have a camera Id</iso:assert>
        </iso:rule>
    </iso:pattern>
    <iso:pattern id="check-for-plot-treatment-only">
        <iso:rule context="CameraTrapDeployment">
            <iso:assert test="if ((string-length(normalize-space(PlotTreatment)) &gt; 0) and ((string-length(normalize-space(PlotName))= 0) )) then false() else true()" >If Plot Treatment exists, Plot name is required.</iso:assert>
        </iso:rule>
    </iso:pattern>
    <iso:pattern id="image-sequence-required-fields-check">
        <iso:rule context="CameraTrapDeployment/ImageSequence">
            <iso:assert
                test="string-length(normalize-space(ImageSequenceId)) &gt; 0">An image sequence must have an Id</iso:assert>
            <iso:assert
                test="string-length(normalize-space(ImageSequenceBeginTime)) &gt; 0">An image sequence must have a begin time</iso:assert>
            <iso:assert
                test="string-length(normalize-space(ImageSequenceEndTime)) &gt; 0">An image sequence must have an end time</iso:assert>
        </iso:rule>
    </iso:pattern>
    <iso:pattern id="volunteer-identifications-required-fields-check">
        <iso:rule context="CameraTrapDeployment/ImageSequence/VolunteerIdentifications/Identification">
            <iso:assert
                test="string-length(normalize-space(SpeciesId)) &gt; 0">A identification must have an IUCNId</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SpeciesScientificName)) &gt; 0">An identification must have a scientific name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SpeciesCommonName)) &gt; 0">An identification must have a common name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(Count)) &gt; 0">An identification must have a count</iso:assert>
        </iso:rule>
    </iso:pattern>
    <iso:pattern id="researcher-identifications-required-fields-check">
        <iso:rule context="CameraTrapDeployment/ImageSequence/ResearcherIdentifications/Identification">
            <iso:assert
                test="string-length(normalize-space(SpeciesId)) &gt; 0">A identification must have an IUCNId</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SpeciesScientificName)) &gt; 0">An identification must have a scientific name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(SpeciesCommonName)) &gt; 0">An identification must have a common name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(Count)) &gt; 0">An identification must have a count</iso:assert>
        </iso:rule>
    </iso:pattern>
    <iso:pattern id="image-required-fields-check">
        <iso:rule context="CameraTrapDeployment/ImageSequence/Image">
            <iso:assert
                test="string-length(normalize-space(ImageId)) &gt; 0">An image must have an Id</iso:assert>
            <iso:assert
                test="string-length(normalize-space(ImageFileName)) &gt; 0">An image must have a file name</iso:assert>
            <iso:assert
                test="string-length(normalize-space(ImageInterestRanking)) &gt; 0">An image must have an image ranking</iso:assert>
        </iso:rule>
    </iso:pattern>
</iso:schema>