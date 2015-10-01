<?xml version="1.0" encoding="UTF-8"?>
<!--Creates an Volunteer observation text file from Volunteer identifications in the manifest
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs">
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
    <xsl:template match="/">
        <volunteer xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">            
            <xsl:attribute name="xsi:noNamespaceSchemaLocation"></xsl:attribute>
            <xsl:for-each select="CameraTrapDeployment">
                <xsl:variable name="var2_cur" select="."/>
                <xsl:for-each select="ImageSequence">
                    <xsl:variable name="var1_cur" select="."/>
                    <xsl:for-each select="ResearcherIdentifications/Identification">
                        <xsl:text>Volunteer,</xsl:text>
                        <xsl:value-of select="string($var2_cur/CameraDeploymentID)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string($var1_cur/ImageSequenceId)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string($var1_cur/ImageSequenceBeginTime)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string($var1_cur/ImageSequenceEndTime)"/><xsl:text>,</xsl:text>
                        <xsl:text>"</xsl:text><xsl:value-of select="string(SpeciesScientificName)"/><xsl:text>",</xsl:text>
                        <xsl:text>"</xsl:text><xsl:value-of select="string(SpeciesCommonName)"/><xsl:text>",</xsl:text>
                        <xsl:value-of select="string(Age)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string(Sex)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string(IndividualId)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string(number(string(Count)))"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string(AnimalRecognizable)"/><xsl:text>,</xsl:text>
                        <xsl:text>"</xsl:text><xsl:value-of select="string(IndividualAnimalNotes)"/><xsl:text>",</xsl:text>
                        <xsl:value-of select="string(TSNId)"/><xsl:text>,</xsl:text>
                        <xsl:value-of select="string(IUCNId)"/>
                        <xsl:text>&#xa;</xsl:text>
                    </xsl:for-each>
                </xsl:for-each>
            </xsl:for-each>
        </volunteer>
    </xsl:template>
</xsl:stylesheet>
