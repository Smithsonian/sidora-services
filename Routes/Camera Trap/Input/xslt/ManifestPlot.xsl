<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"    
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:output name="content" method="text"/>
    
 <!-- This stylesheet processes an XML file. Each project element will be mapped to a Research Project object in the repository.  Each Sub-Project element will be mapped 
     to a research project object in the repository.  Any plot information will result in a Plot object in the repository.  Camera Trap Deployment data will be mapped to a 
     cameratrap deployment object in the repository.  
     
 -->
    
    <xsl:template match="/">  
        <xsl:apply-templates select="/CameraTrapDeployment"/> 
    </xsl:template>
   
    <xsl:template match="CameraTrapDeployment">
        <xsl:call-template name="writeFiles">
            <xsl:with-param name="filename"><xsl:value-of select="ProjectId"></xsl:value-of>_<xsl:value-of select="PlotName"></xsl:value-of>.xml</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
        
    <xsl:template name="writeFiles">
        <xsl:param name="filename"/>
 
<!-- This section opens a file for the Plot concept object and writes it's metadata.   -->        
<!--        <xsl:result-document href="/Users/sternb/Documents/{$filename}"> -->
            
           
 <!-- This is the in-line datastream content for the descriptive metadata.  -->                                                
                            <metadata xmlns:fgdc="http://localhost/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                <idinfo>
                                    <citation>
                                        <citeinfo>
                                            <title><xsl:value-of select="PlotName"></xsl:value-of></title>
                                        </citeinfo>
                                    </citation>
                                    <descript>
                                        <supplinf></supplinf>
                                    </descript>
                                </idinfo>
                                <dataqual>
                                    <lineage>
                                        <method>
                                            <methdesc><xsl:value-of select="PlotTreatment"></xsl:value-of></methdesc>
                                        </method>
                                    </lineage>
                                </dataqual>
                            </metadata>
            
<!--        </xsl:result-document> -->
        
        
    </xsl:template>
</xsl:stylesheet>