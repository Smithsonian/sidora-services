<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:map="xalan://java.util.Map"
                extension-element-prefixes="map">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:output name="content" method="text"/>

    <xsl:param name="correlationId"/>

    <!-- This stylesheet processes an XML file. Each project element will be mapped to a Resource object in the repository.
     
 -->
    
    <xsl:template match="/">  
        <xsl:apply-templates select="/Fields"/> 
    </xsl:template>
   
    <xsl:template match="Fields">
        <xsl:call-template name="writeFiles">
            <xsl:with-param name="filename"><xsl:value-of select="Field[@Name='MCI_ID']"/><xsl:text>_</xsl:text><xsl:value-of select="Field[@Name='Title']"/>.xml</xsl:with-param>
            
        </xsl:call-template>
    </xsl:template>
        
    <xsl:template name="writeFiles">
        <xsl:param name="filename"/>
 
<!-- This section opens a file for the Project concept object and writes metadata.   -->        
        <!--<xsl:result-document href="/Users/sternb/Documents/{$filename}">-->
            
 <!-- This is the in-line datastream content for the descriptive metadata.  -->                                                
            <SIdoraConcept>
                <resourceType>Generic Object</resourceType>
                <primaryTitle>
                    <titleText><xsl:value-of select="Field[@Name='MCI_ID']"/><xsl:text> </xsl:text><xsl:value-of select="Field[@Name='Title']"/></titleText>
                </primaryTitle>
                <altTitle type="">
                    <titleText></titleText>
                </altTitle>
                <summaryDescription></summaryDescription>
                <keyword type="MCI Request CorrelationId">
                    <value><xsl:value-of select="$correlationId"/></value>
                </keyword>
                <identifier type="">
                    <value></value>
                </identifier>
                <rights type="">
                    <value></value>
                    <note></note>
                </rights>
                <source type="MCI Web Service">
                    <value></value>
                    <note></note>
                </source>
                <relationship type="" id="">
                    <value></value>
                    <note></note>
                </relationship>
                <note type=""></note>
                <timePeriod type="Date of Request">
                    <intervalname><xsl:value-of select="Field[@Name='RequestDate']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                </timePeriod>
                <place type="">
                    <geogname type=""></geogname>
                    <boundedby>
                        <northlat></northlat>
                        <westlong></westlong>
                        <southlat></southlat>
                        <eastlong></eastlong>
                    </boundedby>
                    <timePeriod type="">
                        <intervalname></intervalname>
                        <date type="">
                            <year></year>
                            <month></month>
                            <day></day>
                        </date>
                    </timePeriod>
                    <note></note>
                </place>
                <actor role="" id="">
                    <persname>
                        <firstname></firstname>
                        <middlename></middlename>
                        <lastname></lastname>
                        <suffix></suffix>
                        <contactInfo type="">
                            <address1></address1>
                            <address2></address2>
                            <city></city>
                            <state></state>
                            <postalCode></postalCode>
                            <country></country>
                            <phone type=""></phone>
                            <email type=""></email>
                        </contactInfo>
                    </persname>
                </actor>
                <actor role="">
                    <corpname></corpname>
                </actor>
                <actor>
                    <taxonName>
                        <genus></genus>
                        <species></species>
                        <scientificNameAuthorship></scientificNameAuthorship>
                    </taxonName>
                </actor>
                <format type="XML">
                    <value>XML</value>
                    <note></note>
                </format>
                <method type="">
                    <p></p>
                    <note></note>
                </method>
                <technique type="">
                    <value></value>
                    <note></note>
                </technique>
                <measurements>
                    <measurementText></measurementText>
                    <measurement type="" value="" units=""></measurement>
                    <timePeriod type="">
                        <intervalname></intervalname>
                        <date type="">
                            <year></year>
                            <month></month>
                            <day></day>
                        </date>
                    </timePeriod>
                    <accuracy></accuracy>
                    <note></note>
                </measurements>
            </SIdoraConcept>
              
        <!--</xsl:result-document>-->
        
    </xsl:template>
</xsl:stylesheet>