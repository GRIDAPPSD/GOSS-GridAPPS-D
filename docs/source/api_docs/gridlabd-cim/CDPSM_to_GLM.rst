.. java:import:: java.io FileNotFoundException

.. java:import:: java.io InputStream

.. java:import:: java.io InputStreamReader

.. java:import:: java.io PrintWriter

.. java:import:: java.io UnsupportedEncodingException

.. java:import:: java.util HashMap

.. java:import:: org.apache.jena.ontology OntModelSpec

.. java:import:: org.apache.jena.ontology OntResource

.. java:import:: org.apache.jena.query Query

.. java:import:: org.apache.jena.query QueryExecution

.. java:import:: org.apache.jena.query QueryExecutionFactory

.. java:import:: org.apache.jena.query QueryFactory

.. java:import:: org.apache.jena.query QuerySolution

.. java:import:: org.apache.jena.query ResultSet

.. java:import:: org.apache.jena.rdf.model Model

.. java:import:: org.apache.jena.rdf.model ModelFactory

.. java:import:: org.apache.jena.rdf.model Property

.. java:import:: org.apache.jena.rdf.model ResIterator

.. java:import:: org.apache.jena.rdf.model Resource

.. java:import:: org.apache.jena.util FileManager

.. java:import:: org.apache.commons.math3.complex Complex

CDPSM_to_GLM
============

.. java:package:: gov.pnnl.gridlabd.cim
   :noindex:

.. java:type:: public class CDPSM_to_GLM

   This class converts CIM (IEC 61968) RDF to GridLAB-D format

   The general pattern is to retrieve iterators on the different types of objects (e.g. ACLineSegment) through simple SPARQL queries. Usually these iterators include just the mrID, or the mrID and name. Then Jena RDF model and resource functions are used to pull other properties from iterated objects, writing GridLAB-D input along the way. A future version will rely more heavily on SPARQL queries to do the selection and filtering, as the preferred pattern for developers working with CIM. In existing code, the EnergySource most closely follows this new preferred pattern.

   Invoke as a console-mode program

   :author: Tom McDermott

   **See also:** :java:ref:`CDPSM_to_GLM.main`, \ `CIM User Group <http://www.ucaiug.org/default.aspx>`_\, <a
            href="https://github.com/GRIDAPPSD/Powergrid-Models/blob/temcdrm/CIM/CDPSM_RC1.docx">CIM
            Profile and Queries for Feeder Modeling in GridLAB-D</a>, \ `GridLAB-D <http://www.gridlabd.org>`_\

Fields
------
baseURI
^^^^^^^

.. java:field:: static final String baseURI
   :outertype: CDPSM_to_GLM

   identifies gridlabd

mapNodes
^^^^^^^^

.. java:field:: static HashMap<String, GldNode> mapNodes
   :outertype: CDPSM_to_GLM

   to look up nodes by name

mapSpacings
^^^^^^^^^^^

.. java:field:: static HashMap<String, SpacingCount> mapSpacings
   :outertype: CDPSM_to_GLM

   to look up line spacings by name

neg120
^^^^^^

.. java:field:: static final Complex neg120
   :outertype: CDPSM_to_GLM

   Rotates a phasor -120 degrees by multiplication

nsCIM
^^^^^

.. java:field:: static final String nsCIM
   :outertype: CDPSM_to_GLM

   namespace for CIM; should match the CIM version used to generate the RDF

nsRDF
^^^^^

.. java:field:: static final String nsRDF
   :outertype: CDPSM_to_GLM

   namespace for RDF

pos120
^^^^^^

.. java:field:: static final Complex pos120
   :outertype: CDPSM_to_GLM

   Rotates a phasor +120 degrees by multiplication

Methods
-------
AccumulateLoads
^^^^^^^^^^^^^^^

.. java:method:: static boolean AccumulateLoads(GldNode nd, String phs, double pL, double qL, double Pv, double Qv, double Pz, double Pi, double Pp, double Qz, double Qi, double Qp)
   :outertype: CDPSM_to_GLM

   Distributes a total load (pL+jqL) among the phases (phs) present on GridLAB-D node (nd) from a CIM LoadResponseCharacteristic: Pv and Qv are voltage exponents, Pz and Qz are constant-impedance percentages, Pi and Qi are constant-current percentages, Pp and Qp are constant-power percentages

