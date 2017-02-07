<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2015-2016 Smithsonian Institution.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
  ~ use this file except in compliance with the License.You may obtain a copy of
  ~ the License at: http://www.apache.org/licenses/
  ~
  ~ This software and accompanying documentation is supplied without
  ~ warranty of any kind. The copyright holder and the Smithsonian Institution:
  ~ (1) expressly disclaim any warranties, express or implied, including but not
  ~ limited to any implied warranties of merchantability, fitness for a
  ~ particular purpose, title or non-infringement; (2) do not assume any legal
  ~ liability or responsibility for the accuracy, completeness, or usefulness of
  ~ the software; (3) do not represent that use of the software would not
  ~ infringe privately owned rights; (4) do not warrant that the software
  ~ is error-free or will be maintained, supported, updated or enhanced;
  ~ (5) will not be liable for any indirect, incidental, consequential special
  ~ or punitive damages of any kind or nature, including but not limited to lost
  ~ profits or loss of data, on any basis arising from contract, tort or
  ~ otherwise, even if any of the parties has been warned of the possibility of
  ~ such loss or damage.
  ~
  ~ This distribution includes several third-party libraries, each with their own
  ~ license terms. For a complete copy of all copyright and license terms, including
  ~ those of third-party libraries, please see the product release notes.
  -->

