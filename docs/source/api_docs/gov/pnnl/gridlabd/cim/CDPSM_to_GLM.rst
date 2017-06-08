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

   **See also:** :java:ref:`CDPSM_to_GLM.main`, \ `CIM User Group <http://www.ucaiug.org/default.aspx>`_\, \ `CIM Profile and Queries for Feeder Modeling in GridLAB-D <https://github.com/GRIDAPPSD/Powergrid-Models/blob/temcdrm/CIM/CDPSM_RC1.docx>`_\, \ `GridLAB-D <http://www.gridlabd.org>`_\

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

ptBaseNomV
^^^^^^^^^^

.. java:field::  Property ptBaseNomV
   :outertype: CDPSM_to_GLM

ptEqBaseV
^^^^^^^^^

.. java:field::  Property ptEqBaseV
   :outertype: CDPSM_to_GLM

ptEquip
^^^^^^^

.. java:field::  Property ptEquip
   :outertype: CDPSM_to_GLM

ptLevBaseV
^^^^^^^^^^

.. java:field::  Property ptLevBaseV
   :outertype: CDPSM_to_GLM

Methods
-------
AccumulateLoads
^^^^^^^^^^^^^^^

.. java:method:: static boolean AccumulateLoads(GldNode nd, String phs, double pL, double qL, double Pv, double Qv, double Pz, double Pi, double Pp, double Qz, double Qi, double Qp)
   :outertype: CDPSM_to_GLM

   Distributes a total load (pL+jqL) among the phases (phs) present on GridLAB-D node (nd)

   :param nd: GridLAB-D node to receive the total load
   :param phs: phases actually present at the node
   :param pL: total real power
   :param qL: total reactive power
   :param Pv: real power voltage exponent from a CIM LoadResponseCharacteristic
   :param Qv: reactive power voltage exponent from a CIM LoadResponseCharacteristic
   :param Pz: real power constant-impedance percentage from a CIM LoadResponseCharacteristic
   :param Qz: reactive power constant-impedance percentage from a CIM LoadResponseCharacteristic
   :param Pi: real power constant-current percentage from a CIM LoadResponseCharacteristic
   :param Qi: reactive power constant-current percentage from a CIM LoadResponseCharacteristic
   :param Pp: real power constant-power percentage from a CIM LoadResponseCharacteristic
   :param Qp: reactive power constant-power percentage from a CIM LoadResponseCharacteristic
   :return: always true

Bus_ShuntPhases
^^^^^^^^^^^^^^^

.. java:method:: static String Bus_ShuntPhases(String phs, String conn)
   :outertype: CDPSM_to_GLM

   appends N or D for GridLAB-D loads and capacitors, based on wye or delta connection

   :param phs: from CIM PhaseCode
   :param conn: contains `w` for wye connection and `d` for delta connection
   :return: phs with N or D possibly appended

CFormat
^^^^^^^

.. java:method:: static String CFormat(Complex c)
   :outertype: CDPSM_to_GLM

   :param c: complex number
   :return: formatted string for GridLAB-D input files with 'j' at the end

Count_Phases
^^^^^^^^^^^^

.. java:method:: static int Count_Phases(String phs)
   :outertype: CDPSM_to_GLM

   from the phase string, determine how many are present, but ignore D, N and S

   :param phs: the parsed CIM PhaseCode
   :return: (1..3)

FindBaseVoltage
^^^^^^^^^^^^^^^

.. java:method:: static double FindBaseVoltage(Resource res, Property ptEquip, Property ptEqBaseV, Property ptLevBaseV, Property ptBaseNomV)
   :outertype: CDPSM_to_GLM

   Returns the nominal voltage for conduction equipment, from either its own or container's base voltage. For example, capacitors and transformer ends have their own base voltage, but line segments don't.

   :param res: an RDF resource corresponding to a ConductingEquipment instance; we need to find its base voltage
   :param ptEquip: an RDF property corresponding to the EquipmentContainer association
   :param ptEqBaseV: an RDF property corresponding to a possible BaseVoltage association on the equipment itself
   :param ptLevBaseV: an RDF property corresponding to the EquipmentContainer's BaseVoltage association
   :param ptBaseNomV: an RDF property corresponding to the nominalVoltage attribute of a CIM BaseVoltage
   :return: the nominal voltage as found from the equipment or its container, or 1.0 if not found

