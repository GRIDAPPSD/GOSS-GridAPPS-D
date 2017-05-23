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

CDPSM_to_GLM.SpacingCount
=========================

.. java:package:: gov.pnnl.gridlabd.cim
   :noindex:

.. java:type:: static class SpacingCount
   :outertype: CDPSM_to_GLM

   helper class to keep track of the conductor counts for WireSpacingInfo instances

   Number of Conductors is the number of phases (1..3) plus neutrals (0..1)

Constructors
------------
SpacingCount
^^^^^^^^^^^^

.. java:constructor:: public SpacingCount(int nconds, int nphases)
   :outertype: CDPSM_to_GLM.SpacingCount

   construct with number of conductors and phases

   :param nconds: number of phases plus neutrals (1..4)
   :param nphases: number of phase conductors (1..3)

Methods
-------
getNumConductors
^^^^^^^^^^^^^^^^

.. java:method:: public int getNumConductors()
   :outertype: CDPSM_to_GLM.SpacingCount

   :return: accessor to number of conductors

getNumPhases
^^^^^^^^^^^^

.. java:method:: public int getNumPhases()
   :outertype: CDPSM_to_GLM.SpacingCount

   :return: accessor to number of phases

