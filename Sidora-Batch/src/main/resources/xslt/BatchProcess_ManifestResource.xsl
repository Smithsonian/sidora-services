<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:param name="primaryTitleLabel"/>
    <xsl:param name="titlePath"/>
    <xsl:param name="CamelSplitIndex"/>


    <!--<xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*[local-name() = $titlePath]/text()">
        <xsl:value-of select="$titleLabel"/>
    </xsl:template>-->


    <xsl:template match="@*|node()">

        <xsl:variable name="path-to-title">
            <xsl:text>/*[local-name() = '</xsl:text>
            <xsl:for-each select="ancestor-or-self::node()">
                <xsl:value-of select="name()" />
                <xsl:choose>
                    <xsl:when test="position() != last()">
                        <xsl:text>']/*[local-name() = '</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>']</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="contains($path-to-title, $titlePath)">
                <xsl:choose>
                    <xsl:when test=".=''">
                        <xsl:copy>
                            <xsl:value-of select="$primaryTitleLabel"/>
                        </xsl:copy>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy>
                            <xsl:value-of select="concat(., '(', $CamelSplitIndex+1, ')')"/>
                        </xsl:copy>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

</xsl:stylesheet>