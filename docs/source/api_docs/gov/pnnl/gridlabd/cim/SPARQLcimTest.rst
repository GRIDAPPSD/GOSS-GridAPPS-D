.. java:import:: java.util HashMap

.. java:import:: java.text DecimalFormat

.. java:import:: org.apache.jena.util FileManager

.. java:import:: org.apache.commons.math3.complex Complex

.. java:import:: org.apache.commons.math3.complex ComplexFormat

SPARQLcimTest
=============

.. java:package:: gov.pnnl.gridlabd.cim
   :noindex:

.. java:type:: public class SPARQLcimTest extends Object

   This class runs an example SQARQL query against CIM XML

   Future versions of GridAPPS-D will rely more heavily on SPARQL queries to do the selection and filtering, as the preferred pattern for developers working with CIM. This example uses several triples to execute a query on LinearShuntCompensators (aka capacitors).

   Invoke as a console-mode program

   :author: Tom McDermott

   **See also:** :java:ref:`SPARQLcimTest.main`

Fields
------
baseURI
^^^^^^^

.. java:field:: static final String baseURI
   :outertype: SPARQLcimTest

   identifies gridlabd

nsCIM
^^^^^

.. java:field:: static final String nsCIM
   :outertype: SPARQLcimTest

   namespace for CIM; should match the CIM version used to generate the RDF

nsRDF
^^^^^

.. java:field:: static final String nsRDF
   :outertype: SPARQLcimTest

   namespace for RDF

Methods
-------
GLD_Name
^^^^^^^^

.. java:method:: static String GLD_Name(String arg, boolean bus)
   :outertype: SPARQLcimTest

   convert a CIM name to GridLAB-D name, replacing unallowed characters

main
^^^^

.. java:method:: public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException
   :outertype: SPARQLcimTest

   Reads command-line input for the converter

   :param args: will be SPARQLcimTest [options] input.xml

   Options: -e={u|i} encoding; UTF-8 or ISO-8859-1; choose u if input.xml came from OpenDSS

