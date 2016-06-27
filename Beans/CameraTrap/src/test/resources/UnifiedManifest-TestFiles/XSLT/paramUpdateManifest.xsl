<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:param name="replaceNodeValue"/>
    <xsl:param name="nodeToChange"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*[name() = $nodeToChange]/text()">
        <xsl:value-of select="$replaceNodeValue"/>
    </xsl:template>
</xsl:stylesheet>