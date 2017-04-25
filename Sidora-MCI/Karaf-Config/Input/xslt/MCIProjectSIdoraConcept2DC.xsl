<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="1.0">
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="SIdoraConcept">
       <oai_dc:dc 
            xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
            xmlns:dc="http://purl.org/dc/elements/1.1/" 
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
           <xsl:choose>
               <xsl:when test="primaryTitle/titleText">
                    <dc:title><xsl:value-of select="primaryTitle/titleText"/></dc:title>
               </xsl:when>
               <xsl:when test="primaryTitle/taxonName">
                   <dc:title><xsl:value-of select="primaryTitle/taxonName/species"/></dc:title>
               </xsl:when>
               <xsl:when test="primaryTitle/persnameText">
                   <dc:title><xsl:value-of select="primaryTitle/persnameText"/></dc:title>
               </xsl:when>
               <xsl:when test="primaryTitle/persname">
                   <dc:title>
                       <xsl:if test="primaryTitle/persname/lastname">
                           <xsl:value-of select="primaryTitle/persname/lastname"/><xsl:text>, </xsl:text>            
                       </xsl:if>
                       <xsl:if test="primaryTitle/persname/firstname">
                           <xsl:value-of select="primaryTitle/persname/firstname"/><xsl:text> </xsl:text>            
                       </xsl:if>
                       <xsl:if test="primaryTitle/persname/middlename">
                           <xsl:value-of select="primaryTitle/persname/middlename"/><xsl:text> </xsl:text>            
                       </xsl:if>
                       <xsl:if test="primaryTitle/persname/suffix">
                           <xsl:value-of select="primaryTitle/persname/suffix"/><xsl:text> </xsl:text>            
                       </xsl:if>
                   </dc:title>
               </xsl:when>
           </xsl:choose>
           <xsl:choose>
               <xsl:when test="conceptType">
                    <dc:type><xsl:value-of select="conceptType"/></dc:type>
               </xsl:when>
               <xsl:when test="resourceType">
                    <dc:type><xsl:value-of select="resourceType"/></dc:type>
               </xsl:when>
           </xsl:choose>
       </oai_dc:dc>
    </xsl:template>
    
</xsl:stylesheet>