<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <!--<xsl:output name="content" method="xml"/>-->

    <!-- This stylesheet processes an XML file. Each project element will be mapped to a Project object in the repository.

 -->

    <xsl:template match="/">
        <xsl:apply-templates select="/Fields"/>
    </xsl:template>

    <xsl:template match="Fields">
        <xsl:call-template name="writeFiles">
            <xsl:with-param name="filename"><xsl:value-of select="Field[@Name='MCI_ID']"/><xsl:text>_</xsl:text><xsl:value-of select="Field[@Name='Title']"/>.xml</xsl:with-param>

        </xsl:call-template>
    </xsl:template>

    <xsl:template name="writeFiles">
        <xsl:param name="filename"/>

        <!-- This section opens a file for the Project concept object and writes metadata.   -->
        <!--<xsl:result-document href="/Users/sternb/Documents/{$filename}">-->

            <!-- This is the in-line datastream content for the descriptive metadata.  -->
            <SIdoraConcept>
                <conceptType>Project</conceptType>
                <primaryTitle>
                    <titleText><xsl:value-of select="Field[@Name='MCI_ID']"/><xsl:text> </xsl:text><xsl:value-of select="Field[@Name='Title']"/></titleText>
                </primaryTitle>
                <altTitle type="">
                    <titleText></titleText>
                </altTitle>
                <summaryDescription><xsl:value-of select="Field[@Name='Project_x0020_Summary']/div/p"/></summaryDescription>
                <xsl:element name="keyword">
                    <xsl:attribute name="type"><xsl:value-of select="Field[@Name='KeywordType1']"/></xsl:attribute>
                    <value><xsl:value-of select="Field[@Name='Keyword1']"/></value>
                </xsl:element>
                <xsl:element name="keyword">
                    <xsl:attribute name="type"><xsl:value-of select="Field[@Name='KeywordType2']"/></xsl:attribute>
                    <value><xsl:value-of select="Field[@Name='Keyword2']"/></value>
                </xsl:element>
                <xsl:element name="keyword">
                    <xsl:attribute name="type"><xsl:value-of select="Field[@Name='KeywordType3']"/></xsl:attribute>
                    <value><xsl:value-of select="Field[@Name='Keyword3']"/></value>
                </xsl:element>
                <identifier type="MCI #">
                    <value><xsl:value-of select="Field[@Name='MCI_ID']"/></value>
                </identifier>
                <identifier type="Accession #">
                    <value><xsl:value-of select="Field[@Name='Accession_x0020__x0023_']"/></value>
                </identifier>
                <identifier type="Case File #">
                    <value><xsl:value-of select="Field[@Name='Case_x0020_File_x0020__x0023_']"/></value>
                </identifier>
                <identifier type="IRMS #">
                    <value><xsl:value-of select="Field[@Name='IRMS_x0020__x0023_']"/></value>
                </identifier>
                <identifier type="Object Title">
                    <value><xsl:value-of select="Field[@Name='ObjectTitle']"/></value>
                </identifier>
                <identifier type="Other ID #">
                    <value><xsl:value-of select="Field[@Name='IDNum']"/></value>
                </identifier>
                <identifier type="Temporary Request #">
                    <value><xsl:value-of select="Field[@Name='TemporaryRequest']"/></value>
                </identifier>
                <rights type="Project Data Access and Use Constraints">
                    <value><xsl:value-of select="Field[@Name='Confidential']"/></value>
                    <note></note>
                </rights>
                <relationship type="Related MCI Project #">
                    <value><xsl:value-of select="Field[@Name='Related_x0020__x0023_']"/></value>
                    <note></note>
                </relationship>
                <note class="Project Notes" type="Project Description"><xsl:value-of select="Field[@Name='Request']"/></note>
                <note class="Project Notes" type="Project Objectives"><xsl:value-of select="Field[@Name='Purpose']"/></note>
                <note class="Project Notes" type="Project Design"><xsl:value-of select="Field[@Name='ConservationGroup']"/></note>
                <note class="General Notes" type="General Notes"><xsl:value-of select="Field[@Name='Notes']/div/p"/></note>
                <note class="General Notes" type="Notes to Staff"><xsl:value-of select="Field[@Name='StaffNotes']/div/p"/></note>
                <note class="General Notes" type="Object Description"><xsl:value-of select="Field[@Name='Object_x0020_Description']/div/p"/></note>
                <note class="General Notes" type="HC Comments"><xsl:value-of select="Field[@Name='HC_x0020_Comments']/div"/></note>
                <note class="General Notes" type="HTS Comments"><xsl:value-of select="Field[@Name='HTS_x0020_Comments']/div"/></note>
                <note class="General Notes" type="Deputy Comments"><xsl:value-of select="Field[@Name='Deputy_x0020_Comments']/div"/></note>
                <note class="General Notes" type="Director Comments"><xsl:value-of select="Field[@Name='Director_x0020_Comments']/div"/></note>
                <timePeriod type="Date of Request">
                    <intervalname><xsl:value-of select="Field[@Name='RequestDate']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Date of Approval">
                    <intervalname><xsl:value-of select="Field[@Name='ApprovalDate']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Project Date of Deadline">
                    <intervalname><xsl:value-of select="Field[@Name='Deadline']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Date Completed">
                    <intervalname><xsl:value-of select="Field[@Name='Date_x0020_Completed']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Project Fiscal Year">
                    <intervalname></intervalname>
                    <date type="Fiscal Year">
                        <year><xsl:value-of select="Field[@Name='FiscalYear']"/></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Object Date">
                    <intervalname><xsl:value-of select="Field[@Name='Textual_x0020_Date']"/></intervalname>
                    <date type="Object Year">
                        <year><xsl:value-of select="Field[@Name='Year']"/></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="HC Signature Date">
                    <intervalname><xsl:value-of select="Field[@Name='HC_x0020_Signature_x0020_Date']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="HTS Signature Date">
                    <intervalname><xsl:value-of select="Field[@Name='HTS_x0020_Signature_x0020_Date']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Deputy Signature Date">
                    <intervalname><xsl:value-of select="Field[@Name='Deputy_x0020_Signature_x0020_Date']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <timePeriod type="Director Signature Date">
                    <intervalname><xsl:value-of select="Field[@Name='Director_x0020_Signature_x0020_Date']"/></intervalname>
                    <date type="">
                        <year></year>
                        <month></month>
                        <day></day>
                    </date>
                    <note></note>
                </timePeriod>
                <place type="Requestor Address">
                    <geogname type="Street Address"><xsl:value-of select="Field[@Name='Requestor_x0020_Address']"/></geogname>
                    <geogname type="City"><xsl:value-of select="Field[@Name='RequestorCity']"/></geogname>
                    <geogname type="State"><xsl:value-of select="Field[@Name='RequestorState']"/></geogname>
                    <geogname type="Zip"><xsl:value-of select="Field[@Name='RequestorZIP']"/></geogname>
                    <point>
                        <lat></lat>
                        <long></long>
                    </point>
                    <timePeriod type="">
                        <intervalname></intervalname>
                        <date type="">
                            <year></year>
                            <month></month>
                            <day></day>
                        </date>
                    </timePeriod>
                    <note><xsl:text>Requestor Phone:  </xsl:text><xsl:value-of select="Field[@Name='Requestor_x0020_Phone']"/></note>
                </place>
                <place type="On Behalf Of Address">
                    <geogname type="Street Address"><xsl:value-of select="Field[@Name='NonSIAddress']"/></geogname>
                    <point>
                        <lat></lat>
                        <long></long>
                    </point>
                    <timePeriod type="">
                        <intervalname></intervalname>
                        <date type="">
                            <year></year>
                            <month></month>
                            <day></day>
                        </date>
                    </timePeriod>
                    <note><xsl:text>On Behalf Of Phone:  </xsl:text><xsl:value-of select="Field[@Name='NonSIPhone']"/></note>
                </place>
                <actor role="Folder Holder">
                    <persnameText><xsl:value-of select="substring-after(Field[@Name='Folder_x0020_Holder'],'/')"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='folderholderEmail']"/></email>
                    </contactInfo>
                </actor>
                <actor role="Requestor">
                    <persnameText><xsl:value-of select="Field[@Name='Requestor']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='Requestor_x0020_Email']"/></email>
                    </contactInfo>
                </actor>
                <actor role="Initiator">
                    <persnameText><xsl:value-of select="substring-after(Field[@Name='Initiator'],'/')"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role1']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing1']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role2']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing2']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role3']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing3']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role4']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing4']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role5']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing5']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role6']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing6']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role7']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing7']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <xsl:element name="actor">
                    <xsl:attribute name="role"><xsl:value-of select="Field[@Name='Role8']"/></xsl:attribute>
                    <persnameText><xsl:value-of select="Field[@Name='ProposedStaffing8']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </xsl:element>
                <actor role="MCI Contact">
                    <persnameText><xsl:value-of select="Field[@Name='MCI_x0020_Contact']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <actor role="On Behalf Of">
                    <persnameText><xsl:value-of select="Field[@Name='NonSIName']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='NonSIEmail']"/></email>
                    </contactInfo>
                </actor>
                <actor role="On Behalf Of">
                    <corpname><xsl:value-of select="Field[@Name='NonSIOrganization']"/></corpname>
                </actor>
                <actor role="Requesting Unit">
                    <corpname><xsl:value-of select="Field[@Name='requestingUnit']"/></corpname>
                </actor>
                <actor role="Approving Official">
                    <persnameText><xsl:value-of select="Field[@Name='Approving_x0020_Official_x0020_N']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='Approving_x0020_Official_x0020_E']"/></email>
                    </contactInfo>
                </actor>
                <actor role="Approving Unit">
                    <corpname><xsl:value-of select="Field[@Name='Approving_x0020_Unit']"/></corpname>
                </actor>
                <actor role="Principal Investigator">
                    <persnameText><xsl:value-of select="Field[@Name='Principal_x0020_Investigator']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='Principal_x0020_Investigator_x0020_E']"/></email>
                    </contactInfo>
                </actor>
                <actor role="Collaborator">
                    <persnameText><xsl:value-of select="Field[@Name='Collaborator']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='Collaborator_x0020_E']"/></email>
                    </contactInfo>
                </actor>
                <actor role="Object Curator">
                    <persnameText><xsl:value-of select="Field[@Name='Object_x0020_Curator']"/></persnameText>
                    <contactInfo>
                        <email><xsl:value-of select="Field[@Name='Object_x0020_Curator_x0020_Email']"/></email>
                    </contactInfo>
                </actor>
                <actor role="HC Approver">
                    <persnameText><xsl:value-of select="Field[@Name='HC_x0020_Approver']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <actor role="HTS Approver">
                    <persnameText><xsl:value-of select="Field[@Name='HTS_x0020_Approver']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <actor role="Deputy Approver">
                    <persnameText><xsl:value-of select="Field[@Name='Deputy_x0020_Approver']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <actor role="Director Approver">
                    <persnameText><xsl:value-of select="Field[@Name='Director_x0020_Approver']"/></persnameText>
                    <contactInfo>
                        <email></email>
                    </contactInfo>
                </actor>
                <context type="Object Artist or Manufacturer">
                    <value><xsl:value-of select="Field[@Name='Object_x0020_Artist']"/></value>
                    <note></note>
                </context>
                <context type="Object Owner">
                    <value><xsl:value-of select="Field[@Name='Object_x0020_Owner']"/></value>
                    <note></note>
                </context>
                <context type="Geographic or Cultural Provenience">
                    <value><xsl:value-of select="Field[@Name='Cultural_x0020_Provenience']"/></value>
                    <note>1asdf</note>
                </context>
                <method type="Sample or Material">
                    <p><xsl:value-of select="Field[@Name='SampleMaterial']"/></p>
                    <note></note>
                </method>
                <method type="Special Equipment">
                    <p><xsl:value-of select="Field[@Name='Special_x0020_Equipment']"/></p>
                    <note></note>
                </method>
                <technique type="Materials or Techniques of Manufacturer">
                    <value><xsl:value-of select="Field[@Name='Techniques']/div/p"/></value>
                    <note></note>
                </technique>
                <measurements>
                    <measurementText><xsl:value-of select="Field[@Name='Dimensions']"/></measurementText>
                    <xsl:element name="measurement">
                        <xsl:attribute name="type"></xsl:attribute>
                        <xsl:attribute name="value"></xsl:attribute>
                        <xsl:attribute name="units"></xsl:attribute>
                    </xsl:element>
                    <timePeriod type="">
                        <intervalname/>
                        <date type="">
                            <year/>
                            <month/>
                            <day/>
                        </date>
                    </timePeriod>
                    <accuracy/>
                    <note/>
                </measurements>
                <measurements>
                    <measurementText></measurementText>
                    <xsl:element name="measurement">
                        <xsl:attribute name="type"><xsl:text>Number of Objects</xsl:text></xsl:attribute>
                        <xsl:attribute name="value"><xsl:value-of select="Field[@Name='ObjectNum']"/></xsl:attribute>
                        <xsl:attribute name="units"><xsl:text>Objects</xsl:text></xsl:attribute>
                    </xsl:element>
                    <timePeriod type="">
                        <intervalname/>
                        <date type="">
                            <year/>
                            <month/>
                            <day/>
                        </date>
                    </timePeriod>
                    <accuracy/>
                    <note/>
                </measurements>
            </SIdoraConcept>

        <!--</xsl:result-document>-->

    </xsl:template>
</xsl:stylesheet>