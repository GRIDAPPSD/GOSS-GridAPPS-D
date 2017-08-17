/*******************************************************************************
 * Copyright © 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity 
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the 
 * Software) to redistribute and use the Software in source and binary forms, with or without modification. 
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any 
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. 
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any 
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these 
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for 
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process 
 * disclosed, or represents that its use would not infringe privately owned rights.
 * 
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, 
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United 
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed 
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 * 
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the 
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.data.handlers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class CIMDataSQLtoRDF {
	public static final String CIM_NS = "http://iec.ch/TC57/2012/CIM-schema-cim16#";
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String CIM_PREFIX = "cim:";
	public static final String RDF_PREFIX = "rdf:";
	public static final String ID_ATTRIBUTE = "ID";
	public static final String RESOURCE_ATTRIBUTE = "resource";
	static HashMap<String, String> fieldNameMap = new HashMap<String, String>();
	static HashMap<String, String> referenceMap = new HashMap<String, String>();
//	static List<String> typesWithParent = new ArrayList<String>();
	static List<String> booleanColumns = new ArrayList<String>();
	static HashMap<String, String> joinFields = new HashMap<String, String>();
    private Logger log = LoggerFactory.getLogger(getClass());

	
	public static void main(String[] args) {
		if(args.length<4){
			System.out.println("Usage: <output file> <database url> <database user> <database password>");
			System.exit(1);
		}
		Connection conn = null;
		OutputStream out = null;
		String dataLocation = args[0];
		String db = args[1];
		String user = args[2];
		String pw = args[3];
		
		
		try {
			conn = DriverManager.getConnection(db, user, pw);
			CIMDataSQLtoRDF parse = new CIMDataSQLtoRDF();
			out = new FileOutputStream(dataLocation);
			parse.outputModel("ieee8500", new BufferedWriter(new OutputStreamWriter(out)), conn);
//			parse.outputModel("Feeder1", new BufferedWriter(new OutputStreamWriter(out)), conn);
//			parse.outputModel("ieee13nodeckt", new BufferedWriter(new OutputStreamWriter(out)), conn);
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(conn!=null)
					conn.close();
				if(out!=null){
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
		
		
		
	public void outputModel(String lineName, BufferedWriter out, Connection conn) throws IOException, SQLException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			doc.setXmlStandalone(true);
			doc.setDocumentURI(RDF_NS);
			Element rootElement = doc.createElementNS(RDF_NS, RDF_PREFIX+"RDF");
	        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cim", CIM_NS);
	        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rdf", RDF_NS);
	 
			doc.appendChild(rootElement);
			
			ArrayList<String> notFound = new ArrayList<String>();
		
			//All components that belong in the same model as the line
			String lineLookup = "SELECT distinct mc2.componentMRID, mc2.tableName, mc2.id"
						+ "	FROM ModelComponents mc1, Line l, ModelComponents mc2"
						+ " where mc1.componentMRID=l.mRID and l.name='"+lineName+"' and mc1.mRID=mc2.mRID order by mc2.id";
			log.debug("Querying line components: "+lineLookup);
			
			
			Statement lookupStmt = conn.createStatement();
			ResultSet results = lookupStmt.executeQuery(lineLookup);
			int count = 0;
			//For each result
			while(results.next()){
				
				count++;
				String tableName = results.getString("tableName");
				String mrid = results.getString("componentMRID");
//				System.out.println(tableName+" "+mrid+"  "+count);
				Element next = doc.createElementNS(CIM_NS, CIM_PREFIX+tableName);
				next.setAttributeNS(RDF_NS, RDF_PREFIX+ID_ATTRIBUTE, mrid);
				rootElement.appendChild(next);
//				out.write(results.getString("componentMRID")+" "+results.getString("tableName"));
//				out.newLine();
				String tableLookup = "SELECT * from "+tableName+" where mRID='"+mrid+"'";
				Statement tableLookupStmt = conn.createStatement();
				ResultSet tableResults = tableLookupStmt.executeQuery(tableLookup);
				tableResults.next();
				ResultSetMetaData metadata = tableResults.getMetaData();
				for(int i=1;i<=metadata.getColumnCount();i++){
					//create element with the table name and rdf:ID of the mRID
					//add element for each field that it has content for, do a lookup by name and table name to see what it should be written out as
					String column = metadata.getColumnName(i);
					String fullColumn = tableName+"."+column;
//					System.out.println(fullColumn);
					
					
					String value = tableResults.getString(i);
					if(value!=null){
						if(fieldNameMap.containsKey(tableName+"."+column)){
							fullColumn = fieldNameMap.get(tableName+"."+column);
						} else {
							if(!notFound.contains(fullColumn)){
								notFound.add(fullColumn);
							}
						}
						
						
						if(!column.equals("Parent") &&!column.equals("SwtParent") && !column.equals("PowerSystemResource")){
							Element field = doc.createElementNS(CIM_NS, CIM_PREFIX+fullColumn);
							if(referenceMap.containsKey(column)){
								field.setAttributeNS(RDF_NS, RDF_PREFIX+RESOURCE_ATTRIBUTE, CIM_NS+referenceMap.get(column)+"."+value);
							} else if(value.startsWith("_") && !column.equals("mRID")&& !column.equals("name")){
								field.setAttributeNS(RDF_NS, RDF_PREFIX+RESOURCE_ATTRIBUTE, "#"+value);
							} else {
								if(booleanColumns.contains(fullColumn)){
									if("1".equals(value)){
										field.setTextContent("true");
									} else if("0".equals(value)){
										field.setTextContent("false");
									} else {
										Boolean b = new Boolean(value);
										field.setTextContent(b.toString());
									}
								} else {
									field.setTextContent(value);
								}
							}
							next.appendChild(field);
						}
					}
						
					//the table is linked to a join table
					if(joinFields.containsKey(tableName)){
						String joinField = joinFields.get(tableName);
						String lookupTable = tableName+"_"+joinField+"Join";
						String joinLookup = "SELECT distinct "+joinField+" from "+lookupTable+" where "+
								tableName+"='"+mrid+"'";
					
						Statement joinlookupStmt = conn.createStatement();
						ResultSet joinresults = joinlookupStmt.executeQuery(joinLookup);
						//For each joinresult
						while(joinresults.next()){
							String joinvalue = joinresults.getString(joinField);
							Element field = doc.createElementNS(CIM_NS, CIM_PREFIX+tableName+"."+joinField);
							field.setAttributeNS(RDF_NS, RDF_PREFIX+RESOURCE_ATTRIBUTE, "#"+joinvalue);
							next.appendChild(field);
						}
					}
				}
			}
			log.debug(count+" components added to output model");
			
			// Use a Transformer for output
		    TransformerFactory tFactory = TransformerFactory.newInstance();
		    Transformer transformer = tFactory.newTransformer();
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    

		    log.debug("Writing output model to file");
			DOMSource source = new DOMSource(doc);
		    StreamResult result = new StreamResult(out);
		    transformer.transform(source, result);
		    log.debug("Output model transformation complete");
		    
		}catch(ParserConfigurationException e){
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		out.flush();
	}
		
	
	static {
		fieldNameMap.put("IEC61970CIMVersion.version", "IEC61970CIMVersion.version");
		fieldNameMap.put("IEC61970CIMVersion.date", "IEC61970CIMVersion.date");
		fieldNameMap.put("CoordinateSystem.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("CoordinateSystem.name", "IdentifiedObject.name");
		fieldNameMap.put("GeographicalRegion.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("GeographicalRegion.name", "IdentifiedObject.name");
		fieldNameMap.put("SubGeographicalRegion.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("SubGeographicalRegion.name", "IdentifiedObject.name");
		fieldNameMap.put("Location.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("Location.name", "IdentifiedObject.name");
		fieldNameMap.put("Line.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("Line.name", "IdentifiedObject.name");
		fieldNameMap.put("Line.Location", "PowerSystemResource.Location");
		fieldNameMap.put("TopologicalNode.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TopologicalNode.name", "IdentifiedObject.name");
		fieldNameMap.put("ConnectivityNode.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("ConnectivityNode.name", "IdentifiedObject.name");		
		fieldNameMap.put("TopologicalIsland.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TopologicalIsland.name", "IdentifiedObject.name");
		fieldNameMap.put("BaseVoltage.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("BaseVoltage.name", "IdentifiedObject.name");
		fieldNameMap.put("EnergySource.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("EnergySource.name", "IdentifiedObject.name");
		fieldNameMap.put("EnergySource.BaseVoltage", "ConductingEquipment.BaseVoltage");
		fieldNameMap.put("EnergySource.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("EnergySource.Location", "PowerSystemResource.Location");
		fieldNameMap.put("Terminal.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("Terminal.name", "IdentifiedObject.name");
		fieldNameMap.put("PositionPoint.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PositionPoint.name", "IdentifiedObject.name");
		fieldNameMap.put("LinearShuntCompensator.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("LinearShuntCompensator.name", "IdentifiedObject.name");
		fieldNameMap.put("LinearShuntCompensator.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("LinearShuntCompensator.BaseVoltage", "ConductingEquipment.BaseVoltage");
		fieldNameMap.put("LinearShuntCompensator.nomU", "ShuntCompensator.nomU");
		fieldNameMap.put("LinearShuntCompensator.aVRDelay", "ShuntCompensator.aVRDelay");
		fieldNameMap.put("LinearShuntCompensator.phaseConnection", "ShuntCompensator.phaseConnection");
		fieldNameMap.put("LinearShuntCompensator.grounded", "ShuntCompensator.grounded");
		fieldNameMap.put("LinearShuntCompensator.normalSections", "ShuntCompensator.normalSections");
		fieldNameMap.put("LinearShuntCompensator.maximumSections", "ShuntCompensator.maximumSections");
		fieldNameMap.put("LinearShuntCompensator.Location", "PowerSystemResource.Location");
		fieldNameMap.put("LinearShuntCompensatorPhase.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("LinearShuntCompensatorPhase.name", "IdentifiedObject.name");
		fieldNameMap.put("LinearShuntCompensatorPhase.phase", "ShuntCompensatorPhase.phase");
		fieldNameMap.put("LinearShuntCompensatorPhase.normalSections", "ShuntCompensatorPhase.normalSections");
		fieldNameMap.put("LinearShuntCompensatorPhase.maximumSections", "ShuntCompensatorPhase.maximumSections");
		fieldNameMap.put("LinearShuntCompensatorPhase.ShuntCompensator", "ShuntCompensatorPhase.ShuntCompensator");
		fieldNameMap.put("LinearShuntCompensatorPhase.Location", "PowerSystemResource.Location");
		fieldNameMap.put("RegulatingControl.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("RegulatingControl.name", "IdentifiedObject.name");
		fieldNameMap.put("RegulatingControl.Location", "PowerSystemResource.Location");
		fieldNameMap.put("PowerTransformerInfo.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PowerTransformerInfo.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerTankInfo.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerTankInfo.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerEndInfo.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerEndInfo.name", "IdentifiedObject.name");
		fieldNameMap.put("NoLoadTest.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("NoLoadTest.name", "IdentifiedObject.name");
		fieldNameMap.put("NoLoadTest.basePower", "TransformerTest.basePower");
		fieldNameMap.put("NoLoadTest.temperature", "TransformerTest.temperature");
		fieldNameMap.put("ShortCircuitTest.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("ShortCircuitTest.name", "IdentifiedObject.name");
		fieldNameMap.put("ShortCircuitTest.basePower", "TransformerTest.basePower");
		fieldNameMap.put("ShortCircuitTest.temperature", "TransformerTest.temperature");
		fieldNameMap.put("Asset.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("Asset.name", "IdentifiedObject.name");
		fieldNameMap.put("TapChangerInfo.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TapChangerInfo.name", "IdentifiedObject.name");
		fieldNameMap.put("TapChangerControl.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TapChangerControl.name", "IdentifiedObject.name");
		fieldNameMap.put("TapChangerControl.mode", "RegulatingControl.mode");
		fieldNameMap.put("TapChangerControl.Terminal", "RegulatingControl.Terminal");
		fieldNameMap.put("TapChangerControl.monitoredPhase", "RegulatingControl.monitoredPhase");
		fieldNameMap.put("TapChangerControl.enabled", "RegulatingControl.enabled");
		fieldNameMap.put("TapChangerControl.discrete", "RegulatingControl.discrete");
		fieldNameMap.put("TapChangerControl.targetValue", "RegulatingControl.targetValue");
		fieldNameMap.put("TapChangerControl.targetDeadband", "RegulatingControl.targetDeadband");
		fieldNameMap.put("TapChangerControl.Location", "PowerSystemResource.Location");
		fieldNameMap.put("RatioTapChanger.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("RatioTapChanger.name", "IdentifiedObject.name");
		fieldNameMap.put("RatioTapChanger.TapChangerControl", "TapChanger.TapChangerControl");
		fieldNameMap.put("RatioTapChanger.highStep", "TapChanger.highStep");
		fieldNameMap.put("RatioTapChanger.lowStep", "TapChanger.lowStep");
		fieldNameMap.put("RatioTapChanger.neutralStep", "TapChanger.neutralStep");
		fieldNameMap.put("RatioTapChanger.normalStep", "TapChanger.normalStep");
		fieldNameMap.put("RatioTapChanger.neutralU", "TapChanger.neutralU");
		fieldNameMap.put("RatioTapChanger.initialDelay", "TapChanger.initialDelay");
		fieldNameMap.put("RatioTapChanger.subsequentDelay", "TapChanger.subsequentDelay");
		fieldNameMap.put("RatioTapChanger.ltcFlag", "TapChanger.ltcFlag");
		fieldNameMap.put("RatioTapChanger.controlEnabled", "TapChanger.controlEnabled");
		fieldNameMap.put("RatioTapChanger.step", "TapChanger.step");
		fieldNameMap.put("RatioTapChanger.Location", "PowerSystemResource.Location");
		fieldNameMap.put("ACLineSegment.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("ACLineSegment.name", "IdentifiedObject.name");
		fieldNameMap.put("ACLineSegment.EquipmentContainer", "Equipment.EquipmentContaine");
		fieldNameMap.put("ACLineSegment.BaseVoltage", "ConductingEquipment.BaseVoltage");
		fieldNameMap.put("ACLineSegment.Location", "PowerSystemResource.Location");
		fieldNameMap.put("ACLineSegment.length", "Conductor.length");
		fieldNameMap.put("ACLineSegment.Location", "PowerSystemResource.Location");
		fieldNameMap.put("ACLineSegmentPhase.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("ACLineSegmentPhase.name", "IdentifiedObject.name");
		fieldNameMap.put("ACLineSegmentPhase.Location", "PowerSystemResource.Location");
		fieldNameMap.put("LoadBreakSwitch.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("LoadBreakSwitch.name", "IdentifiedObject.name");
		fieldNameMap.put("LoadBreakSwitch.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("LoadBreakSwitch.BaseVoltage", "ConductingEquipment.BaseVoltage");
		fieldNameMap.put("LoadBreakSwitch.breakingCapacity", "ProtectedSwitch.breakingCapacity");
		fieldNameMap.put("LoadBreakSwitch.ratedCurrent", "Switch.ratedCurrent");
		fieldNameMap.put("LoadBreakSwitch.normalOpen", "Switch.normalOpen");
		fieldNameMap.put("LoadBreakSwitch.open", "Switch.open");
		fieldNameMap.put("LoadBreakSwitch.retained", "Switch.retained");
		fieldNameMap.put("LoadBreakSwitch.Location", "PowerSystemResource.Location");
		fieldNameMap.put("SwitchPhase.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("SwitchPhase.name", "IdentifiedObject.name");
		fieldNameMap.put("SwitchPhase.Location", "PowerSystemResource.Location");
		fieldNameMap.put("LoadResponseCharacteristic.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("LoadResponseCharacteristic.name", "IdentifiedObject.name");
		fieldNameMap.put("EnergyConsumer.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("EnergyConsumer.name", "IdentifiedObject.name");
		fieldNameMap.put("EnergyConsumer.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("EnergyConsumer.BaseVoltage", "ConductingEquipment.BaseVoltage");
		fieldNameMap.put("EnergyConsumer.Location", "PowerSystemResource.Location");
		fieldNameMap.put("EnergyConsumerPhase.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("EnergyConsumerPhase.name", "IdentifiedObject.name");
		fieldNameMap.put("EnergyConsumerPhase.Location", "PowerSystemResource.Location");
		fieldNameMap.put("PerLengthPhaseImpedance.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PerLengthPhaseImpedance.name", "IdentifiedObject.name");
		fieldNameMap.put("PerLengthSequenceImpedance.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PerLengthSequenceImpedance.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerCoreAdmittance.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerCoreAdmittance.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerMeshImpedance.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerMeshImpedance.name", "IdentifiedObject.name");
		fieldNameMap.put("PowerTransformerEnd.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PowerTransformerEnd.name", "IdentifiedObject.name");
		fieldNameMap.put("PowerTransformerEnd.endNumber", "TransformerEnd.endNumber");
		fieldNameMap.put("PowerTransformerEnd.grounded", "TransformerEnd.grounded");
		fieldNameMap.put("PowerTransformerEnd.Terminal", "TransformerEnd.Terminal");
		fieldNameMap.put("PowerTransformerEnd.BaseVoltage", "TransformerEnd.BaseVoltage");
		fieldNameMap.put("TransformerTank.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerTank.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerTank.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("TransformerTank.Location", "PowerSystemResource.Location");
		fieldNameMap.put("TransformerTankEnd.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("TransformerTankEnd.name", "IdentifiedObject.name");
		fieldNameMap.put("TransformerTankEnd.endNumber", "TransformerEnd.endNumber");
		fieldNameMap.put("TransformerTankEnd.grounded", "TransformerEnd.grounded");
		fieldNameMap.put("TransformerTankEnd.Terminal", "TransformerEnd.Terminal");
		fieldNameMap.put("TransformerTankEnd.BaseVoltage", "TransformerEnd.BaseVoltage");
		fieldNameMap.put("PowerTransformer.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PowerTransformer.name", "IdentifiedObject.name");
		fieldNameMap.put("PowerTransformer.EquipmentContainer", "Equipment.EquipmentContainer");
		fieldNameMap.put("PowerTransformer.Location", "PowerSystemResource.Location");
		fieldNameMap.put("PhaseImpedanceData.mRID", "IdentifiedObject.mRID");
		fieldNameMap.put("PhaseImpedanceData.name", "IdentifiedObject.name");
		
		referenceMap.put("phaseConnection","PhaseShuntConnectionKind");
		referenceMap.put("phase","SinglePhaseKind");
		referenceMap.put("phaseSide1","SinglePhaseKind");
		referenceMap.put("phaseSide2","SinglePhaseKind");
		referenceMap.put("connectionKind","WindingConnection");
		referenceMap.put("phases","PhaseCode");
		referenceMap.put("mode","RegulatingControlModeKind");
		referenceMap.put("monitoredPhase","PhaseCode");
		referenceMap.put("tculControlMode","TransformerControlMode");
//		
//		typesWithParent.add("ConcentricNeutralCableInfo");
//		typesWithParent.add("TapeShieldCableInfo");
//		typesWithParent.add("OverheadWireInfo");
//		typesWithParent.add("WireSpacingInfo");
//		typesWithParent.add("TapChangerInfo");
//		typesWithParent.add("TransformerTankInfo");
//		typesWithParent.add("PowerTransformerEnd");
//		typesWithParent.add("TransformerTankEnd");
//		typesWithParent.add("PerLengthPhaseImpedance");
//		typesWithParent.add("PerLengthSequenceImpedance");
//		typesWithParent.add("ACLineSegment");
//		typesWithParent.add("EnergySource");
//		typesWithParent.add("EnergyConsumer");
//		typesWithParent.add("LinearShuntCompensator");
//		typesWithParent.add("PowerTransformer");
//		typesWithParent.add("Breaker");
//		typesWithParent.add("Recloser");
//		typesWithParent.add("LoadBreakSwitch");
//		typesWithParent.add("Sectionaliser");
//		typesWithParent.add("Jumper");
//		typesWithParent.add("Fuse");
//		typesWithParent.add("Disconnector");
		booleanColumns.add("ShuntCompensator.grounded");
		booleanColumns.add("TransformerEnd.grounded");
		booleanColumns.add("EnergyConsumer.grounded");
		booleanColumns.add("TapChangerControl.enabled");
		booleanColumns.add("TapChangerControl.discrete");
		booleanColumns.add("TapChangerControl.lineDropCompensation");
		booleanColumns.add("cim:RegulatingControl.enabled.enabled");
		booleanColumns.add("cim:RegulatingControl.enabled.discrete");
		booleanColumns.add("cim:RegulatingControl.enabled.lineDropCompensation");
		booleanColumns.add("RatioTapChanger.ltcFlag");
		booleanColumns.add("RatioTapChanger.controlEnabled");
		booleanColumns.add("TapChanger.ltcFlag");
		booleanColumns.add("TapChanger.controlEnabled");		
		booleanColumns.add("LoadBreakSwitch.normalOpen");
		booleanColumns.add("Switch.normalOpen");
		booleanColumns.add("LoadBreakSwitch.open");
		booleanColumns.add("Switch.open");
		booleanColumns.add("LoadBreakSwitch.retained");
		booleanColumns.add("Switch.retained");
		booleanColumns.add("LoadResponseCharacteristic.exponentModel");
	
		joinFields.put("ShortCircuitTest","GroundedEnds");
		joinFields.put("Asset","PowerSystemResources");
		
	}

}