FindConductorAmps
^^^^^^^^^^^^^^^^^

.. java:method:: static String FindConductorAmps(Model mdl, Resource res, Property ptDataSheet, Property ptAmps)
   :outertype: CDPSM_to_GLM

   needs to return the current rating for a line segment 'res' that has associated WireInfo at 'ptDataSheet', which in turn has the current rating at ptAmps

   TODO - this is not implemented; emitted syntax is for OpenDSS and the function call (below, in main) needs review

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param res: an RDF resource corresponding to a CIM ACLineSegment
   :param ptDataSheet: an RDF property corresponding to CIM AssetDatasheet attribute
   :param ptAmps: an RDF property corresponding to CIM ratedCurrent attribute
   :return: unusable OpenDSS input

FirstPhase
^^^^^^^^^^

.. java:method:: static String FirstPhase(String phs)
   :outertype: CDPSM_to_GLM

   :param phs: a parsed CIM PhaseCode
   :return: the first phase found as A, B, or C

GLDCapMode
^^^^^^^^^^

.. java:method:: static String GLDCapMode(String s)
   :outertype: CDPSM_to_GLM

   translate the capacitor control mode from CIM to GridLAB-D

   :param s: CIM regulating control mode enum
   :return: MANUAL, CURRENT, VOLT, VAR

GLD_ID
^^^^^^

.. java:method:: static String GLD_ID(String arg)
   :outertype: CDPSM_to_GLM

   parse the GridLAB-D name from a CIM name, based on # position

   :param arg: the CIM IdentifiedObject.name attribute, not the mrID
   :return: the compatible name for GridLAB-D

GLD_Name
^^^^^^^^

.. java:method:: static String GLD_Name(String arg, boolean bus)
   :outertype: CDPSM_to_GLM

   convert a CIM name to GridLAB-D name, replacing unallowed characters and prefixing for a bus/node

   :param arg: the root bus or component name, aka CIM name
   :param bus: to flag whether `nd_` should be prepended
   :return: the compatible name for GridLAB-D

GetACLineParameters
^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetACLineParameters(Model mdl, String name, Resource r, double len, double freq, String phs, PrintWriter out)
   :outertype: CDPSM_to_GLM

   for a standalone ACLineSegment with sequence parameters, find GridLAB-D formatted and normalized phase impedance matrix

   TODO - this is always three-phase, so we don't need all 7 variations from GetSequenceLineConfigurations

   *

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param name: the root name of the line segment and its line_configuration
   :param r: an RDF resource corresponding to a CIM ACLineSegment
   :param len: the length of the ACLineSegment in feet
   :param freq: frequency in Hz for converting susceptance to capacitance
   :param phs: phasing for the written line_configuration (one of 7 variations) that needs to be referenced
   :param out: the PrintWriter instance opened from the main program, passed here so that we can share code in GetSequenceLineConfigurations
   :return: the name of the written line_configuration

GetBusName
^^^^^^^^^^

.. java:method:: static String GetBusName(Model mdl, String eq_id, int seq)
   :outertype: CDPSM_to_GLM

   finds the bus (ConnectivityNode) name for conducting equipment

   :param mdl: an RDF model (set of statements) read from the CIM imput file*
   :param eq_id: the CIM mrID of the conducting equipment
   :param seq: equals 1 to use the first terminal found, or 2 to use the second terminal found
   :return: the GridLAB-D compatible bus name, or `x` if not found. As Terminals no longer have sequence numbers, the ordering of seq is unpredictable, so if there are two we can get bus 1 - bus 2 or bus 2 - bus 1

GetBusPositionString
^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetBusPositionString(Model mdl, String id)
   :outertype: CDPSM_to_GLM

   for a bus (ConnectivityNode), search for X,Y geo coordinates based on connected Terminals and equipment

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param id: name of the bus to search from
   :return: X,Y coordinates in comma-separated value (CSV) format

GetCableData
^^^^^^^^^^^^

.. java:method:: static String GetCableData(Model mdl, Resource res)
   :outertype: CDPSM_to_GLM

   needs to return underground_line_conductor data in GridLAB-D format

   TODO - this is not implemented; the emitted syntax is actually for OpenDSS

   *

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param res: an RDF resource corresponding to a CIM CableInfo (not a leaf/concrete class)
   :return: unusable OpenDSS input

