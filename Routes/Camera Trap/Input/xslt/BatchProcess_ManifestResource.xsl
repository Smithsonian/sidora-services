<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:param name="titleLabel"/>
    <xsl:param name="titlePath"/>
    <!--<xsl:param name="camelSplitIndex"/>-->


    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*[local-name() = $titlePath]/text()">
        <xsl:value-of select="$titleLabel"/>
    </xsl:template>


    <!--<xsl:template match="@*|node()">
        <xsl:variable name="path-to-title">
            <xsl:for-each select="ancestor-or-self::node()">
                <xsl:value-of select="name()" />
                <xsl:if test="position()!=last()">
                    <xsl:text>/</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$titlePath=$path-to-title">
                <xsl:copy>
                    <xsl:value-of select="concat(., '(', $camelSplitIndex, ')')"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>-->

</xsl:stylesheet>