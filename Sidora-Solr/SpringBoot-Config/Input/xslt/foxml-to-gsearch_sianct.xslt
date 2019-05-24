<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:res="http://www.w3.org/2001/sw/DataAccess/rf1/result"
    xmlns:foxml="info:fedora/fedora-system:def/foxml#"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:eac="urn:isbn:1-931666-33-4"
    xmlns:fedora-model="info:fedora/fedora-system:def/model#"
        exclude-result-prefixes="xalan res foxml rdf dc oai_dc mods eac fedora-model">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

	<xsl:param name="HOST" select="'localhost'"/>
	<xsl:param name="PORT" select="'8080'"/>
	<xsl:param name="PROT" select="'http'"/>

    <xsl:param name="SOLR_HOST" select="'localhost'"/>
    <xsl:param name="SOLR_PORT" select="'8090'"/>
    <xsl:param name="SOLR_PROT" select="'http'"/>

	<xsl:variable name="PID" select="/foxml:digitalObject/@PID"/>
	<xsl:variable name="MODEL" select="/foxml:digitalObject/foxml:datastream[@ID = 'RELS-EXT']/foxml:datastreamVersion[last()]/foxml:xmlContent//fedora-model:hasModel/@rdf:resource"/>
	<xsl:variable name="STATE" select="/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state']/@VALUE"/>
	<xsl:variable name="OBJECT_URL" select="concat($PROT, '://', $HOST, ':', $PORT, '/fedora/objects/')"/> <!-- Only Anonymous Read -->
	<xsl:variable name="DATASTREAM_URL" select="concat($PROT, '://', $HOST, ':', $PORT, '/fedora/objects/', $PID, '/datastreams/')"/> <!-- Only Anonymous Read -->
	<xsl:variable name="FGS_URL" select="concat($PROT, '://', $HOST, ':', $PORT, '/fedoragsearch/rest?')"/> <!-- Only Anonymous Read -->
    <xsl:variable name="SOLR_URL" select="concat($SOLR_PROT, '://', $SOLR_HOST, ':', $SOLR_PORT, '/solr/gsearch_solr/select?')"/> <!-- Only Anonymous Read -->
	<xsl:variable name="RISEARCH_URL" select="concat($PROT, '://', $HOST, ':', $PORT, '/fedora/risearch?')"/> <!-- Only Anonymous Read -->

    <xsl:variable name="CT" select="document(concat($SOLR_URL, 'q=RELS-EXT.content.hasModel_it:%22si:cameraTrapCModel%22+AND+RELS-EXT.content.hasResource_it:%22', $PID, '%22&amp;version=2.2'))"/>
    <xsl:variable name="ctPID" select="$CT//doc/str[@name='PID']"/>

    <xsl:variable name="SITE" select="document(concat($SOLR_URL, 'q=RELS-EXT.content.hasModel_it:%22si:ctPlotCModel%22+AND+RELS-EXT.content.hasConcept_it:%22', $ctPID, '%22+AND+dsid:%22FGDC-CTPlot%22&amp;version=2.2'))"/>
    <xsl:variable name="sitePID" select="$SITE//doc/str[@name='PID']"/>

    <xsl:variable name="childPID2">
        <xsl:choose>
           <xsl:when test="$SITE/response/result/@numFound > 0">
               <xsl:value-of select="$sitePID"/>
           </xsl:when>
           <xsl:otherwise>
               <xsl:value-of select="$ctPID"/>
           </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="PARK" select="document(concat($SOLR_URL, 'q=RELS-EXT.content.hasModel_it:%22si:ctPlotCModel%22+AND+RELS-EXT.content.hasConcept_it:%22', $childPID2, '%22+AND+dsid:%22FGDC-Research%22&amp;version=2.2'))"/>
    <xsl:variable name="parkPID" select="$PARK//doc/str[@name='PID']"/>

    <xsl:variable name="SUB" select="document(concat($SOLR_URL, 'q=RELS-EXT.content.hasModel_it:%22si:projectCModel%22+AND+RELS-EXT.content.hasConcept_it:%22', $childPID2, '%22&amp;version=2.2'))"/>
    <xsl:variable name="subPID" select="$SUB//doc/str[@name='PID']"/>

    <xsl:variable name="childPID1">
        <xsl:choose>
           <xsl:when test="$PARK/response/result/@numFound > 0">
               <xsl:value-of select="$parkPID"/>
           </xsl:when>
           <xsl:when test="$SUB/response/result/@numFound > 0">
               <xsl:value-of select="$subPID"/>
           </xsl:when>
           <xsl:otherwise>
               <xsl:value-of select="$childPID2"/>
           </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="subprojectPID">
        <xsl:choose>
           <xsl:when test="$PARK/response/result/@numFound > 0">
               <xsl:value-of select="$parkPID"/>
           </xsl:when>
           <xsl:when test="$SUB/response/result/@numFound > 0">
               <xsl:value-of select="$subPID"/>
           </xsl:when>
           <xsl:otherwise>
           </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="subprojectLabel">
        <xsl:choose>
           <xsl:when test="$PARK/response/result/@numFound > 0">
               <xsl:value-of select="$PARK//doc/str[@name='label']"/>
           </xsl:when>
           <xsl:when test="$SUB/response/result/@numFound > 0">
               <xsl:value-of select="$SUB//doc/str[@name='label']"/>
           </xsl:when>
           <xsl:otherwise>
           </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="PROJECT" select="document(concat($SOLR_URL, 'q=RELS-EXT.content.hasModel_it:%22si:projectCModel%22+AND+RELS-EXT.content.hasConcept_it:%22', $childPID1, '%22&amp;version=2.2'))"/>
    <xsl:variable name="projectPID" select="$PROJECT//doc/str[@name='PID']"/>

    <xsl:variable name="FGDC" select="document(concat($OBJECT_URL, $ctPID, '/datastreams/FGDC/content'))"/>
    <xsl:variable name="HAS_FGDC" select="$CT//arr[@name='dsid']/str[text()='FGDC']/text()"/>

	<xsl:template match="/">
            <xsl:if test="$STATE = 'Active'">
            <xsl:if test="$projectPID">
            <xsl:if test="$HAS_FGDC">
            <xsl:if test="$MODEL = 'info:fedora/si:datasetCModel'">
                <add>
                    <doc>
                        <xsl:apply-templates select="foxml:digitalObject"/>
                    </doc>
                </add>
            </xsl:if>
            </xsl:if>
            </xsl:if>
            </xsl:if>
	</xsl:template>

	<xsl:template match="foxml:digitalObject">

		<field name="PID">
            <xsl:value-of select="$PID"/>
		</field>
		<field name="datasetLabel">
            <xsl:value-of select="foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label']/@VALUE"/>
		</field>

		<!-- select $ctPID $ctLabel $sitePID $siteLabel $parkPID $parkLabel $projectPID $projectLabel from <#ri>
