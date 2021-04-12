<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:res="http://www.w3.org/2001/sw/DataAccess/rf1/result"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:mods="http://www.loc.gov/mods/v3" xmlns:eac="urn:isbn:1-931666-33-4"
                version="1.0"
                exclude-result-prefixes="xalan res foxml rdf dc oai_dc mods eac">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
    <!-- Parameters -->
    <xsl:param name="REPOSITORYNAME" select="repositoryName"/>
    <xsl:param name="FEDORASOAP" select="repositoryName"/>
    <xsl:param name="FEDORAUSER" select="repositoryName"/>
    <xsl:param name="FEDORAPASS" select="repositoryName"/>
    <xsl:param name="TRUSTSTOREPATH" select="repositoryName"/>
    <xsl:param name="TRUSTSTOREPASS" select="repositoryName"/>
    <xsl:param name="HOST" select="'localhost'"/>
    <xsl:param name="PORT" select="'8080'"/>
    <xsl:param name="PROT" select="'http'"/>
    <xsl:variable name="PID" select="/foxml:digitalObject/@PID"/>
    <xsl:variable name="STATE"
                  select="/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state']/@VALUE"/>
    <xsl:variable name="DATASTREAM_URL"
                  select="concat($PROT, '://', $HOST, ':', $PORT, '/fedora/objects/', $PID, '/datastreams/')"/> <!-- Only Anonymous Read -->
    <xsl:variable name="irregular" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ -,'"/>
    <xsl:variable name="normalized" select="'abcdefghijklmnopqrstuvwxyz__'"/>
    <!-- main -->
    <xsl:template match="/">
        <!--<add>-->
            <doc>
                <xsl:apply-templates select="foxml:digitalObject"/>
            </doc>
        <!--</add>-->
    </xsl:template>
    <!-- Object: Grabs fields common to all fedora objects -->
    <xsl:template match="foxml:digitalObject">
        <field name="PID">
            <!-- PID-->
            <xsl:value-of select="$PID"/>
        </field>
        <xsl:for-each select="foxml:objectProperties/foxml:property">
            <xsl:variable name="name" select="substring-after(@NAME,'#')"/>
            <!-- Object Properties -->
            <field>
                <xsl:attribute name="name">
                    <xsl:value-of select="substring-after(@NAME,'#')"/>
                </xsl:attribute>
                <xsl:value-of select="@VALUE"/>
            </field>
        </xsl:for-each>
        <!-- Object Datastreams -->
        <xsl:for-each select="foxml:datastream">
            <xsl:variable name="prefix" select="concat(@ID, '.')"/>
            <field>
                <xsl:attribute name="name">dsid</xsl:attribute>
                <xsl:value-of select="@ID"/>
            </field>
            <!-- Datastreams Properties-->
            <xsl:for-each select="@*[local-name() != 'ID'] | foxml:datastreamVersion[last()]/@*[local-name() != 'ID']">
                <xsl:variable name="name" select="translate(local-name(), $irregular, $normalized)"/>
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, $name)"/>
                    </xsl:attribute>
                    <xsl:value-of select="self::node()"/>
                </field>
            </xsl:for-each>
            <!-- Datastreams Content -->
            <xsl:if test="$STATE = 'Active'">
                <!-- Only index Datastream content of Active objects -->
                <xsl:choose>
                    <xsl:when test="self::node()[@CONTROL_GROUP = 'X']">
                        <xsl:apply-templates select="self::node()[@STATE = 'A']">
                            <xsl:with-param name="prefix" select="concat($prefix, 'content.')"/>
                            <xsl:with-param name="content" select="foxml:datastreamVersion[last()]/foxml:xmlContent"/>
                        </xsl:apply-templates>
                    </xsl:when>
                    <xsl:when test="foxml:datastreamVersion[last()]/@MIMETYPE = 'text/xml'">
                        <!-- <xsl:message>Matched XML</xsl:message> -->
                        <xsl:apply-templates select="self::node()[@STATE = 'A']">
                            <xsl:with-param name="prefix" select="concat($prefix, 'content.')"/>
                            <xsl:with-param name="content" select="document(concat($DATASTREAM_URL, @ID, '/content'))"/>
                        </xsl:apply-templates>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- There are no templates to match. -->
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- RELS-EXT (basic-datastream) -->
    <xsl:template match="foxml:datastream[@ID = 'RELS-EXT']">
        <xsl:param name="prefix"/>
        <xsl:param name="content"/>
        <xsl:for-each select="$content/rdf:RDF/rdf:Description/*">
            <xsl:variable name="name" select="local-name()"/>
            <field>
                <xsl:attribute name="name">
                    <xsl:value-of select="concat($prefix, $name, '_s')"/>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="@rdf:resource and starts-with(@rdf:resource, 'info:fedora/')">
                        <xsl:value-of select="substring-after(@rdf:resource, 'info:fedora/')"/>
                    </xsl:when>
                    <xsl:when test="@rdf:resource">
                        <xsl:value-of select="@rdf:resource"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="text()"/>
                    </xsl:otherwise>
                </xsl:choose>
            </field>
        </xsl:for-each>
    </xsl:template>
    <!-- DC (basic-datastream) -->
    <xsl:template match="foxml:datastream[@ID = 'DC']">
        <xsl:param name="prefix"/>
        <xsl:param name="content"/>
        <xsl:for-each select="$content/oai_dc:dc/*">
            <xsl:variable name="name" select="substring-after(name(),':')"/>
            <xsl:if test="text()[normalize-space(.)]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, $name, '_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- MODS -->
    <xsl:template match="foxml:datastream[@ID = 'MODS']">
        <xsl:param name="prefix"/>
        <xsl:param name="content"/>
        <xsl:for-each select="$content//mods:mods/mods:titleInfo/mods:title">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'title_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="../mods:nonSort/text()"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:mods/mods:titleInfo/mods:title[@type='alternative']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'titleAlt_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="../mods:nonSort/text()"/>
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--Creator-->
        <xsl:for-each select="$content//mods:name//mods:role[mods:roleTerm = 'creator']/../../mods:name/mods:namePart">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'creator_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'creator']/../../mods:name/mods:namePart[@type='family']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'creatorFamilyName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'creator']/../../mods:name/mods:namePart[@type='given']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'creatorGivenName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'creator']/../../mods:name/mods:namePart[@type='termsOfAddress']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'creatorTA_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='corporate']//mods:role[mods:roleTerm = 'creator']/../../mods:name/mods:namePart">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'creatorCorporate_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--Creator-->
        <!--Contributor-->
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'contributor']/../../mods:name/mods:namePart">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'contributor_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'contributor']/../../mods:name/mods:namePart[@type='family']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'contributorFamilyName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'contributor']/../../mods:name/mods:namePart[@type='given']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'contributorGivenName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='personal']//mods:role[mods:roleTerm = 'contributor']/../../mods:name/mods:namePart[@type='termsOfAddress']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'contributorTA_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each
                select="$content//mods:name[@type='corporate']//mods:role[mods:roleTerm = 'contributor']/../../mods:name/mods:namePart[@type='termsOfAddress']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'contributorTA_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--Contributor-->
        <!--orginInfo-->
        <xsl:for-each select="$content//mods:originInfo/mods:place/mods:placeTerm">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat('mods_s', 'placeOfPublication_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:physicalDescription/*">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'physicalDescription_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:originInfo/mods:publisher">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'publisher_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:dateCreated[@keyDate='yes']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'dateCreated_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:dateCaptured">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'dateDigitized_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:originInfo/mods:dateIssued">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'dateIssued_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:originInfo/mods:copyrightDate">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'dateCopyright_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--orginInfo>-->
        <!--physicalDescription-->
        <xsl:for-each select="$content//mods:physicalDescription/mods:extent">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'extent_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:physicalDescription/mods:form">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'form_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:physicalDescription/mods:digitalOrigin">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'digitalOrigin_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:physicalDescription/mods:note">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'notePhysical_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--physicalDescription-->
        <xsl:for-each select="$content//mods:language/languageTerm[@type='text']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'language_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subTitle">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'subTitle_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:abstract">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'description_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subject/mods:genre">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'genre_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:note[@type='statement of responsibility']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'sor_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:note[@type='admin']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'noteAdmin_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--subjects-->
        <xsl:for-each select="$content//mods:subject/mods:topic">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'topic_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subject/mods:name[@type='corporate']/mods:namePart">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'organization_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subject/mods:name[@type='personal']/mods:namePart">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'people_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subject/mods:temporal">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'dates_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:subject/mods:geographic">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'geographic_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <!--subject-->
        <xsl:for-each select="$content//mods:country">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'country_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:county">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'county_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:region">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'region_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:city">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'city_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:citySection">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'city_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:originInfo/mods:edition">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'edition_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:accessCondition[@type='useAndReproduction']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'rightsStatement_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:originInfo/mods:issuance">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'issuance_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:relatedItem[@type='host']//title">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'hostObject_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:relatedItem[@type='constituent']//title">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'constituentObject_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:relatedItem[@type='series']//title">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'series_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:identifier[@type='local']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'localIdentifier_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:identifier[@type='isbn']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'isbn_')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:identifier[@type='issn']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'issn_')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:location/url[@usage='primary display']">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'handle_')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//mods:recordInfo//languageOfCataloging/languageTerm">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'languageOfCataloging_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- FGDC -->
    <xsl:template match="foxml:datastream[@ID = 'FGDC']">
        <xsl:param name="prefix"/>
        <xsl:param name="content"/>
        <xsl:for-each select="$content//eac:agencyName">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat('eac_', 'agencyName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//abstract">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'abstract','_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//purpose">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'purpose_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//status/progress">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'progress_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//seciinfo/secclass">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'secclass_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//keywords/place/placekey">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'placekey_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//keywords/theme/themekey">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'themekey_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//distinfo/distrib">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'distrib_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- EAC-CPF -->
    <xsl:template match="foxml:datastream[@ID = 'EAC-CPF']">
        <xsl:param name="prefix"/>
        <xsl:param name="content"/>
        <xsl:for-each select="$content//eac:agencyName">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'agencyName_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//eac:cpfDescription/eac:identity/eac:entityType">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, text(),'_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="../eac:nameEntry/eac:part[@localType='primary']"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//eac:biogHist/eac:p">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'biogHist_t')"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//eac:term">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'topic_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="."/>
                </field>
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="$content//eac:placeEntry">
            <xsl:if test="text()[normalize-space(.) ]">
                <field>
                    <xsl:attribute name="name">
                        <xsl:value-of select="concat($prefix, 'place_s')"/>
                    </xsl:attribute>
                    <xsl:value-of select="."/>
                </field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    <!-- Disable default output of apply-templates when no matches are found. This needs to be defined for each mode -->
    <xsl:template match="node() | @*"/>
    <!--
    <xsl:template match="node() | @*">
       <xsl:message>Otherwise Template</xsl:message>
    </xsl:template>
    -->
</xsl:stylesheet>
