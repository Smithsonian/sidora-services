<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/association">
        <xsl:for-each select="title_field">

            <xsl:call-template name="join">
                <xsl:with-param name="list" select="*" />
            </xsl:call-template>
        </xsl:for-each>

    </xsl:template>

    <xsl:template name="join">
        <xsl:param name="list" />

        <xsl:text>/*[local-name() = '</xsl:text>
        <xsl:for-each select="$list">
            <xsl:value-of select="." />
            <xsl:choose>
                <xsl:when test="position() != last()">
                    <xsl:text>']/*[local-name() = '</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>']</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>