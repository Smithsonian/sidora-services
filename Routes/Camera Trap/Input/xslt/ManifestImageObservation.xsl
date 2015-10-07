<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"    
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:output name="content" method="text"/>
    
 <!-- This stylesheet transforms camera trap manifest data into fgdc tabular data object metadata. 
     
 -->
    
    <xsl:template match="/">  
        <xsl:apply-templates select="/CameraTrapDeployment"/> 
    </xsl:template>
 
    
    <xsl:template match="CameraTrapDeployment">
        <xsl:call-template name="writeFiles">
            <xsl:with-param name="filename"><xsl:value-of select="ProjectId"></xsl:value-of>_<xsl:value-of select="CameraDeploymentID"></xsl:value-of>.xml</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
        
    <xsl:template name="writeFiles">
        <xsl:param name="filename"/>
 
<!-- This section opens a file for the camera deployment concept object and writes it's  metadata.   -->        
<!--        <xsl:result-document href="/Users/sternb/Documents/{$filename}">-->
            
 
 <!-- This is the in-line datastream content for the  descriptive metadata.  -->
                            
        <metadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="C:/Users/liuf/Documents/Sidora/CAMERA~1/AMAZON~1/fgdc-std-001-dot-1-1999/fgdc-std-001.1-1999.xsd">
            <idinfo>
                <citation>
                    <citeinfo>
                        <xsl:for-each select="Originators/Originator">
                            <origin><xsl:value-of select="OriginatorName"></xsl:value-of></origin>
                        </xsl:for-each>
                        <pubdate></pubdate>
                        <title><xsl:text>Camera Site Name:  </xsl:text><xsl:value-of select="CameraSiteName"></xsl:value-of><xsl:text> Image Observations</xsl:text></title>
                        <geoform>tabular digital data</geoform>
                        <othercit></othercit>
                    </citeinfo>
                </citation>
                <descript>
                    <abstract></abstract>
                    <purpose></purpose>
                </descript>
                <timeperd>
                    <timeinfo>
                        <rngdates>
                            <begdate><xsl:value-of select="translate(CameraDeploymentBeginDate, '-', '')"></xsl:value-of></begdate>
                            <enddate><xsl:value-of select="translate(CameraDeploymentEndDate, '-','')"></xsl:value-of></enddate>
                        </rngdates>
                    </timeinfo>
                    <current>observed</current>
                </timeperd>
                <status>
                    <progress>In work</progress>
                    <update>As needed</update>
                </status>
                <keywords>
                    <theme>
                        <themekt>None</themekt>
                        <themekey>Image Observations</themekey>
                    </theme>
                </keywords>
                <taxonomy>
                    <keywtax>
                        <taxonkt>http://www.iucnredlist.org</taxonkt>
                        <taxonkey>multiple species</taxonkey>
                    </keywtax>
                    <taxonsys>
                        <classsys>
                            <classcit>
                                <citeinfo>
                                    <origin/>
                                    <pubdate></pubdate>
                                    <title/>
                                    <geoform></geoform>
                                </citeinfo>
                            </classcit>
                        </classsys>
                        <taxonpro/>
                        <taxoncom/>
                    </taxonsys>
                    <taxoncl>
                        <taxonrn>Kingdom</taxonrn>
                        <taxonrv>Animal</taxonrv>
                        <common>Animal</common>
                        <xsl:for-each-group select="//ImageSequence/Image/ImageIdentifications/Identification" group-by="SpeciesScientificName">
                            <taxoncl>
                                <taxonrn>Species</taxonrn>
                                <taxonrv><xsl:value-of select="current-grouping-key()"></xsl:value-of></taxonrv>
                                
                            <xsl:for-each-group select="current-group()" group-by="SpeciesCommonName">
                                <common><xsl:value-of select="SpeciesCommonName"></xsl:value-of></common>
                            </xsl:for-each-group>                               
                            </taxoncl>
                        </xsl:for-each-group>
                    </taxoncl>
                </taxonomy>
                <accconst><xsl:value-of select="AccessConstraints"></xsl:value-of></accconst>
                <useconst></useconst>
            </idinfo>
            <dataqual>
                <attracc>
                    <attraccr/>
                </attracc>
                <logic></logic>
                <complete></complete>
                <posacc>
                    <horizpa>
                        <horizpar/>
                    </horizpa>
                    <vertacc>
                        <vertaccr/>
                    </vertacc>
                </posacc>
                <lineage>
                    <method>
                        <methodid>
                            <methkt></methkt>
                            <methkey></methkey>
                        </methodid>
                        <methdesc/>
                    </method>
                    <procstep>
                        <procdesc/>
                        <procdate/>
                    </procstep>
                </lineage>
                <cloud/>
            </dataqual>
            <spref>
                <horizsys>
                    <geograph>
                        <latres/>
                        <longres/>
                        <geogunit></geogunit>
                    </geograph>
                    <geodetic>
                        <horizdn/>
                        <ellips/>
                        <semiaxis/>
                        <denflat/>
                    </geodetic>
                </horizsys>
                <vertdef>
                    <altsys>
                        <altdatum/>
                        <altres/>
                        <altunits></altunits>
                        <altenc/>
                    </altsys>
                    <depthsys>
                        <depthdn/>
                        <depthres/>
                        <depthdu/>
                        <depthem></depthem>
                    </depthsys>
                </vertdef>
            </spref>
            <eainfo>
                <detailed>
                    <enttyp>
                        <enttypl/>
                        <enttypd/>
                        <enttypds/>
                    </enttyp>
                    <attr>
                        <attrlabl/>
                        <attrdef/>
                        <attrdefs/>
                        <attrdomv>
                            <edom>
                                <edomv/>
                                <edomvd/>
                                <edomvds/>
                            </edom>
                        </attrdomv>
                        <begdatea/>
                        <enddatea/>
                        <attrvai>
                            <attrva/>
                            <attrvae/>
                        </attrvai>
                        <attrmfrq></attrmfrq>
                    </attr>
                </detailed>
                <overview>
                    <eaover/>
                    <eadetcit/>
                </overview>
            </eainfo>
            <distinfo>
                <distrib>
                    <cntinfo>
                        <cntperp>
                            <cntper/>
                        </cntperp>
                        <cntaddr>
                            <addrtype></addrtype>
                            <address/>
                            <city/>
                            <state/>
                            <postal/>
                        </cntaddr>
                        <cntvoice/>
                        <cntemail/>
                    </cntinfo>
                </distrib>
                <resdesc/>
                <distliab/>
                <custom/>
                <techpreq/>
            </distinfo>
            <metainfo>
                <metd/>
                <metrd/>
                <metfrd/>
                <metc>
                    <cntinfo>
                        <cntperp>
                            <cntper/>
                            <cntorg/>
                        </cntperp>
                        <cntaddr>
                            <addrtype></addrtype>
                            <address/>
                            <city/>
                            <state/>
                            <postal/>
                        </cntaddr>
                        <cntvoice/>
                        <cntemail/>
                    </cntinfo>
                </metc>
                <metstdn>FGDC Biological Data Profile of the Content Standard for Digital Geospatial Metadata</metstdn>
                <metstdv>1999</metstdv>
                <metac/>
                <metuc/>
                <metsi>
                    <metscs/>
                    <metsc></metsc>
                    <metshd/>
                </metsi>
            </metainfo>
        </metadata>
             
        <!--</xsl:result-document>-->
        
        
    </xsl:template>
</xsl:stylesheet>