GetCapControlData
^^^^^^^^^^^^^^^^^

.. java:method:: static String GetCapControlData(Model mdl, Resource rCap, Resource ctl)
   :outertype: CDPSM_to_GLM

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param rCap: an RDF resource corresponding to a CIM LinearShuntCompensator (aka capacitor)
   :param ctl: an RDF resource corresponding to the CIM RegulatingControl that was found attached to the LinearShuntCompensator
   :return: the embedded capacitor control data for a GridLAB-D capacitor object

GetEquipmentType
^^^^^^^^^^^^^^^^

.. java:method:: static String GetEquipmentType(Resource r)
   :outertype: CDPSM_to_GLM

   find the type of monitored equipment for controlled capacitors, usually a line or the capacitor itself

   :param r: an RDF resource, will have a CIM mrID, should be a LinearShuntCompensator, ACLineSegment, EnergyConsumer or PowerTransformer
   :return: cap, line, xf if supported in GridLAB-D; NULL or ##UNKNOWN## if unsupported

GetGldTransformerConnection
^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetGldTransformerConnection(String[] wye, int nwdg)
   :outertype: CDPSM_to_GLM

   Map CIM connectionKind to GridLAB-D winding connections. TODO: some of the returnable types aren't actually supported in GridLAB-D

   :param wye: array of CIM connectionKind attributes per winding
   :param nwdg: number of transformer windings, also the size of wye
   :return: the GridLAB-D winding connection. This may be something not supported in GridLAB-D, which should be treated as a feature request