Bus_ShuntPhases
^^^^^^^^^^^^^^^

.. java:method:: static String Bus_ShuntPhases(String phs, String conn)
   :outertype: CDPSM_to_GLM

   appends N or D for GridLAB-D loads and capacitors, based on wye or delta connection

CFormat
^^^^^^^

.. java:method:: static String CFormat(Complex c)
   :outertype: CDPSM_to_GLM

   Formats a complex number, c, for GridLAB-D input files with 'j' at the end

Count_Phases
^^^^^^^^^^^^

.. java:method:: static int Count_Phases(String phs)
   :outertype: CDPSM_to_GLM

   from the phase string, determine how many are present, but ignore D, N and S

FindBaseVoltage
^^^^^^^^^^^^^^^

.. java:method:: static double FindBaseVoltage(Resource res, Property ptEquip, Property ptEqBaseV, Property ptLevBaseV, Property ptBaseNomV)
   :outertype: CDPSM_to_GLM

   Returns the nominal voltage for ptEquip, from either its own (ptEqBaseV) or container's (ptLevBaseV) base voltage

   For example, capacitors and transformer ends have their own base voltage, but line segments don't

   When found, ptBaseNomV references the nominal voltage value from the base voltage

FindConductorAmps
^^^^^^^^^^^^^^^^^

.. java:method:: static String FindConductorAmps(Model mdl, Resource res, Property ptDataSheet, Property ptAmps)
   :outertype: CDPSM_to_GLM

   needs to return the current rating for a line segment 'res' that has associated WireInfo at 'ptDataSheet', which in turn has the current rating at ptAmps

   TODO - this is not implemented; emitted syntax is for OpenDSS and the function call (below, in main) needs review

FirstPhase
^^^^^^^^^^

.. java:method:: static String FirstPhase(String phs)
   :outertype: CDPSM_to_GLM

   returns the first phase found as A, B, or C

GLDCapMode
^^^^^^^^^^

.. java:method:: static String GLDCapMode(String s)
   :outertype: CDPSM_to_GLM

   translate the capacitor control mode from CIM to GridLAB-D

GLD_ID
^^^^^^

.. java:method:: static String GLD_ID(String arg)
   :outertype: CDPSM_to_GLM

   parse the GridLAB-D name from a CIM name, based on # position

GLD_Name
^^^^^^^^

.. java:method:: static String GLD_Name(String arg, boolean bus)
   :outertype: CDPSM_to_GLM

   convert a CIM name to GridLAB-D name, replacing unallowed characters and prefixing for a bus/node

GetACLineParameters
^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetACLineParameters(Model mdl, String name, Resource r, double len, double freq, String phs, PrintWriter out)
   :outertype: CDPSM_to_GLM

   for a standalone ACLineSegment with sequence parameters, return the GridLAB-D formatted and normalized phase impedance matrix

   TODO - this is always three-phase, so we don't need all 7 variations from GetSequenceLineConfigurations

GetBusName
^^^^^^^^^^

.. java:method:: static String GetBusName(Model mdl, String eq_id, int seq)
   :outertype: CDPSM_to_GLM

   finds the bus (ConnectivityNode) name for conducting equipment with mrID of eq_id seq = 1 to use the first terminal found, seq = 2 to use the second terminal found // As Terminals no longer have sequence numbers, the ordering of seq is unpredictable,* so if there are two we can get bus 1 - bus 2 or bus 2 - bus 1 *

GetBusPositionString
^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetBusPositionString(Model mdl, String id)
   :outertype: CDPSM_to_GLM

   for a bus (ConnectivityNode) 'id', search for X,Y geo coordinates based on connected Terminals and equipment

   returns in CSV format

GetCableData
^^^^^^^^^^^^

