<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xalan="http://xml.apache.org/xslt"
        version="1.0">
    <xsl:output method="xml" encoding="utf-8" indent="yes" xalan:indent-amount="4"/>
    <xsl:strip-space elements="*"/>


    <xsl:param name="cameraMake"/>
    <xsl:param name="cameraModel"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[enttypl = 'Camera Settings']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
        <!-- Insert the camera make and model-->
        <xsl:element name="attr">
            <xsl:element name="attrlabl">Camera Make</xsl:element>
            <xsl:element name="attrlabl">The Digital Camera Manufacture</xsl:element>
            <xsl:element name="attrlabl">Camera Trap Data Network</xsl:element>
            <xsl:element name="attrdomv">
                <xsl:element name="edom">
                    <xsl:element name="edomv">
                        <xsl:choose>
                            <xsl:when test="$cameraMake != ''"><xsl:value-of select="$cameraMake"></xsl:value-of></xsl:when>
                            <xsl:otherwise>None</xsl:otherwise>
                        </xsl:choose>
                    </xsl:element>
                    <xsl:element name="edomvd">The Digital Camera Manufacture</xsl:element>
                    <xsl:element name="edomvds">Camera Trap Data Network Standard</xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:element>
        <xsl:element name="attr">
            <xsl:element name="attrlabl">Camera Model</xsl:element>
            <xsl:element name="attrlabl">The Digital Camera Model</xsl:element>
            <xsl:element name="attrlabl">Camera Trap Data Network</xsl:element>
            <xsl:element name="attrdomv">
                <xsl:element name="edom">
                    <xsl:element name="edomv">
                        <xsl:choose>
                            <xsl:when test="$cameraModel != ''"><xsl:value-of select="$cameraModel"></xsl:value-of></xsl:when>
                            <xsl:otherwise>None</xsl:otherwise>
                        </xsl:choose>
                    </xsl:element>
                    <xsl:element name="edomvd">The Digital Camera Model</xsl:element>
                    <xsl:element name="edomvds">Camera Trap Data Network Standard</xsl:element>
                </xsl:element>
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>