GetImpedanceMatrix
^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetImpedanceMatrix(Model mdl, String name, Property ptCount, Resource r, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   Convert CIM PerLengthPhaseImpedance to GridLAB-D line_configuration

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param name: root name of the line_configuration(s), should be the CIM name
   :param r: an RDF resource, will have a CIM mrID, should be PerLengthPhaseImpedance
   :param ptCount: an RDF property for the PerLengthPhaseImpedance.conductorCount
   :param bWantSec: flags the inclusion of triplex, true except for debugging
   :return: the GridLAB-D formatted impedance matrix for a line configuration. We have to write 3 of these in the case of 1-phase or 2-phase matrices. If (by name) it appears to be triplex and bWantSec is false, nothing will be returned.

GetLineSpacing
^^^^^^^^^^^^^^

.. java:method:: static String GetLineSpacing(Model mdl, Resource rLine)
   :outertype: CDPSM_to_GLM

   needs to return the line_spacing and wire/cncable/tscable assignments for this rLine in GridLAB-D format

   TODO - this is not implemented, the emitted syntax is actually for OpenDSS

   *

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param rLine: an RDF resource corresponding to a CIM ACLineSegment that should have an associated AssetInfo
   :return: unusable OpenDSS input

GetMatIdx
^^^^^^^^^

.. java:method:: static int GetMatIdx(int n, int row, int col)
   :outertype: CDPSM_to_GLM

   converts the [row,col] of nxn matrix into the sequence number for CIM PerLengthPhaseImpedanceData (only valid for the lower triangle) *

   :param n: 2x2 matrix order
   :param row: first index of the element
   :param col: second index
   :return: sequence number

GetPowerTransformerData
^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetPowerTransformerData(Model mdl, Resource rXf)
   :outertype: CDPSM_to_GLM

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param rXf: an RDF resource corresponding to CIM PowerTransformer; it should have mesh impedance data
   :return: transformer and transformer_configuration objects in GridLAB-D format

GetPowerTransformerTanks
^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetPowerTransformerTanks(Model mdl, Resource rXf, ResIterator itTank, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   writes a PowerTransformer in GridLAB-D format, in the case where individual tranformer tanks that are connected together in a bank. GridLAB-D supports only 2-winding banks with same phasing on both sides, or single-phase, center-tapped secondary transformers.

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param rXf: an RDF resource corresponding to a CIM PowerTransformer that uses tank modeling
   :param itTank: a Jena iterator on the tanks associated with rXf, known to be non-empty before this function is called
   :param bWantSec: usually true, in order to include single-phase, center-tapped secondary transformers, which would come to this function
   :return: transformer object in GridLAB-D format; the transformer_configuration comes from calling GetXfmrCode

GetPropValue
^^^^^^^^^^^^

.. java:method:: static String GetPropValue(Model mdl, String uri, String prop)
   :outertype: CDPSM_to_GLM

   unprotected lookup of uri.prop value, to be deprecated in favor of SafeProperty

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param uri: an RDF resource, currently only an EquipmentContainer is used, and it should always exist
   :param prop: currently only IdentifiedObject.name is used, and it should always exist
   :return: the name of the CIM object

GetRegulatorData
^^^^^^^^^^^^^^^^

.. java:method:: static String GetRegulatorData(Model mdl, Resource rXf, String name, String xfGroup, String bus1, String bus2, String phs)
   :outertype: CDPSM_to_GLM

   Connects a regulator in GridLAB-D format between bus1 and bus2; should be called from GetPowerTransformerTanks. In CIM, a regulator consists of a transformer plus the ratio tap changer, so if such is found, should call GetRegulatorData instead of just writing the transformer data in GetPowerTransformerTanks. Any impedance in the regulating transformer will be lost in the GridLAB-D model. Should be called from PowerTransformers that have RatioTapChangers attached, so we know that lookup will succeed

   TODO: implement regulators for tank transformers

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param rXf: an RDF resource corresponding to a CIM PowerTransformer that has a RatioTapChanger associated
   :param name: the name of the PowerTransformer (already looked up before calling this function)
   :param xfGroup: the PowerTransformer's IEC vector group (already looked up before calling this function)
   :param bus1: first bus (ConnectivityNode) on the regulator (already looked up before calling this function)
   :param bus2: second bus (ConnectivityNode) on the regulator (already looked up before calling this function)
   :param phs: phases that contain A, B and/or C (already looked up before calling this function)
   :return: regulator and regulator_configuration objects in GridLAB-D format

GetSequenceLineConfigurations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GetSequenceLineConfigurations(String name, double sqR1, double sqX1, double sqC1, double sqR0, double sqX0, double sqC0)
   :outertype: CDPSM_to_GLM

   For balanced sequence impedance, return a symmetric phase impedance matrix for GridLAB-D. We have to write 7 variations to support all combinations of 3, 2 or 1 phases used.

   :param name: is the root name for these 7 variations
   :param sqR1: positive sequence resistance in ohms/mile
   :param sqX1: positive sequence reactance in ohms/mile
   :param sqC1: positive sequence capacitance in nF/mile
   :param sqR0: zero sequence resistance in ohms/mile
   :param sqX0: zero sequence reactance in ohms/mile
   :param sqC0: zero sequence capacitance in nF/mile
   :return: text for 7 line_configuration objects

GetWdgConnection
^^^^^^^^^^^^^^^^

.. java:method:: static String GetWdgConnection(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   parse the CIM WindingConnection enumeration

   :param r: an RDF resource, will have a CIM mrID, should be a transformerEnd
   :param p: an RDF property, will be a CIM attribute, should be connectionKind
   :param def: default value if property is not found, such as Y
   :return: D, Y, Z, Yn, Zn, A or I

GetWireData
^^^^^^^^^^^

.. java:method:: static String GetWireData(Model mdl, Resource res)
   :outertype: CDPSM_to_GLM

   needs to return overhead_line_conductor data in GridLAB-D format; res is the CIM OverheadWireInfo instance

   TODO - this is not implemented; the emitted syntax is actually for OpenDSS

   *

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param res: an RDF resource corresponding to CIM OverheadWireInfo
   :return: unusable OpenDSS input

GetXfmrCode
^^^^^^^^^^^

.. java:method:: static String GetXfmrCode(Model mdl, String id, double smult, double vmult, boolean bWantSec)
   :outertype: CDPSM_to_GLM

   Translates a single TransformerTankInfo into GridLAB-D format. These transformers are described with short-circuit and open-circuit tests, which sometimes use non-SI units like percent and kW, as they appear on transformer test reports.

   TODO: smult and vmult may be removed, as they should always be 1 for valid CIM XML

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param id: CIM mRID corresponding to a CIM TransformerTankInfo
   :param smult: scaling factor for converting winding ratings to volt-amperes (should be 1)
   :param vmult: scaling factor for converting winding ratings to volts (should be 1)
   :param bWantSec: usually true to include single-phase, center-tapped secondary tranformers, which come to this function
   :return: transformer_configuration object in GridLAB-D format

GldPrefixedNodeName
^^^^^^^^^^^^^^^^^^^

.. java:method:: static String GldPrefixedNodeName(String arg)
   :outertype: CDPSM_to_GLM

   prefix all bus names with `nd_` for GridLAB-D, so they "should" be unique

   :param arg: the root bus name, aka CIM name
   :return: nd_arg

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

   :param arg: CIM SinglePhaseKind enum
   :return: A, B, C, N, s1 or s2

Phase_String
^^^^^^^^^^^^

.. java:method:: static String Phase_String(String arg)
   :outertype: CDPSM_to_GLM

   parses the phase string from CIM phaseCode

   :param arg: CIM PhaseCode enum
   :return: some combination of A, B, C, N, s1, s2, s12

SafeBoolean
^^^^^^^^^^^

.. java:method:: static boolean SafeBoolean(Resource r, Property p, boolean def)
   :outertype: CDPSM_to_GLM

   look up Jena boolean value

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: boolean value, or default if not found

SafeDouble
^^^^^^^^^^

.. java:method:: static double SafeDouble(Resource r, Property p, double def)
   :outertype: CDPSM_to_GLM

   look up Jena double value

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: double value, or default if not found

SafeInt
^^^^^^^

.. java:method:: static int SafeInt(Resource r, Property p, int def)
   :outertype: CDPSM_to_GLM

   look up Jena integer value

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: integer value, or default if not found

SafePhasesX
^^^^^^^^^^^

.. java:method:: static String SafePhasesX(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   look up Jena phase property

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :return: phases in string format, or ABCN if not found

SafeProperty
^^^^^^^^^^^^

.. java:method:: static String SafeProperty(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   look up Jena string property

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: the property (or default value) as a string

SafeRegulatingMode
^^^^^^^^^^^^^^^^^^

.. java:method:: static String SafeRegulatingMode(Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   parse the CIM regulating control mode enum

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: voltage, timeScheduled, reactivePower, temperature, powerFactor, currentFlow, userDefined

SafeResName
^^^^^^^^^^^

.. java:method:: static String SafeResName(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   for components (not buses) returns the CIM name from r.p attribute if it exists, or the r.mrID if not, in GridLAB-D format

   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :return: a name compatible with GridLAB-D

SafeResourceLookup
^^^^^^^^^^^^^^^^^^

.. java:method:: static String SafeResourceLookup(Model mdl, Property ptName, Resource r, Property p, String def)
   :outertype: CDPSM_to_GLM

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param ptName: should be the IdentifiedObject.Name property of the resource we are looking for
   :param r: an RDF resource, will have a CIM mrID
   :param p: an RDF property, will be a CIM attribute
   :param def: default value if property is not found
   :return: the GridLAB-D formatted name of a resource referenced by r.p

Shunt_Delta
^^^^^^^^^^^

.. java:method:: static boolean Shunt_Delta(Resource r, Property p)
   :outertype: CDPSM_to_GLM

   for loads and capacitors, returns true only if CIM PhaseShuntConnectionKind indicates delta

   :param r: an RDF resource, will have a CIM mrID, should be LinearShuntCompensator or EnergyConsumer
   :param p: an RDF property, will be a CIM attribute for phaseConnection
   :return: true if delta connection

WirePhases
^^^^^^^^^^

.. java:method:: static String WirePhases(Model mdl, Resource r, Property p1, Property p2)
   :outertype: CDPSM_to_GLM

   Returns GridLAB-D formatted phase string by accumulating CIM single phases, if such are found, or assuming ABC if not found. Note that in CIM, secondaries have their own phases s1 and s2. *

   :param mdl: an RDF model (set of statements) read from the CIM imput file
   :param r: an RDF resource, will have a CIM mrID, should be something that can have single phases attached
   :param p1: an RDF property, will be a CIM attribute, should associate from a single phase back to r
   :param p2: an RDF property, will be a CIM attribute, should be the single phase instance's phase attribute
   :return: concatenation of A, B, C, s1 and/or s2 based on the found individual phases

main
^^^^

.. java:method:: public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException
   :outertype: CDPSM_to_GLM

   Reads command-line input for the converter

   :param args: will be CDPSM_to_GLM [options] input.xml output_root
   :throws java.io.FileNotFoundException: if the CIM RDF input file is not found

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

   :throws java.io.UnsupportedEncodingException: if the UTF encoding flag is wrong

   **See also:** :java:ref:`CDPSM_to_GLM`

