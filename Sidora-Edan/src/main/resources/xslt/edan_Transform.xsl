<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2015-2016 Smithsonian Institution.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
  ~ use this file except in compliance with the License.You may obtain a copy of
  ~ the License at: "http://www.apache.org/licenses/
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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="1.0">
    <xsl:output method="text" encoding="UTF-8" media-type="text/plain"/>


    <xsl:param name="imageid"/>
    <xsl:param name="CamelFedoraPid"/>
    <xsl:param name="extraJson"/>
    <xsl:param name="edanId"/>

    <xsl:template match="/">

        <xsl:apply-templates select="/CameraTrapDeployment">
            <xsl:with-param name="imageid"><xsl:value-of select="$imageid"/></xsl:with-param>
            <xsl:with-param name="CamelFedoraPid"><xsl:value-of select="$CamelFedoraPid"/></xsl:with-param>
            <xsl:with-param name="extraJson"><xsl:value-of select="$extraJson"/></xsl:with-param>
            <xsl:with-param name="edanId"><xsl:value-of select="$edanId"/></xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="CameraTrapDeployment">
        <xsl:param name="imageid"/>
        <xsl:param name="CamelFedoraPid"/>
        <xsl:param name="extraJson"/>
        <xsl:param name="edanId"/>
        <xsl:text>{</xsl:text>
        <xsl:text>"content": &#xa;{</xsl:text>
        <xsl:value-of select="$extraJson"/>
        <xsl:text>&#xa;"project_id": "</xsl:text><xsl:value-of select="ProjectId"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"project_name": "</xsl:text><xsl:value-of select="ProjectName"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"sub_project_id": "</xsl:text><xsl:value-of select="SubProjectId"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"sub_project_name": "</xsl:text><xsl:value-of select="SubProjectName"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"deployment_id": "</xsl:text><xsl:value-of select="CameraDeploymentID"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"deployment_name": "</xsl:text><xsl:value-of select="CameraSiteName"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"image_sequence_id": "</xsl:text><xsl:value-of select="//ImageSequence[Image[ImageId=$imageid]]/ImageSequenceId"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"image": {</xsl:text>
        <xsl:text>&#xa;"id": "</xsl:text><xsl:value-of select="$imageid"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"online_media": [</xsl:text>
        <xsl:text>&#xa;{</xsl:text>
        <xsl:text>&#xa;"content": "http://ids.si.edu/ids/deliveryService?id=emammal_image_</xsl:text><xsl:value-of select="$imageid"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"idsId": "</xsl:text>emammal_image_<xsl:value-of select="$imageid"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"sidoraPid": "</xsl:text><xsl:value-of select="$CamelFedoraPid"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"type": "</xsl:text>Images<xsl:text>",</xsl:text>
        <xsl:text>&#xa;"caption": "</xsl:text>Camera Trap Image <xsl:for-each select="//ImageSequence[Image[ImageId = $imageid]]/ResearcherIdentifications/Identification/SpeciesCommonName">
          <xsl:value-of select="."/><xsl:if test="not(position() = last())">,</xsl:if>
        </xsl:for-each><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"thumbnail": "</xsl:text>http://ids.si.edu/ids/deliveryService?id=emammal_image_<xsl:value-of select="$imageid"/><xsl:text>&amp;max=100"</xsl:text>
        <xsl:text>&#xa;}</xsl:text>
        <xsl:text>&#xa;],</xsl:text>
        <xsl:text>&#xa;"date_time": "</xsl:text><xsl:value-of select="//Image[ImageId=$imageid]/ImageDateTime"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"photo_type": "</xsl:text><xsl:value-of select="//Image[ImageId=$imageid]/PhotoType"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"photo_type_identified_by": "</xsl:text><xsl:value-of select="//Image[ImageId=$imageid]/PhotoTypeIdentifications/PhotoTypeIdentifiedBy"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"interest_ranking": "</xsl:text>None<xsl:text>"</xsl:text>
        <xsl:text>&#xa;},</xsl:text>
        <xsl:text>&#xa;"image_identifications": [</xsl:text>
        <xsl:for-each select="//ImageSequence[Image[ImageId = $imageid]]/ResearcherIdentifications/Identification">
        <xsl:text>&#xa;{</xsl:text>
        <xsl:text>&#xa;"iucn_id": "</xsl:text><xsl:value-of
                select="IUCNId"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"species_scientific_name": "</xsl:text><xsl:value-of select="SpeciesScientificName"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"individual_animal_notes": "</xsl:text><xsl:value-of
                select="IndividualAnimalNotes"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"species_common_name": "</xsl:text><xsl:value-of select="SpeciesCommonName"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"count": </xsl:text><xsl:value-of select="Count"/><xsl:text>,</xsl:text>
        <xsl:text>&#xa;"age": "</xsl:text><xsl:value-of select="Age"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"sex": "</xsl:text><xsl:value-of select="Sex"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"individual_id": "</xsl:text><xsl:value-of select="IndividualId"/><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"animal_recognizable": "</xsl:text><xsl:value-of select="AnimalRecognizable"/><xsl:text>"</xsl:text>
        <xsl:text>&#xa;}</xsl:text>
        <xsl:if test="not(position() = last())">,</xsl:if>
        </xsl:for-each>
        <xsl:text>&#xa;]</xsl:text>
        <xsl:text>&#xa;},</xsl:text>

        <xsl:text>&#xa;"publicSearch": true,</xsl:text>
        <xsl:text>&#xa;"title": "</xsl:text>Camera Trap Image <xsl:for-each select="//ImageSequence[Image[ImageId = $imageid]]/ResearcherIdentifications/Identification/SpeciesCommonName">
          <xsl:value-of select="."/><xsl:if test="not(position() = last())">, </xsl:if>
        </xsl:for-each><xsl:text>",</xsl:text>
        <xsl:text>&#xa;"type": "</xsl:text>emammal_image<xsl:text>",</xsl:text>
        <xsl:text>&#xa;"url": "</xsl:text><xsl:value-of select="$imageid"/><xsl:text>"</xsl:text><xsl:if test="$edanId != ''"><xsl:text>,</xsl:text>
        <xsl:text>&#xa;"id": "</xsl:text><xsl:value-of select="$edanId"/><xsl:text>"</xsl:text></xsl:if>
        <xsl:text>&#xa;}</xsl:text>
    </xsl:template>

</xsl:stylesheet>