.. java:method:: static String GetCableData(Model mdl, Resource res)
   :outertype: CDPSM_to_GLM

   needs to return underground_line_conductor data in GridLAB-D format

   TODO - this is not implemented; the emitted syntax is actually for OpenDSS

GetCapControlData
^^^^^^^^^^^^^^^^^

.. java:method:: static String GetCapControlData(Model mdl, Resource rCap, Resource ctl)
   :outertype: CDPSM_to_GLM

   returns the embedded capacitor control data for a GridLAB-D capacitor object

   rCap is the CIM capacitor, and ctl is the RegulatingControl that we found attached to the capacitor

   this function still has to look up the monitored equipment

GetEquipmentType
^^^^^^^^^^^^^^^^

.. java:method:: static String GetEquipmentType(Resource r)
   :outertype: CDPSM_to_GLM

   find the type of monitored equipment for controlled capacitors, usually a line or the capacitor itself

GetGldTransformerConnection
^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetGldTransformerConnection(String[] wye, int nwdg)
   :outertype: CDPSM_to_GLM

   return the GridLAB-D winding connection from the array of CIM connectionKind per winding

   TODO: some of the returnable types aren't actually supported in GridLAB-D

GetImpedanceMatrix
^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetImpedanceMatrix(Model mdl, String name, Property ptCount, Resource r, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   returns the GridLAB-D formatted impedance matrix for a line configuration

   r is the PerLengthPhaseImpedance and ptCount should reference its conductorCount

   if (by name) it appears to be triplex and bWantSec is false, nothing will be returned

   we have to write 3 of these in the case of 1-phase or 2-phase matrices

GetLineSpacing
^^^^^^^^^^^^^^

.. java:method:: static String GetLineSpacing(Model mdl, Resource rLine)
   :outertype: CDPSM_to_GLM

   needs to return the line_spacing and wire/cncable/tscable assignments for this rLine in GridLAB-D format

   TODO - this is not implemented, the emitted syntax is actually for OpenDSS

GetMatIdx
^^^^^^^^^

.. java:method:: static int GetMatIdx(int n, int row, int col)
   :outertype: CDPSM_to_GLM

   converts the [row,col] of nxn matrix into the sequence number for CIM PerLengthPhaseImpedanceData (only valid for the lower triangle)

GetPowerTransformerData
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetPowerTransformerData(Model mdl, Resource rXf)
   :outertype: CDPSM_to_GLM

   writes the GridLAB-D formatted data for PowerTransformer rXf, which should have mesh impedance data

GetPowerTransformerTanks
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetPowerTransformerTanks(Model mdl, Resource rXf, ResIterator itTank, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   writes PowerTransformer rXf in GridLAB-D format, in the case where itTank points to a non-null set of individual tranformer tanks that are connected together in a bank

   bWantSec can be false if we don't want single-phase center-tapped transformers, which would come to this function

   GridLAB-D support for other cases is limited because only 2 windings are allowed, and phasing must be the same on both sides

GetPropValue
^^^^^^^^^^^^

.. java:method:: static String GetPropValue(Model mdl, String uri, String prop)
   :outertype: CDPSM_to_GLM

   unprotected lookup of uri.prop value, to be deprecated in favor of SafeProperty

GetRegulatorData
^^^^^^^^^^^^^^^^

.. java:method:: static String GetRegulatorData(Model mdl, Resource rXf, String name, String xfGroup, String bus1, String bus2, String phs)
   :outertype: CDPSM_to_GLM

   connects a regulator in GridLAB-D format between bus1 and bus2, with phasing 'phs' and name 'name'

   rXf is the regulating transformer and xfGroup its IEC vector group

   In CIM, a regulator consists of a transformer plus the ratio tap changer, so if such is found, call GetRegulatorData instead of just writing the transformer data. (Note: any impedance in the regulating transformer will be lost in the GridLAB-D model.)

   Should be called from PowerTransformers that have RatioTapChangers attached, so we know that lookup will succeed

   TODO: implement regulators for tank transformers

GetSequenceLineConfigurations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetSequenceLineConfigurations(String name, double sqR1, double sqX1, double sqC1, double sqR0, double sqX0, double sqC0)
   :outertype: CDPSM_to_GLM

   For balanced sequence impedance, return a symmetric phase impedance matrix for GridLAB-D

   We have to write 7 variations to support all combinations of 3, 2 or 1 phases used

   :param name: is the root name for these 7 variations

GetWdgConnection
^^^^^^^^^^^^^^^^

.. java:method:: static String GetWdgConnection(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   parse the CIM WindingConnection enumeration

GetWireData
^^^^^^^^^^^

.. java:method:: static String GetWireData(Model mdl, Resource res)
   :outertype: CDPSM_to_GLM

   needs to return overhead_line_conductor data in GridLAB-D format; res is the CIM OverheadWireInfo instance

   TODO - this is not implemented; the emitted syntax is actually for OpenDSS

GetXfmrCode
^^^^^^^^^^^

.. java:method:: static String GetXfmrCode(Model mdl, String id, double smult, double vmult, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   returns the GridLAB-D transformer_configuration corresponding to TransformerTankInfo 'id'

   bWantSec would be false to ignore single-phase center-tap configurations

   TODO: smult and vmult may be removed, as they should always be 1 for valid CIM XML

   These transformers are described with short-circuit and open-circuit tests, which sometimes use non-SI units like percent and kW, as they appear on transformer test reports

GldPrefixedNodeName
^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GldPrefixedNodeName(String arg)
   :outertype: CDPSM_to_GLM

   prefix all bus names with nd_ for GridLAB-D, so they "should" be unique

MergePhases
^^^^^^^^^^^

.. java:method:: static String MergePhases(String phs1, String phs2)
   :outertype: CDPSM_to_GLM

   accumulate phases without duplication

Phase_Kind_String
^^^^^^^^^^^^^^^^^

.. java:method:: static String Phase_Kind_String(String arg)
   :outertype: CDPSM_to_GLM

   parses a single phase from CIM SinglePhaseKind

Phase_String
^^^^^^^^^^^^

.. java:method:: static String Phase_String(String arg)
   :outertype: CDPSM_to_GLM

   parses the phase string from CIM phaseCode

SafeBoolean
^^^^^^^^^^^

.. java:method:: static boolean SafeBoolean(Resource r, Property p, boolean def)
   :outertype: CDPSM_to_GLM

   look up Jena boolean property p from resource r, returns def if not found

SafeDouble
^^^^^^^^^^

.. java:method:: static double SafeDouble(Resource r, Property p, double def)
   :outertype: CDPSM_to_GLM

   look up Jena double property p from resource r, returns def if not found

SafeInt
^^^^^^^

.. java:method:: static int SafeInt(Resource r, Property p, int def)
   :outertype: CDPSM_to_GLM

   look up Jena integer property p from resource r, returns def if not found

SafePhasesX
^^^^^^^^^^^

.. java:method:: static String SafePhasesX(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   look up Jena phase property p from resource r, returns ABCN if not found

SafeProperty
^^^^^^^^^^^^

.. java:method:: static String SafeProperty(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   look up Jena string property p from resource r, returns def if not found

SafeRegulatingMode
^^^^^^^^^^^^^^^^^^

.. java:method:: static String SafeRegulatingMode(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   parse the CIM regulating control mode enum as Jena property p from resource r

SafeResName
^^^^^^^^^^^

.. java:method:: static String SafeResName(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   returns the CIM name from r.p if it exists, or the r.mrID if not, in GridLAB-D format

SafeResourceLookup
^^^^^^^^^^^^^^^^^^

.. java:method:: static String SafeResourceLookup(Model mdl, Property ptName, Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   returns the GridLAB-D formatted name of a resource referenced by r.p

   ptName should be the IdentifiedObject.Name property of the resource we are looking for

   mdl will always be the ontology model created when reading the CIM XML file

Shunt_Delta
^^^^^^^^^^^

.. java:method:: static boolean Shunt_Delta(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   for loads and capacitors, returns true only if CIM PhaseShuntConnectionKind indicates delta

WirePhases
^^^^^^^^^^

.. java:method:: static String WirePhases(Model mdl, Resource r, Property p1, Property p2)
   :outertype: CDPSM_to_GLM

   Returns GridLAB-D formatted phase string by accumulating CIM single phases, if such are found, or assuming ABC if not found. Note that in CIM, secondaries have their own phases s1 and s2.

main
^^^^

.. java:method:: public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException
   :outertype: CDPSM_to_GLM

   Reads command-line input for the converter

   :param args: will be CDPSM_to_GLM [options] input.xml output_root

   \ **Options**\ :

   -l={0..1} load scaling factor, defaults to 1

   -t={y|n} triplex; y/n to include or ignore secondaries. Defaults to yes. Use no for debugging only, as all secondary load will be ignored.

   -e={u|i} encoding; UTF-8 or ISO-8859-1. No default, so this should be specified. Choose 'u' if the CIM file came frome OpenDSS.

   -f={50|60} system frequency; defaults to 60

   -v={1|0.001} multiplier that converts CIM voltage to V for GridLAB-D; defaults to 1

   -s={1000|1|0.001} multiplier that converts CIM p,q,s to VA for GridLAB-D; defaults to 1

   -q={y|n} are unique names used? If yes, they are used as unique GridLAB-D names. If no, the CIM mrID is de-mangled to create a unique GridLAB-D name, but this option is only implemented for ACLineSegments as written to some earlier GIS profiles.

   -n={schedule_name} root filename for scheduled ZIPloads (defaults to none)

   -z={0..1} constant Z portion (defaults to 0 for CIM-defined LoadResponseCharacteristic)

   -i={0..1} constant I portion (defaults to 0 for CIM-defined LoadResponseCharacteristic)

   -p={0..1} constant P portion (defaults to 0 for CIM-defined LoadResponseCharacteristic)

   \ **Example:**\  java CDPSM_to_GLM -l=1 -e=u -i=1 ieee8500.xml ieee8500

   Assuming Jena and Commons-Math are in Java's classpath, this will produce two output files:

   ..

   #. \ **ieee8500_base.glm**\  with GridLAB-D components for a constant-current model at peak load. This file includes an adjustable source voltage and manual capacitor/tap changer states. It should be invoked from a separate GridLAB-D file that sets up the clock, solver, recorders, etc. For example, these two GridLAB-D input lines set up 1.05 per-unit source voltage on a 115-kV system:

      ..

      * #define VSOURCE=69715.065 // 66395.3 * 1.05
      * #include "ieee8500_base.glm"

      If there were capacitor/tap changer controls in the CIM input file, that data was written to ieee8500_base.glm as comments, which can be recovered through manual edits.

   #. \ **ieee8500_busxy.glm**\  with bus geographic coordinates, used in GridAPPS-D but not GridLAB-D

   \ **Cautions:**\  this converter does not yet implement all variations in the CIM for unbalanced power flow.

   ..

   #. AssetInfo links to WireSpacing, OverheadWireInfo, ConcentricNeutralCableInfo and TapeShieldCableInfo
   #. PerLengthSequenceImpedance has not been tested
   #. Capacitor power factor control mode - not in GridLAB-D
   #. Capacitor user-defined control mode - not in GridLAB-D
   #. Capacitor controlled by load (EnergyConsumer) - need to name loads
   #. Line ratings for PerLengthImpedance
   #. Dielectric constant (epsR) for cables - not in CIM
   #. Soil resistivity (rho) for line impedance - not in CIM
   #. Multi-winding transformers other than centertap secondary-not in GridLAB-D
   #. Unbalanced transformer banks - not in GridLAB-D
   #. Autotransformers have not been tested
   #. schedule_name implemented for secondary loads only, primary loads to be done
   #. Fuse not implemented
   #. Breaker not implemented
   #. Jumper not implemented
   #. Disconnector not implemented
   :throws java.io.UnsupportedEncodingException: If the UTF encoding flag is wrong
   :throws FileNotFoundException: If the CIM RDF input file is not found

   **See also:** :java:ref:`CDPSM_to_GLM`