where  $dsPID <mulgara:is> <info:fedora/si:286545>
and $ctPID <info:fedora/fedora-system:def/relations-external#hasResource> $dsPID
and $sitePID <info:fedora/fedora-system:def/relations-external#hasConcept> $ctPID
and $parkPID <info:fedora/fedora-system:def/relations-external#hasConcept> $sitePID
and $projectPID <info:fedora/fedora-system:def/relations-external#hasConcept> $parkPID
and $ctPID <info:fedora/fedora-system:def/model#label> $ctLabel
and $sitePID <info:fedora/fedora-system:def/model#label> $siteLabel
and $parkPID <info:fedora/fedora-system:def/model#label> $parkLabel
and $projectPID <info:fedora/fedora-system:def/model#label> $projectLabel 

			<xsl:comment><xsl:value-of select="$RIQUERY"/></xsl:comment>
        <xsl:variable name="RI" select="document($RIQUERY)"/>
            <xsl:comment><xsl:copy-of select="$RI"/></xsl:comment>
        <xsl:variable name="ctPID" select="$RI//ctPID/@uri"/>
		<field name="ctPID">
			<xsl:value-of select="$ctPID"/>
		</field>
		<field name="ctLabel">
			<xsl:value-of select="$RI//ctLabel"/>
		</field>
		-->

		<field name="ctPID">
			<xsl:value-of select="$ctPID"/>
		</field>
		<field name="ctLabel">
			<xsl:value-of select="$CT//doc/str[@name='label']"/>
		</field>

		<field name="sitePID">
			<xsl:value-of select="$sitePID"/>
		</field>
		<field name="siteLabel">
			<xsl:value-of select="$SITE//doc/str[@name='label']"/>
		</field>

                <!--
		<field name="parkPID">
			<xsl:value-of select="$parkPID"/>
		</field>
		<field name="parkLabel">
			<xsl:value-of select="$PARK//doc/str[@name='label']"/>
		</field>
                -->

                <field name="parkPID">
                        <xsl:value-of select="$subprojectPID"/>
                </field>
                <field name="parkLabel">
                        <xsl:value-of select="$subprojectLabel"/>
                </field>

		<field name="projectPID">
			<xsl:value-of select="$projectPID"/>
		</field>
		<field name="projectLabel">
			<xsl:value-of select="$PROJECT//doc/str[@name='label']"/>
		</field>

		<field name="cameraId"><xsl:value-of select="$FGDC/metadata/eainfo/detailed/attr[attrlabl = 'Camera ID']/attrdomv/edom/edomv"/></field>
		<field name="cameraLongitude"><xsl:value-of select="$FGDC/metadata/eainfo/detailed/attr[attrlabl = 'Actual Longitude']/attrdomv/edom/edomv"/></field>
		<field name="cameraLatitude"><xsl:value-of select="$FGDC/metadata/eainfo/detailed/attr[attrlabl = 'Actual Latitude']/attrdomv/edom/edomv"/></field>
		<field name="cameraDeploymentBeginDate"><xsl:value-of select="$FGDC/metadata/idinfo/timeperd/timeinfo/rngdates/begdate"/></field>
		<field name="cameraDeploymentEndDate"><xsl:value-of select="$FGDC/metadata/idinfo/timeperd/timeinfo/rngdates/enddate"/></field>
		<field name="cameraCiteinfo"><xsl:value-of select="$FGDC/metadata/idinfo/citation/citeinfo/othercit/text()"/></field>

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
