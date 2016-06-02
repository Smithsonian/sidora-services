<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:output name="content" method="text"/>
    <xsl:param name="imageid"/>

    <!-- This stylesheet creates MODS metadata for a camera trap image.
    -->

    <xsl:template match="/">
        <xsl:apply-templates select="/CameraTrapDeployment"/>

    </xsl:template>

    <!-- This template allows us to grab the   -->

    <xsl:template match="CameraTrapDeployment">
        <xsl:param name="projectid" select="ProjectId"></xsl:param>
        <xsl:param name="camerasitename" select="CameraSiteName"></xsl:param>
        <xsl:param name="imagesequenceid" select="ImageSequence/Image[ImageId=$imageid]/ancestor::ImageSequence/ImageSequenceId"></xsl:param>
        <xsl:param name="imagecount" select="count(ImageSequence[ImageSequenceId=$imagesequenceid]/Image)"></xsl:param>
        <xsl:call-template name="writeFiles">
            <xsl:with-param name="projectid"><xsl:value-of select="$projectid"></xsl:value-of></xsl:with-param>
            <xsl:with-param name="imagecount" select="$imagecount"></xsl:with-param>
            <xsl:with-param name="imagesequenceid"><xsl:value-of select="$imagesequenceid"></xsl:value-of></xsl:with-param>
            <xsl:with-param name="camerasitename"><xsl:value-of select="$camerasitename"></xsl:value-of></xsl:with-param>
            <xsl:with-param name="imageid"><xsl:value-of select="$imageid"></xsl:value-of></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="writeFiles">
        <xsl:param name="imageid"></xsl:param>
        <xsl:param name="filename"/>
        <xsl:param name="imagecount"></xsl:param>
        <xsl:param name="imagesequenceid"></xsl:param>
        <xsl:param name="projectid"></xsl:param>
        <xsl:param name="camerasitename"></xsl:param>
        <!-- This section opens a file for the metadata.   -->
        <!-- <xsl:result-document href="/Users/sternb/Documents/{$filename}">-->
        <mods xmlns="http://www.loc.gov/mods/v3" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/1999/xlink">
            <titleInfo>
                <title><xsl:value-of select="$camerasitename"></xsl:value-of><xsl:text>, </xsl:text>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/ImageOrder"></xsl:value-of>
                    <xsl:text> of </xsl:text>
                    <xsl:value-of select="$imagecount"></xsl:value-of>
                </title>
            </titleInfo>
            <identifier type="Image ID"><xsl:value-of select="$imageid"></xsl:value-of></identifier>
            <relatedItem>
                <identifier type="Image Sequence ID"><xsl:value-of select="$imagesequenceid"></xsl:value-of></identifier>
            </relatedItem>
            <typeOfResource>still image</typeOfResource>
            <abstract></abstract>
            <physicalDescription>
                <digitalOrigin><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/digitalOrigin"></xsl:value-of></digitalOrigin>
            </physicalDescription>
            <subject authority="Camera Trap Data Network Photo Type">
                <topic><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/PhotoType"></xsl:value-of></topic>
            </subject>
            <xsl:for-each select="ImageSequence/Image[ImageId=$imageid]/ImageIdentifications/Identification">
                <subject authorityURI="http://www.iucnredlist.org">
                    <topic><xsl:value-of select="SpeciesScientificName"></xsl:value-of></topic>
                </subject>
            </xsl:for-each>
            <subject>
                <topic></topic>
                <geographic></geographic>
                <cartographics>
                    <coordinates></coordinates>
                </cartographics>
            </subject>
            <xsl:for-each select="ImageSequence/Image[ImageId=$imageid]/PhotoTypeIdentifications/PhotoTypeIdentifiedBy">
                <name>
                    <namePart><xsl:value-of select="."></xsl:value-of></namePart>
                    <role>
                        <roleTerm authority="marcrelator" type="text">Photo Type Identified By</roleTerm>
                    </role>
                </name>
            </xsl:for-each>
            <note type="Interest Rank"><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/ImageInterestRanking"></xsl:value-of></note>
            <originInfo>
                <dateCaptured><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/ImageDateTime"></xsl:value-of></dateCaptured>
                <dateOther type="Embargo Period End Date"><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/EmbargoPeriodEndDate"></xsl:value-of></dateOther>
            </originInfo>
            <location>
                <physicalLocation></physicalLocation>
                <holdingSimple>
                    <copyInformation>
                        <subLocation></subLocation>
                    </copyInformation>
                </holdingSimple>
            </location>
            <accessCondition type="restriction on access"><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/RestrictionsonAccess"></xsl:value-of></accessCondition>
            <accessCondition type="use and reproduction"><xsl:value-of select="ImageSequence/Image[ImageId=$imageid]/ImageUseRestrictions"></xsl:value-of></accessCondition>
        </mods>

        <!--</xsl:result-document>-->

    </xsl:template>
</xsl:stylesheet>