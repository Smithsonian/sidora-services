<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    
    <xsl:output indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:key name="kTaxoncl" match="taxoncl" use="concat('N',taxonrn,'D',taxonrv,'M',common)"/>
    
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="taxoncls">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="taxoncl[generate-id() = generate-id(key('kTaxoncl', concat('N',taxonrn,'D',taxonrv,'M',common)))]"/>
        </xsl:copy>
    </xsl:template>      
    
    
</xsl:stylesheet>