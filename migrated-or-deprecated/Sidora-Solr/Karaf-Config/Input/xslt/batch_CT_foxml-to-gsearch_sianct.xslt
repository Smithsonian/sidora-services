<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2015-2016 Smithsonian Institution.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
  ~ use this file except in compliance with the License.You may obtain a copy of
  ~ the License at: http://www.apache.org/licenses/
  ~
  ~ This software and accompanying documentation is supplied without
  ~ warranty of any kind. The copyright holder and the Smithsonian Institution:
  ~ (1) expressly disclaim any warranties, express or implied, including but not
  ~ limited to any implied warranties of merchantability, fitness for a
  ~ particular purpose, title or non-infringement; (2) do not assume any legal
  ~ liability or responsibility for the accuracy, completeness, or usefulness of
  ~ the software; (3) do not represent that use of the software would not
  ~ infringe privately owned rights; (4) do not warrant that the software
  ~ is error-free or will be maintained, supported, updated or enhanced;
  ~ (5) will not be liable for any indirect, incidental, consequential special
  ~ or punitive damages of any kind or nature, including but not limited to lost
  ~ profits or loss of data, on any basis arising from contract, tort or
  ~ otherwise, even if any of the parties has been warned of the possibility of
  ~ such loss or damage.
  ~
  ~ This distribution includes several third-party libraries, each with their own
  ~ license terms. For a complete copy of all copyright and license terms, including
  ~ those of third-party libraries, please see the product release notes.
  -->

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:res="http://www.w3.org/2001/sw/DataAccess/rf1/result"
	xmlns:ri="http://www.w3.org/2005/sparql-results#"
    xmlns:foxml="info:fedora/fedora-system:def/foxml#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:eac="urn:isbn:1-931666-33-4"
	xmlns:fedora-model="info:fedora/fedora-system:def/model#"
		exclude-result-prefixes="xalan res foxml rdf dc oai_dc mods eac fedora-model ri">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

    <xsl:param name="pid"/>
	<xsl:param name="dsLabel"/>
	<xsl:param name="state"/>

 <!--Headers Supplied by CT Ingest JMS
	Cant use these header's in the sidora solr route because it would break on Fedora JMS
	&lt;!&ndash;Project (projectPID)&ndash;&gt;
	<xsl:param name="ProjectId"/>
	<xsl:param name="ProjectName"/>
	<xsl:param name="ProjectPID"/>

	&lt;!&ndash;SubProject (parkPID)&ndash;&gt;
	<xsl:param name="SubProjectId"/>
	<xsl:param name="SubProjectName"/>
	<xsl:param name="SubProjectPID"/>

	&lt;!&ndash;Plot (sitePID)&ndash;&gt;
	<xsl:param name="PlotId"/>
	<xsl:param name="PlotName"/>
	<xsl:param name="PlotPID"/>

	&lt;!&ndash;Site (ctPID)&ndash;&gt;
	<xsl:param name="SiteId"/>
	<xsl:param name="SiteName"/>
	<xsl:param name="SitePID"/>
	
	<xsl:param name="siteFGDC"/>-->

	<xsl:param name="HOST" select="'localhost'"/>
	<xsl:param name="PORT" select="'8080'"/>
	<xsl:param name="PROT" select="'http'"/>

	<xsl:variable name="OBJECT_URL" select="concat($PROT, '://', $HOST, ':', $PORT, '/fedora/objects/')"/> <!-- Only Anonymous Read -->

	<!--<xsl:param name="FUSEKI_PORT" select="'9080'"/>
	<xsl:param name="FUSEKI_DATASET" select="'fedora3'"/>

	<xsl:variable name="FUSEKI_URL" select="concat($PROT, '://', $HOST, ':', $FUSEKI_PORT, '/fuseki/', $FUSEKI_DATASET, '?output=xml')"/> &lt;!&ndash; Only Anonymous Read &ndash;&gt;

	<xsl:param name="FUSEKI_QUERY"/>
	<xsl:variable name="FUSEKI_RESPONSE" select="document(concat($FUSEKI_URL, '&amp;query=', $FUSEKI_QUERY))"/>
	<xsl:variable name="ctPID" select="substring-after($FUSEKI_RESPONSE/ri:sparql/ri:results/ri:result/ri:binding[@name = 'ctPID']/ri:uri , 'info:fedora/')"/>-->

	<xsl:variable name="ctPID" select="substring-after(/ri:sparql/ri:results/ri:result/ri:binding[@name = 'ctPID']/ri:uri , 'info:fedora/')"/>

	<xsl:variable name="FGDC" select="document(concat($OBJECT_URL, $ctPID, '/datastreams/FGDC/content'))"/>

    <xsl:template match="/">
        <doc>
			<field name="PID">
				<xsl:value-of select="$pid"/>
			</field>
			<field name="datasetLabel">
				<xsl:value-of select="$dsLabel"/>
			</field>
			<xsl:apply-templates select="/ri:sparql/ri:results/ri:result[1]"/>

			<!--<xsl:apply-templates select="$FUSEKI_RESPONSE/ri:sparql/ri:results/ri:result"/>-->

			<!-- Use the headers from the ct ingest JMS
				Cant use these header's in the sidora solr route because it would break on Fedora JMS

				<field name="ctPID">
					<xsl:value-of select="$SitePID"/>
				</field>
				<field name="ctLabel">
					<xsl:value-of select="$SiteName"/>
				</field>

				<field name="sitePID">
					<xsl:value-of select="$PlotPID"/>
				</field>
				<field name="siteLabel">
					<xsl:value-of select="$PlotName"/>
				</field>

				<field name="parkPID">
					<xsl:value-of select="$SubProjectPID"/>
				</field>
				<field name="parkLabel">
					<xsl:value-of select="$SubProjectName"/>
				</field>

				<field name="projectPID">
					<xsl:value-of select="$ProjectPID"/>
				</field>
				<field name="projectLabel">
					<xsl:value-of select="$ProjectName"/>
				</field>-->
			
            <xsl:apply-templates select="$FGDC/metadata"/>
        </doc>
    </xsl:template>

	<xsl:template match="/ri:sparql/ri:results/ri:result[1]">
		<field name="ctPID">
			<xsl:value-of select="$ctPID"/>
		</field>
		<field name="ctLabel">
			<xsl:value-of select="/ri:sparql/ri:results/ri:result/ri:binding[@name = 'ctLabel']/ri:literal"/>
		</field>

		<field name="sitePID">
			<xsl:value-of select="substring-after(/ri:sparql/ri:results/ri:result/ri:binding[@name = 'sitePID']/ri:uri, 'info:fedora/')"/>
		</field>
		<field name="siteLabel">
			<xsl:value-of select="/ri:sparql/ri:results/ri:result/ri:binding[@name = 'siteLabel']/ri:literal"/>
		</field>

		<field name="parkPID">
			<xsl:value-of select="substring-after(/ri:sparql/ri:results/ri:result/ri:binding[@name = 'parkPID']/ri:uri, 'info:fedora/')"/>
		</field>
		<field name="parkLabel">
			<xsl:value-of select="/ri:sparql/ri:results/ri:result/ri:binding[@name = 'parkLabel']/ri:literal"/>
		</field>

		<field name="projectPID">
			<xsl:value-of select="substring-after(/ri:sparql/ri:results/ri:result/ri:binding[@name = 'projectPID']/ri:uri, 'info:fedora/')"/>
		</field>
		<field name="projectLabel">
			<xsl:value-of select="/ri:sparql/ri:results/ri:result/ri:binding[@name = 'projectLabel']/ri:literal"/>
		</field>
	</xsl:template>

	<xsl:template match="metadata">
		<field name="cameraId"><xsl:value-of select="$FGDC//metadata/eainfo/detailed/attr[attrlabl = 'Camera ID']/attrdomv/edom/edomv"/></field>
		<field name="cameraLongitude"><xsl:value-of select="$FGDC//metadata/eainfo/detailed/attr[attrlabl = 'Actual Longitude']/attrdomv/edom/edomv"/></field>
		<field name="cameraLatitude"><xsl:value-of select="$FGDC//metadata/eainfo/detailed/attr[attrlabl = 'Actual Latitude']/attrdomv/edom/edomv"/></field>
		<field name="cameraDeploymentBeginDate"><xsl:value-of select="$FGDC//metadata/idinfo/timeperd/timeinfo/rngdates/begdate"/></field>
		<field name="cameraDeploymentEndDate"><xsl:value-of select="$FGDC//metadata/idinfo/timeperd/timeinfo/rngdates/enddate"/></field>
		<field name="cameraCiteinfo"><xsl:value-of select="$FGDC//metadata/idinfo/citation/citeinfo/othercit/text()"/></field>

		<xsl:for-each select="$FGDC//method[methtype='Feature']">
			<field name="featureType"><xsl:value-of select="methodid/methkey"/></field>
		</xsl:for-each>

		<xsl:for-each select="$FGDC//taxoncl[taxonrn='Species']">
			<field name="speciesTaxonrv"><xsl:value-of select="taxonrv"/></field>
			<field name="speciesTaxonrvCommon"><xsl:value-of select="taxonrv"/> (<xsl:value-of select="common"/>)</field>
		</xsl:for-each>
	</xsl:template>
        <!-- Disable default output of apply-templates when no matches are found. This needs to be defined for each mode -->
	<xsl:template match="node() | @*"/>
</xsl:stylesheet>
