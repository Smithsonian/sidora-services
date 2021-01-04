<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"    
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:output name="content" method="text"/>
    
 <!-- This stylesheet processes an XML file. Each project element will be mapped to a Research Project object in the repository.  Each Sub-Project element will be mapped 
     to a research project object in the repository.  Any plot information will result in a Plot object in the repository.  Camera Trap Deployment data will be mapped to a 
     cameratrap deployment object in the repository.  
     
 -->
    
    <xsl:param name="cameraMake"/>
    <xsl:param name="cameraModel"/>

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
<!--        <xsl:result-document href="/Users/sternb/Documents/{$filename}"> -->
            
 
 <!-- This is the in-line datastream content for the  descriptive metadata.  -->
                            
        <metadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="C:/Users/liuf/Documents/Sidora/CAMERA~1/AMAZON~1/fgdc-std-001-dot-1-1999/fgdc-std-001.1-1999.xsd">
            <idinfo>
                <citation>
                    <citeinfo>
                        <xsl:for-each select="Originators/Originator">
                            <origin><xsl:value-of select="OriginatorName"></xsl:value-of></origin>
                        </xsl:for-each>
                        <pubdate></pubdate>
                        <title><xsl:value-of select="CameraSiteName"></xsl:value-of></title>
                        <geoform>remote-sensing image</geoform>
                        <othercit><xsl:value-of select="CameraDeploymentID"></xsl:value-of></othercit>
                    </citeinfo>
                </citation>
                <descript>
                    <abstract><xsl:value-of select="CameraDeploymentNotes"></xsl:value-of></abstract>
                    <purpose>See Project Objectives</purpose>
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
                        <themekey>Camera Trap</themekey>
                    </theme>
                </keywords>
                <taxonomy>
                    <keywtax>
                        <taxonkt>http://www.iucnredlist.org</taxonkt>
                        <taxonkey>multiple species</taxonkey>
                    </keywtax>
                    <taxoncl>
                        <taxonrn>Kingdom</taxonrn>
                        <taxonrv>Animal</taxonrv>
                        <common>Animal</common>
                        <xsl:for-each-group select="//ImageSequence/ResearcherIdentifications/Identification" group-by="SpeciesScientificName">
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
                <logic></logic>
                <complete><xsl:value-of select="CameraFailureDetails"></xsl:value-of></complete>
                <lineage>
                    <method>
                        <methtype>Bait</methtype>
                        <methodid>
                            <methkt>Camera Trap Data Network</methkt>
                            <methkey><xsl:value-of select="Bait"></xsl:value-of></methkey>
                        </methodid>
                        <methdesc><xsl:value-of select="BaitDescription"></xsl:value-of></methdesc>
                    </method>
                    <method>
                        <methtype>Feature</methtype>
                        <methodid>
                            <methkt>Camera Trap Data Network</methkt>
                            <methkey><xsl:value-of select="Feature"></xsl:value-of></methkey>
                        </methodid>
                        <methdesc><xsl:value-of select="FeatureMethodology"></xsl:value-of></methdesc>
                    </method>
                    <procstep>
                        <procdesc>Proposed Camera Deployment Begin Date</procdesc>
                        <procdate><xsl:value-of select="ProposedCameraDeploymentBeginDate"></xsl:value-of></procdate>
                    </procstep>
                    <procstep>
                        <procdesc>Proposed Camera Deployment End Date</procdesc>
                        <procdate><xsl:value-of select="ProposedCameraDeploymentEndDate"></xsl:value-of></procdate>
                    </procstep>
                </lineage>
            </dataqual>
            <spref>
                <vertdef>
                    <altsys>
                        <altdatum>North American Vertical Datum of 1988</altdatum>
                        <altres></altres>
                        <altunits>feet</altunits>
                        <altenc>
                            Explicit elevation coordinate included with horizontal coordinates
                        </altenc>
                    </altsys>
                </vertdef>
            </spref>
            <eainfo>
                <detailed>
                    <enttyp>
                        <enttypl>Camera Settings</enttypl>
                        <enttypd>Camera Settings for the Deployment.</enttypd>
                        <enttypds>Camera Trap Data Network</enttypds>
                    </enttyp>
                    <attr>
                        <attrlabl>Camera Make</attrlabl>
                        <attrdef>The Digital Camera Manufacture</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv>
                                    <xsl:choose>
                                        <xsl:when test="$cameraMake != ''">
                                            <xsl:value-of select="$cameraMake"></xsl:value-of>
                                        </xsl:when>
                                        <xsl:otherwise>None</xsl:otherwise>
                                    </xsl:choose>
                                </edomv>
                                <edomvd>The Digital Camera Manufacture</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Camera Model</attrlabl>
                        <attrdef>The Digital Camera Model</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv>
                                    <xsl:choose>
                                        <xsl:when test="$cameraModel != ''">
                                            <xsl:value-of select="$cameraModel"></xsl:value-of>
                                        </xsl:when>
                                        <xsl:otherwise>None</xsl:otherwise>
                                    </xsl:choose>
                                </edomv>
                                <edomvd>The Digital Camera Model</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Camera ID</attrlabl>
                        <attrdef>The Unique identifier for the camera.</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="CameraID"></xsl:value-of></edomv>
                                <edomvd>The Unique identifier for the camera.</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Image Resolution Setting</attrlabl>
                        <attrdef>Image Resolution Setting</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="ImageResolutionSetting"></xsl:value-of></edomv>
                                <edomvd>Image Resolution Setting</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Detection Distance</attrlabl>
                        <attrdef>
                            Maximum distance at which a camera triggered, as tested during deployment, measured in meters
                        </attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="DetectionDistance"></xsl:value-of></edomv>
                                <edomvd>
                                    Maximum distance at which a camera triggered, as tested during deployment, measured in meters
                                </edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Sensitivity Setting</attrlabl>
                        <attrdef>Sensitivity setting for motion sensor</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="SensitivitySetting"></xsl:value-of></edomv>
                                <edomvd>Sensitivity setting for motion sensor</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Quiet Period Setting</attrlabl>
                        <attrdef>
                            Time set as minimum break between triggers of the camera, measured in seconds
                        </attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="QuietPeriodSetting"></xsl:value-of></edomv>
                                <edomvd>
                                    Time set as minimum break between triggers of the camera, measured in seconds
                                </edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Actual Latitude</attrlabl>
                        <attrdef>The actual latitude of the camera</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="ActualLatitude"></xsl:value-of></edomv>
                                <edomvd>The actual latitude of the camera</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Actual Longitude</attrlabl>
                        <attrdef>The actual longitude of the camera</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="ActualLongitude"></xsl:value-of></edomv>
                                <edomvd>The actual longitude of the camera</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Proposed Latitude</attrlabl>
                        <attrdef>The proposed latitude of the camera</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="ProposedLatitude"></xsl:value-of></edomv>
                                <edomvd>The proposed latitude of the camera</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                    <attr>
                        <attrlabl>Proposed Longitude</attrlabl>
                        <attrdef>The proposed longitude of the camera</attrdef>
                        <attrdefs>Camera Trap Data Network</attrdefs>
                        <attrdomv>
                            <edom>
                                <edomv><xsl:value-of select="ProposedLongitude"></xsl:value-of></edomv>
                                <edomvd>The proposed longitude of the camera</edomvd>
                                <edomvds>Camera Trap Data Network Standard</edomvds>
                            </edom>
                        </attrdomv>
                    </attr>
                </detailed>
            </eainfo>
            <metainfo>
                <metstdn>
                    FGDC Biological Data Profile of the Content Standard for Digital Geospatial Metadata
                </metstdn>
                <metstdv>1999</metstdv>
            </metainfo>
        </metadata>
             
        <!-- </xsl:result-document> -->
        
        
    </xsl:template>
</xsl:stylesheet>