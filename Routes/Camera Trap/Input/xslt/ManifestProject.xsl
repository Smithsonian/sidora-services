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
            <xsl:with-param name="filename"><xsl:value-of select="ProjectId"/>.xml</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
        
    <xsl:template name="writeFiles">
        <xsl:param name="filename"/>
 
<!-- This section opens a file for the Project concept object and writes metadata.   -->        
<!--        <xsl:result-document href="/Users/sternb/Documents/{$filename}"> -->
            
 <!-- This is the in-line datastream content for the descriptive metadata.  -->                                                
            <eac-cpf xmlns="urn:isbn:1-931666-33-4" xmlns:eac="urn:isbn:1-931666-33-4" xmlns:xlink="http://www.w3.org/1999/xlink">
                <control>
                    <recordId><xsl:value-of select="ProjectId"/></recordId>
                    <maintenanceStatus>revised</maintenanceStatus>
                    <maintenanceAgency>
                        <agencyName></agencyName>
                    </maintenanceAgency>
                    <localControl localType="Publication Date">
                        <date><xsl:value-of select="PublishDate"></xsl:value-of></date>
                    </localControl>
                    <maintenanceHistory>
                        <maintenanceEvent>
                            <eventType>created</eventType>
                            <eventDateTime></eventDateTime>
                            <agentType>human</agentType>
                            <agent></agent>
                            <eventDescription></eventDescription>
                        </maintenanceEvent>
                    </maintenanceHistory>
                </control>
                <cpfDescription>
                    <identity>
                        <entityType>corporateBody</entityType>
                        <nameEntry localType="primary">
                            <part><xsl:value-of select="ProjectName"/> (<xsl:value-of select="ProjectId"/>)</part>
                        </nameEntry>
                        <nameEntry localType="abbreviation">
                            <part><xsl:value-of select="ProjectId"/></part>
                        </nameEntry>
                    </identity>
                    <description>
                        <existDates>
                            <dateRange>
                                <fromDate></fromDate>
                                <toDate></toDate>
                            </dateRange>
                        </existDates>
                        <place>
                            <placeEntry localType="address"></placeEntry>
                            <placeEntry localType="region"></placeEntry>
                            <xsl:element name="placeEntry">
                                <xsl:attribute name="countryCode"><xsl:value-of select="CountryCode"></xsl:value-of></xsl:attribute>
                                <xsl:attribute name="latitude"><xsl:value-of select="ProjectLatitude"></xsl:value-of></xsl:attribute>
                                <xsl:attribute name="longitude"><xsl:value-of select="ProjectLongitude"></xsl:value-of></xsl:attribute>
                            </xsl:element>                        
                        </place>
                        <functions>
                            <function>
                                <term>Project Objectives</term>
                                <descriptiveNote>
                                    <p><xsl:value-of select="ProjectObjectives"></xsl:value-of></p>
                                </descriptiveNote>
                            </function>
                            <function>
                                <term>Project Data Access and Use Constraints</term>
                                <descriptiveNote>
                                    <p><xsl:value-of select="ProjectDataAccessandUseConstraints"></xsl:value-of></p>
                                </descriptiveNote>
                            </function>
                        </functions>
                    </description>
                    <relations>

                       <xsl:for-each select="ProjectOwners/ProjectOwner">
                                <cpfRelation>
                                <relationEntry><xsl:value-of select="ProjectOwnerName"></xsl:value-of></relationEntry>
                                <placeEntry localType="email address"><xsl:value-of select="ProjectOwnerEmail"></xsl:value-of></placeEntry>
                                <descriptiveNote>
                                    <p>Project Owner</p>
                                </descriptiveNote>
                                </cpfRelation>
                        </xsl:for-each>
                        
                        <xsl:for-each select="PrincipalInvestigators/PrincipalInvestigator">
                            <cpfRelation>
                                <relationEntry><xsl:value-of select="PrincipalInvestigatorName"></xsl:value-of></relationEntry>
                                <placeEntry localType="email address"><xsl:value-of select="PrincipalInvestigatorEmail"></xsl:value-of></placeEntry>
                                <descriptiveNote>
                                    <p>Principal Investigator</p>
                                </descriptiveNote>
                            </cpfRelation>
                        </xsl:for-each>
                        
                        <xsl:for-each select="ProjectContacts/ProjectContact">
                            <cpfRelation>
                                <relationEntry><xsl:value-of select="ProjectContactName"></xsl:value-of></relationEntry>
                                <placeEntry localType="email address"><xsl:value-of select="ProjectContactEmail"></xsl:value-of></placeEntry>
                                <descriptiveNote>
                                    <p>Project Contact</p>
                                </descriptiveNote>
                            </cpfRelation>
                        </xsl:for-each>
                    </relations>
                </cpfDescription>
            </eac-cpf>
              
       <!-- </xsl:result-document> -->
        
    </xsl:template>
</xsl:stylesheet>