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

   Helper class to accumulate nodes and loads all EnergyConsumer data will be attached to node objects, then written as load objects this preserves the input ConnectivityNode names TODO - another option is to leave all nodes un-loaded, and attach all loads to parent nodes, closer to what OpenDSS does

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

nomvln
^^^^^^

.. java:field:: public double nomvln
   :outertype: CDPSM_to_GLM.GldNode

   this is always line-to-neutral

pa_i
^^^^

.. java:field:: public double pa_i
   :outertype: CDPSM_to_GLM.GldNode

pa_p
^^^^

.. java:field:: public double pa_p
   :outertype: CDPSM_to_GLM.GldNode

pa_z
^^^^

.. java:field:: public double pa_z
   :outertype: CDPSM_to_GLM.GldNode

   for zip load parameters like pa_z

   first character denotes real power (p) or reactive power (q)

   second character denotes the phase (a, b, c, s1==a, s2==b)

   fourth character denotes constant impedance (z), constant current (i) or constant power (p) share

pb_i
^^^^

.. java:field:: public double pb_i
   :outertype: CDPSM_to_GLM.GldNode

pb_p
^^^^

.. java:field:: public double pb_p
   :outertype: CDPSM_to_GLM.GldNode

pb_z
^^^^

.. java:field:: public double pb_z
   :outertype: CDPSM_to_GLM.GldNode

pc_i
^^^^

.. java:field:: public double pc_i
   :outertype: CDPSM_to_GLM.GldNode

pc_p
^^^^

.. java:field:: public double pc_p
   :outertype: CDPSM_to_GLM.GldNode

pc_z
^^^^

.. java:field:: public double pc_z
   :outertype: CDPSM_to_GLM.GldNode

phases
^^^^^^

.. java:field:: public String phases
   :outertype: CDPSM_to_GLM.GldNode

   ABC allowed

qa_i
^^^^

.. java:field:: public double qa_i
   :outertype: CDPSM_to_GLM.GldNode

qa_p
^^^^

.. java:field:: public double qa_p
   :outertype: CDPSM_to_GLM.GldNode

qa_z
^^^^

.. java:field:: public double qa_z
   :outertype: CDPSM_to_GLM.GldNode

qb_i
^^^^

.. java:field:: public double qb_i
   :outertype: CDPSM_to_GLM.GldNode

qb_p
^^^^

.. java:field:: public double qb_p
   :outertype: CDPSM_to_GLM.GldNode

qb_z
^^^^

.. java:field:: public double qb_z
   :outertype: CDPSM_to_GLM.GldNode

qc_i
^^^^

.. java:field:: public double qc_i
   :outertype: CDPSM_to_GLM.GldNode

qc_p
^^^^

.. java:field:: public double qc_p
   :outertype: CDPSM_to_GLM.GldNode

qc_z
^^^^

.. java:field:: public double qc_z
   :outertype: CDPSM_to_GLM.GldNode

Constructors
------------
GldNode
^^^^^^^

.. java:constructor:: public GldNode(String name)
   :outertype: CDPSM_to_GLM.GldNode

   defaults to zero load and zero phases present

Methods
-------
AddPhases
^^^^^^^^^

.. java:method:: public boolean AddPhases(String phs)
   :outertype: CDPSM_to_GLM.GldNode

   accumulates phases present, always returns true

ApplyZIP
^^^^^^^^

.. java:method:: public void ApplyZIP(double Z, double I, double P)
   :outertype: CDPSM_to_GLM.GldNode

   reapportion loads according to constant power (Z/sum), constant current (I/sum) and constant power (P/sum)

GetPhases
^^^^^^^^^

.. java:method:: public String GetPhases()
   :outertype: CDPSM_to_GLM.GldNode

   returns phasing string for GridLAB-D with appropriate D, S or N suffix

HasLoad
^^^^^^^

.. java:method:: public boolean HasLoad()
   :outertype: CDPSM_to_GLM.GldNode

   true if a non-zero real or reactive load on any phase

RescaleLoad
^^^^^^^^^^^

.. java:method:: public void RescaleLoad(double scale)
   :outertype: CDPSM_to_GLM.GldNode

   scales the load by a factor that probably came from the command line's -l option

