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

CDPSM_to_GLM.GldNode
====================

.. java:package:: gov.pnnl.gridlabd.cim
   :noindex:

.. java:type:: static class GldNode
   :outertype: CDPSM_to_GLM

   Helper class to accumulate nodes and loads.

   All EnergyConsumer data will be attached to node objects, then written as load objects. This preserves the input ConnectivityNode names

   TODO - another option is to leave all nodes un-loaded, and attach all loads to parent nodes, closer to what OpenDSS does

Fields
------
bDelta
^^^^^^

.. java:field:: public boolean bDelta
   :outertype: CDPSM_to_GLM.GldNode

   will add N or D phasing, if not S

bSecondary
^^^^^^^^^^

.. java:field:: public boolean bSecondary
   :outertype: CDPSM_to_GLM.GldNode

   if bSecondary true, the member variables for phase A and B loads actually correspond to secondary phases 1 and 2. For GridLAB-D, these are written to phase AS, BS or CS, depending on the primary phase, which we find from the service transformer or triplex.

bSwing
^^^^^^

.. java:field:: public boolean bSwing
   :outertype: CDPSM_to_GLM.GldNode

   denotes the SWING bus, aka substation source bus

name
^^^^

.. java:field:: public final String name
   :outertype: CDPSM_to_GLM.GldNode

   root name of the node (or load), will have `nd_` prepended

nomvln
^^^^^^

.. java:field:: public double nomvln
   :outertype: CDPSM_to_GLM.GldNode

   this nominal voltage is always line-to-neutral

pa_i
^^^^

.. java:field:: public double pa_i
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase A or s1, constant current portion

pa_p
^^^^

.. java:field:: public double pa_p
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase A or s1, constant power portion

pa_z
^^^^

.. java:field:: public double pa_z
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase A or s1, constant impedance portion

pb_i
^^^^

.. java:field:: public double pb_i
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase B or s2, constant current portion

pb_p
^^^^

.. java:field:: public double pb_p
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase B or s2, constant power portion

pb_z
^^^^

.. java:field:: public double pb_z
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase B or s2, constant impedance portion

pc_i
^^^^

.. java:field:: public double pc_i
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase C, constant current portion

pc_p
^^^^

.. java:field:: public double pc_p
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase C, constant power portion

pc_z
^^^^

.. java:field:: public double pc_z
   :outertype: CDPSM_to_GLM.GldNode

   real power on phase C, constant impedance portion

phases
^^^^^^

.. java:field:: public String phases
   :outertype: CDPSM_to_GLM.GldNode

   ABC allowed

qa_i
^^^^

.. java:field:: public double qa_i
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase A or s1, constant current portion

qa_p
^^^^

.. java:field:: public double qa_p
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase A or s1, constant power portion

qa_z
^^^^

.. java:field:: public double qa_z
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase A or s1, constant impedance portion

qb_i
^^^^

.. java:field:: public double qb_i
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase B or s2, constant current portion

qb_p
^^^^

.. java:field:: public double qb_p
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase B or s2, constant power portion

qb_z
^^^^

.. java:field:: public double qb_z
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase B or s2, constant impedance portion

qc_i
^^^^

.. java:field:: public double qc_i
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase C, constant current portion

qc_p
^^^^

.. java:field:: public double qc_p
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase C, constant power portion

qc_z
^^^^

.. java:field:: public double qc_z
   :outertype: CDPSM_to_GLM.GldNode

   reactive power on phase C, constant impedance portion

Constructors
------------
GldNode
^^^^^^^

.. java:constructor:: public GldNode(String name)
   :outertype: CDPSM_to_GLM.GldNode

   constructor defaults to zero load and zero phases present

   :param name: CIM name of the bus

Methods
-------
AddPhases
^^^^^^^^^

.. java:method:: public boolean AddPhases(String phs)
   :outertype: CDPSM_to_GLM.GldNode

   accumulates phases present

   :param phs: phases to add, may contain ABCDSs
   :return: always true

ApplyZIP
^^^^^^^^

.. java:method:: public void ApplyZIP(double Z, double I, double P)
   :outertype: CDPSM_to_GLM.GldNode

   reapportion loads according to constant power (Z/sum), constant current (I/sum) and constant power (P/sum)

   :param Z: portion of constant-impedance load
   :param I: portion of constant-current load
   :param P: portion of constant-power load

GetPhases
^^^^^^^^^

.. java:method:: public String GetPhases()
   :outertype: CDPSM_to_GLM.GldNode

   :return: phasing string for GridLAB-D with appropriate D, S or N suffix

HasLoad
^^^^^^^

.. java:method:: public boolean HasLoad()
   :outertype: CDPSM_to_GLM.GldNode

   :return: true if a non-zero real or reactive load on any phase

RescaleLoad
^^^^^^^^^^^

.. java:method:: public void RescaleLoad(double scale)
   :outertype: CDPSM_to_GLM.GldNode

   scales the load by a factor that probably came from the command line's -l option

   :param scale: multiplying factor on all of the load components

