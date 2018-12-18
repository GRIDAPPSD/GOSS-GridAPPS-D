.. _cdpsm:

.. toctree::
   :maxdepth: 2

This section summarizes the use of a reduced-order CIM [1]_ to support 
feeder modeling for the North American circuits and use cases considered 
in GridAPPS-D.  The full CIM includes over 1100 tables in SQL, each one 
corresponding to a UML class, enumeration or datatype.  RC1 used 
approximately 100 such entities, mapped onto 100+ tables in SQL.  
Subsequent versions of GridAPPS-D use a triple-store database, which is 
better suited for CIM [ref].  

The CIM subset described here is based on 
iec61970cim17v23a_iec61968cim13v11, which formed the basis of a release 
candidate for CIM100.  This candidate CIM100 already includes changes 
proposed from the GridAPPS-D project.  


Class Diagrams for the Profile
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Figure 1 through Figure 11 present the UML class diagrams generated from
Enterprise Architect [2]_. These diagrams provide an essential roadmap
for understanding:

1. How to ingest CIM XML from various sources into the database

2. How to generate native GridLAB-D input files from the database

For those unfamiliar with UML class diagrams:

1. Lines with an arrowhead indicate class inheritance. For example, in
   Figure 1, ACLineSegment inherits from Conductor, ConductingEquipment,
   Equipment and then PowerSystemResource. ACLineSegment inherits all
   attributes and associations from its ancestors (e.g., length), in
   addition to its own attributes and ancestors.

2. Lines with a diamond indicate composition. For example, in Figure 1,
   Substations make up a SubGeographicalRegion, which then
   make up a GeographicRegion.

3. Lines without a terminating symbol are associations. For example, in
   Figure 1, ACLineSegment has (through inheritance) a BaseVoltage,
   Location and one or more Terminals.

4. Italicized names at the top of each class indicate the ancestor (aka
   superclass), in cases where the ancestor does not appear on the
   diagram. For example, in Figure 1, PowerSystemResource inherits from
   IdentifiedObject.

Please see *GridAPPSD\_RC4.eap*\  [3]_ in the repository [4]_ on GitHub
for the latest updates. The EnterpriseArchitect file includes a
description of each class, attribute and association. It can also
generate HTML documentation of the CIM, with more detail than provided
here.

The diagrammed UML associations have a role and cardinality at each end, 
source and target.  In practice, *only one end of each association* is 
profiled and implemented in SQL.  In some cases, the figure captions 
indicate which end, but see the CIM profile for specific definitions, as 
described in the object diagram section.  Usually, the end with 0..1 
multiplicity is implemented instead of the end with 0..* or 1..* 
multiplicity.  

Nearly every CIM class inherits from IdentifiedObject, from which we use
two attributes:

1. mRID is the “master identifier” that must be unique and persistent
   among all instances. It’s often used as the RDF resource identifier,
   and is often a GUID.

2. Name is a human-readable identifier that need not be unique.

Figure 1 shows how equipment is organized into a Feeder.  Each piece of 
Equipment has Terminals that connect at ConnectivityNodes, which represent 
the "buses".  Please note that in GridAPPS-D, we do not use 
TopologicalNode at all.  In some CIM use cases, primary for transmission, 
the TopologicalNode coalesces ConnectivityNodes that are connected by 
buswork and closed switches into a single connection point.  Instead, 
substation buswork models need to explicitly include the low-impedance 
branches and switches between ConnectivityNodes.  We assume that modern 
distribution power flow solvers have no need of TopologicalNode.  

|imgcim0|

Figure 1: Placement of ACLineSegment into a Feeder.  In GridAPPS-D, the 
Feeder is the EquipmentContainer for all power system components and the 
ConnectivityNodeContainer for all nodes.  It's energized from a 
Substation, which is part of a SubGeographicalRegion and 
GeographicalRegion for proper context with other CIM models.  For 
visualization, ACLineSegment can be drawn from a sequence of 
PositionPoints associated via Location.  The Terminals are free-standing; 
two of them will “reverse-associate” to the ACLineSegment as 
ConductingEquipment, and each terminal also has one ConnectivityNode.  The 
Terminal:phases attribute is not used; instead, phases will be defined in 
the ConductingEquipment instances.  The associated 
BaseVoltage:nominalVoltage attribute is important for many of the classes 
that don’t have their own rated voltage attributes, for example, 
EnergyConsumer.  

|imgcim1|

Figure 2: There are four different ways to specify ACLineSegment 
impedances.  In all cases, Conductor:length is required.  The first way is 
to specify the individual ACLineSegment attributes, which are sequence 
impedances and admittances, leaving PerLengthImpedance null.  The second 
way is to specify the same attributes on an associated 
PerLengthSequenceImpedance, in which case the ACLineSegment attributes 
should be null.  The third way is to associate a PerLengthPhaseImpedance, 
leaving the ACLineSegment attributes null.  Only conductorCount from 1 to 
3 is supported, and there will be 1, 3 or 6 reverse-associated 
PhaseImpedanceData instances that define the lower triangle of the Z and Y 
matrices per unit length.  The row and column attributes must agree with 
ACLineSegmentPhase:sequenceNumber.  The fourth way to specify impedance is 
by wire/cable and spacing data, by association to WireSpacingInfo and 
WireInfo.  If there are ACLineSegmentPhase instances reverse-associated to 
the ACLineSegment, then per-phase modeling applies.  There are several use 
cases for ACLineSegmentPhase: 1) single-phase or two-phase primary, 2) 
low-voltage secondary using phases s1 and s2, 3) associated WireInfo data 
where the WireSpacingInfo association exists, 4) assign specific phases to 
the matrix rows and columns in PerLengthPhaseImpedance.  It is the 
application’s responsibility to propagate phasing through terminals to 
other components, and to identify any miswiring.  (Note: this profile does 
not use WireAssemblyInfo, nor the fromPhase and toPhase attributes of 
PhaseImpedanceData.) 

|imgcim2|

Figure 3: The EnergySource is balanced three-phase, balanced two-phase or 
single-phase Thevenin source.  GridAPPS-D use a three-phase EnergySource 
to represent the transmission system.  See Figures 13-14 for DER modeling.  
The EnergyConsumer is a ZIP load, possibly unbalanced, with an associated 
LoadResponse instance defining the ZIP coefficients.  For three-phase 
delta loads, the phaseConnection is D and the three reverse-associated 
EnergyConsumerPhase instances will have phase=A for the AB load, phase=B 
for the BC load and phase=C for the AC load.  A three-phase wye load may 
have either Y or Yn for the phaseConnection.  Single-phase and two-phase 
loads, including secondary loads, should have phaseConnection=I (for 
individual).  

|imgcim3|

Figure 4: There are eight different kinds of Switch supported in the CIM, 
and all of them have zero impedance.  They would all behave the same in 
power flow analysis, and all would require many more attributes than are 
defined in CIM to support protection analysis.  The use cases for 
SwitchPhase include 1) single-phase, two-phase and secondary switches, 2) 
one or two conductors open in a three-phase switch or 3) transpositions, 
in which case phaseSide1 and phaseSide2 would be different.  RatedCurrent 
may different among the phases, e.g., individual fuses on the same pole 
may have different ratings.  

|imgcim4|

Figure 5: On the left, LinearShuntCompensator and 
LinearShuntCompensatorPhase define capacitor banks, in a way very similar 
to EnergyConsumer in Figure 3.  The kVAR ratings must be converted to 
susceptance based on the nominal voltage, nomU.  Note that aVRDelay is 
really a capacitor control parameter, to be used in conjunction with 
RegulatingControl on the right-hand side.  The RegulatingControl 
associates to the controlled capacitor bank via RegulatingCondEq, and to 
the monitored location via Terminal.  There is no support for a PT or CT 
ratio, so targetDeadband and targetValue have to be in primary volts, 
amps, vars, etc.  Capacitor banks may _respond_ to any of the 
RegulatingControlModeKind choices, but it's not expected that capacitor 
switching will successfully regulate to the targetValue.  

|imgcim5|

Figure 6: PowerTransformers may be modeled with or without tanks, and in 
both cases vectorGroup should be specified according to IEC transformer 
standards (e.g., Dy1 for many substation transformers).  The case without 
tanks is most suitable for balanced three-phase transformers that won’t 
reference catalog data; any other case should use tank-level modeling.  In 
the tankless case, each winding will have a PowerTransformerEnd that 
associates to both a Terminal and a BaseVoltage, and the parent 
PowerTransformer.  The impedance and admittance parameters are defined by 
reverse-associated TransformerMeshImpedance between each pair of windings, 
and a reverse-associated TransformerCoreAdmittance for one winding.  The 
units for these are ohms and siemens based on the winding voltage, rather 
than per-unit.  WindingConnection is similar to PhaseShuntConnectionKind, 
adding Z and Zn for zig-zag connections and A for autotranformers.  If the 
transformer is unbalanced in any way, then TransformerTankEnd is used 
instead of PowerTransformerEnd, and then one or more TransformerTanks may 
be used in the parent PowerTransformer.  Some of the use cases are 1) 
center-tapped secondary, 2) open-delta and 3) EHV transformer banks.  
Tank-level modeling is also required if using catalog data, as described 
with Figure 9.  (TransformerStarImpedance and several PowerTransformer 
attributes are not used.  Star impedance attributes on PowerTransformerEnd 
and magnetic saturation attributes on TransformerEnd are not used.) 

|imgcim6|

Figure 7: A RatioTapChanger can represent a transformer tap changer on the 
associated TransformerEnd.  The RatioTapChanger has some parameters 
defined in a direct-associated TapChangerControl, which inherits from 
RegulatingControl some of the same attributes used in capacitor controls 
(Figure 5).  Therefore, a line voltage regulator in CIM includes a 
PowerTransformer, a RatioTapChanger, and a TapChangerControl.  The CT and 
PT parameters of a voltage regulator can only be described via the 
AssetInfo mechanism, described with Figure 8.  The RegulationControl.mode 
must be voltage.  (Note: RegulationSchedule, RatioTapChangerTable and 
PhaseTapChanger are not used.) 

|imgcim7|

Figure 8: Many distribution software packages use the concept of catalog 
data, aka library data, especially for lines and transformers.  We use the 
AssetInfo package to implement this in CIM.  Here, the TapChangerInfo 
class includes the CT rating, CT ratio and PT ratio parameters needed for 
line drop compensator settings in voltage regulators.  Catalog data is a 
one-to-many relationship.  In this case, many TapChangers can share the 
same TapChangerInfo data, which saves space and provides consistency.  
Older versions of CIM had many-to-many catalog relationships, but now only 
one AssetDataSheet may be associated per Equipment.  (Note: many datasheet 
attributes are not shown here and not yet used in GridAPPS-D).  

|imgcim8|

Figure 9: The catalog mechanism for transformers will associate a 
TransformerTank (Figure 6) with TransformerTankInfo (here), via the 
AssetDataSheet mechanism described in Figure 8.  The PowerTransformerInfo 
collects TransformerTankInfo by reverse association, but it does not link 
with PowerTransformer.  In other words, the physical tanks are cataloged 
because transformer testing is done on tanks.  One possible use for 
PowerTransformerInfo is to help organize the catalog.  It’s important 
that TransformerEndInfo:endNumber (here) properly match the 
TransformerEnd:endNumber (Figure 6).  The shunt admittances are defined by 
NoLoadTest on a winding / end, usually just one such test.  The impedances 
are defined by a set of ShortCircuitTests; one winding / end will be 
energized, and one or more of the others will be grounded in these tests.  
(OpenCircuitTest is not used, nor are the current, power and voltage 
attributes of ShortCircuitTest).  

|imgcim9|

Figure 10: The catalog / library mechanism for ACLineSegment will have a 
WireSpacingInfo associated as in Figure 9.  This will indicate whether the 
line is overhead or underground.  phaseWireCount and phaseWireSpacing 
define optional bundling, so these will be 1 and 0 for distribution.  The 
number of phase and neutral conductors is actually defined by the number 
of reverse-associated WirePosition instances.  For example, a three-phase 
line with neutral would have four of them, sequenceNumber from 1 to 4.  
Each WirePosition's phase is determined by the ACLineSegmentPhase with 
matching sequenceNumber, i.e., the phases need not be numbered in any 
particular order.  On the left-hand side, concrete classes 
OverheadWireInfo, TapeShieldCableInfo and ConcentricNeutralCableInfo may 
be associated to ACLineSegmentPhase.  It's the application's 
responsibility to calculate impedances from this data.  In particular, 
soil resistivity and dielectric constants are not included in the CIM.  
Typical dielectric constant values might be defined for each 
WireInsulationKind.  

|imgcim10|

Figure 11: The CIM state variables package was designed to report power 
flow solution values on the distribution system.  It could also report 
state estimator solutions as a special case of power flow solutions.  
Voltages are measured on ConnectivityNodes (i.e., not TopologicalNodes), 
power flows are measured at Terminals into the ConductingEquipment, step 
positions are measured on TapChangers, status is measured on 
ConductingEquipment, and on/off state is measured on ShuntCompensators for 
Switches.  The “injections” have been included here, but there may not 
be a use case for them in distribution.  On the other hand, solution 
values for current are very common in distribution system applications.  
These should be represented as SvPowerFlow values at the solved SvVoltage. 
 
|imgmeas|

Figure 12: Measurements
 
|imginverters|

Figure 13: Power Electronics.

|imgmachines|

Figure 14: Rotating Machines.

|imghouses|

Figure 15: Houses.

|imgfaults|
 
Figure 16: Faults.

Typical Queries
^^^^^^^^^^^^^^^

These queries focus on requirements of the first volt-var application.

1. Capacitors (Figure 5, Figure 17, Figure 18, Figure 19)

   a. Create a list of capacitors with bus name (Connectivity Node in
      Figure 1), kVAR per phase, control mode, target value and target
      deadband

   b. For a selected capacitor, update the control mode, target value,
      and target deadband

2. Regulators (Figure 7, Figure 8, Figure 17, Figure 34)

   a. List all transformers that have a tap changer attached, along with
      their bus names and kVA sizes

   b. Given a transformer that has a tap changer attached, list or
      update initialDelay, step, subsequentDelay, mode, targetDeadband,
      targetValue, limitVoltage, lineDropCompensation, lineDropR,
      lineDropX, reverseLineDropR and reverseLineDropX

3. Transformers (Figure 6, Figure 9)

   a. Given a bus name or load (Figure 3), find the transformer serving
      it (Figure 21, Figure 24)

   b. Find the substation transformer, defined as the largest
      transformer (by kVA size and or highest voltage rating)

   c. List the transformer catalog (Figure 9, Figure 25) with name,
      highest ratedS, list of winding ratedU in descending order, vector
      group (https://en.wikipedia.org/wiki/Vector\_group used with
      connectionKind and phaseAngleClock), and percent impedance

   d. List the same information as in item c, but for transformers
      (Figure 6) and also retrieving their bus names. Note that a
      transformer can be defined in three ways

      i.   Without tanks, for three-phase, multi-winding, balanced
           transformers (Figure 21 and Figure 22).

      ii.  With tanks along with TransformerTankInfo (Figure 9) from a
           catalog of “transformer codes”, which may describe balanced
           or unbalanced transformers. See Figure 24 and Figure 25.

      iii. With tanks for unbalanced transformers, and
           TransformerTankInfo created on-the-fly. See Figure 24 and
           Figure 25.

   e. Given a transformer (Figure 6), update it to use a different
      catalog entry (TransformerTankInfo in Figure 9)

4. Lines (Figure 2, Figure 10, Figure 17)

   a. List the line and cable catalog entries that meet a minimum
      ratedCurrent and specific WireUsageKind. For cables, be able to
      specify tape shield vs. concentric neutral, the
      WireInsulationKind, and a minimum insulationThickness. (Figure 32)

   b. Given a line segment (Figure 2) update to use a different linecode
      (Figure 10, Figure 31)

   c. Given a bus name, list the ACLineSegments connected to the bus,
      along with the length, total r, total x, and phases used. There
      are four cases as noted in the caption of Figure 2, and see Figure
      28 through Figure 31.

   d. Given a bus name, list the set of ACLineSegments (or
      PowerTransformers and Switches) completing a path from it back to
      the EnergySource (Figure 3). Normally, the applications have to
      build a graph structure in memory to do this, so it would be very
      helpful if a graph/semantic database can do this.

5. Voltage and other measurements (Figure 1, Figure 11)

   a. Given a bus, attach a voltage solution point (SvVoltage, Figure
      35)

   b. List all voltage solution points and their buses, and for each
      bus, list the phases actually present

   c. For tap changer position (SvTapStep, Figure 36), attach and list
      values as in items a and b

   d. For capacitor switch status (SvShuntCompensatorSections, Figure
      37), attach and list values as in items a and b

6. Loads (Figure 3, Figure 33)

   a. Given a bus name, list and total all of the loads connected by
      phase, showing the total p and q, and the composite ZIP
      coefficients

7. Switching (Figure 4, Figure 27)

   a. Given a bus name, trace back to the EnergySource and list the
      switches encountered, grouped by type (i.e. the leaf class in
      Figure 4). Also include the ratedCurrent, breakingCapacity if
      applicable, and open/close status. If SwitchPhase is used, show
      the phasing on each side and the open/close status of each phase.

   b. Given switch, toggle its open/close status.

Object Diagrams for Queries
^^^^^^^^^^^^^^^^^^^^^^^^^^^

This section contains UML object diagrams for the purpose of
illustrating how to perform typical queries and updates. For those
unfamiliar with UML object diagrams:

1. Each object will be an instance of a class, and more than one
   instance of a class can appear on the diagram. For example, Figure 17
   shows two ConnectivityNode instances, one for each end of a
   ConductingEquipment.

2. The object name (if specified and important) appears before the colon
   (:) above the line, while the UML class appears after the colon.
   Every object in CIM will have a unique ID, and a name (not
   necessarily unique), even if not shown here.

3. Some objects may be shown with run-time state below the line. These
   are attribute value assignments, drawn from those available in the
   UML class or one of the class ancestors. The object may have more
   attribute assignments, but only those directly relevant to the figure
   captions are shown in the diagrams of this section.

4. Object associations are shown with solid lines, role names, and
   multiplicities similar to the UML class diagrams. One important
   difference is that only one way of navigating a particular
   association will be defined in the profile. For example, the lower
   left corner of Figure 1 shows a two-way link between Terminal
   and ConnectivityNode in the UML class diagram. However, Figure 17
   shows that only one direction has been defined in the profile. Each
   Terminal has a direct reference to its corresponding
   ConnectivityNode. In order to navigate the reverse direction from
   ConnectivityNode to Terminal, some type of conditional query
   would be required. In other words, the object diagrams in this
   section indicate which associations can actually be used in
   GridAPPS-D.

5. In some cases, the multiplicities on the object diagrams are more
   restrictive than on the class diagrams, due to profiling. For
   example, EnergyConsumer and ShuntCompensator must have exactly one Terminal, not 1..*.

The object diagrams are intended to help you break down the CIM queries
into common sub-tasks. For example, query #1 works with capacitors. It’s
always possible to select a capacitor (aka LinearShuntCompensator) by
name. In order to find the capacitor at a bus, say “bus1” in Figure 12,
one would retrieve all Terminals having a ConnectivityNode reference to
“bus1”. Each of those Terminals will have a ConductingEquipment
reference, and you want the Terminal(s) for which that reference is
actually a LinearShuntCompensator. In this CIM profile, only leaf
classes (e.g. LinearShuntCompensator) will be instantiated, never base
classes like ConductingEquipment. There can be more than one capacitor
at a bus, more than one load, more than one line, etc.

|imgcim11|

Figure 17: In order to traverse buses and components, begin with a 
ConnectivityNode (left).  Collect all terminals referencing that 
ConnectivityNode; each Terminal will have one-to-one association with 
ConductingEquipment, of which there are many subclasses.  In this example, 
the ConductingEquipment has a second terminal referencing the 
ConnectivityNode called bus2.  There are applications for both Depth-First 
Search (DFS) and Bread-First Search (BFS) traversals.  Note 1: the 
Terminals have names, but these are not useful.  In some cases, the 
Terminal sequenceNumber attribute is needed to clearly identify ends of a 
switch.  Note 2: in earlier versions of GridAPPS-D, we had one-to-one 
association of TopologicalNode and ConnectivityNode, but these are no 
longer necessary.  Note 3: transformers are subclasses of 
ConductingEquipment, but we traverse connectivity via transformer ends 
(aka windings).  This is illustrated later.  

In order to find capacitors (or anything else) associated with a 
particular “feeder”, Figure 18 shows that you would query for objects 
having EquipmentContainer reference to the Feeder object.  In GridAPPS-D, 
we only use Feeder for equipment container in CIM, and this would 
correspond to one entire GridLAB-D model.  There is also a BaseVoltage 
reference that will have the system nominal voltage for the capacitor’s 
location.  However, in order to work with equipment ratings you should use 
ratedS and ratedU attributes where they exist, particularly for capacitors 
and transformers.  These attributes are often slightly different than the 
“system voltage”.  Most of the attribute units in CIM are SI, with a 
few exceptions like percent and kW values on transformer test sheets 
(i.e., CIM represents the test sheet, not the equipment).  

|imgcim12|

Figure 18: All conducting equipment lies within an EquipmentContainer,
which in GridAPPS-D, will be a Feeder object named after the feeder. It
also has reference to a BaseVoltage, which is typically one of the ANSI
preferred system voltages. Power transformers are a little different, in
that each winding (called “end” in CIM) has reference to a BaseVoltage.
Note that equipment ratings come from the vendor, and in this case
ratedU is slightly different from nominalVoltage. All conducting
equipment has a Location, which contains XY coordinates (see Figure 1).
The Location is useful for visualization, but is not essential for a
power flow model.

Completing the discussion of capacitors, Figure 19 provides two examples
for single-phase, and three-phase with local voltage control. As shunt
elements, capacitors have only one Terminal instance. Loads and sources
have one terminal, lines and switches have two terminals, and
transformers have two or more terminals. Examples of all those are shown
later. In Figure 19, the capacitor’s kVAR rating will be based on its
nameplate ratedU, not the system’s nominalVoltage.

Often, the question will arise “what phases exist at this bus?”.There 
is no phasing explicitly associated with a ConnectivityNode, and we don't 
use the Terminal phases attribute in preference to the "wires phase model" 
classes.  For example, thephases at a line segment terminal can always be 
obtained from the ACLineSegmentPhase instances.  To answer the question 
about bus phasing, we’d have to query for all ConductingEquipment 
instances having Terminals connected to that bus, as in Figure 17.  The 
types of ConductingEquipment that may have individual phases include 
LinearShuntCompensators (Figure 19), ACLineSegments, PowerTransformers 
(via TransformerEnds), EnergyConsumers, EnergySources, 
PowerElectronicsConnections, and descendants of Switch.  If the 
ConductingEquipment has such individual phases, then add those phases to 
list of phases existing at the bus.  If there are no individual phases, 
then ABC all exist at the bus.  Note this doesn’t guarantee that all 
wiring to the bus is correct; for example, you could still have a 
three-phase load served by only a two-phase line, which would be a 
modeling error.  In Figure 19, we’d find phase C at Bus611 and phases 
ABC at Bus675.  Elsewhere in the model, there should be ACLineSegments, 
PowerTransformers or Switch descendants delivering phase C to Bus611, all 
three phases ABC to Bus675.  

|imgcim13|

Figure 19: Capacitors are called LinearShuntCompensator in CIM. On the
left, a 100 kVAR, 2400 V single-phase bank is shown on phase C at bus
611. bPerSection = 100e3 / 2400^2 [S], and the bPerSection on
LinearShuntCompensatorPhase predominates; these values can differ among
phases if there is more than one phase present. On the right, a balanced
three-phase capacitor is shown at bus 675, rated 300 kVAR and 4160 V
line-to-line. We know it’s balanced three phase from the absence of
associated LinearShuntCompensatorPhase objects. bPerSection = 300e4 /
4160^2 [S]. This three-phase bank has a voltage controller attached with
2400 V setpoint and 240 V deadband, meaning the capacitor switches ON if
the voltage drops below 2280 V and OFF if the voltage rises above 2520
V. These voltages have to be monitored line-to-neutral in CIM, with no
VT ratio. In this case, the control monitors the same Terminal that the
capacitor is connected to, but a different conducting equipment’s
Terminal could be used. The control delay is called aVRDelay in CIM, and
it’s an attribute of the LinearShuntCompensator instead of the
RegulatingControl. It corresponds to “dwell time” in GridLAB-D.

Figure 20 through Figure 25 illustrate the transformer query tasks, plus 
Figure 34 for attached voltage regulators.  The autotransformer example is 
rated 500/345/13.8 kV and 500/500/50 MVA, for a transmission system.  The 
short circuit test values are Z\ :sub:`HL`\ =10%, Z\ :sub:`HT`\ =25% and 
Z\ :sub:`LT`\ =30%.  The no-load test values are 0.05% exciting current 
and 0.025% no-load losses.  These convert to r, x, g and b in SI units, 
from Z\ :sub:`LT`\ = U\ :sub:`rated`\ * U\ :sub:`rated`\ / S\ 
:sub:`rated`\, where S\ :sub:`rated`\ and U\ :sub:`rated`\ are based on 
the “from” winding (aka end).  The same base quantities would be used 
to convert r, x, g and b back to per-unit or percent.  The open wye – 
open delta impedances are already represented in percent or kW, from the 
test reports.  

|imgcim14|

Figure 20: Autotransformer with delta tertiary winding acts like a
wye-wye transformer with smaller delta tertiary. The vector group would
be Yynd1 or Yyd1. For analyses other than power flow, it can be
represented more accurately as the physical series (n1) – common (n2)
connection, with a vector group Yand1. In either case, it’s a
three-winding transformer.

|imgcim15|

Figure 21: A three-winding autotransformer is represented in CIM as a 
PowerTransformer with three PowerTransformerEnds, because it’s balanced 
and three-phase.  The three Terminals have direct ConductingEquipment 
references to the PowerTransformer, so you can find it from bus1, busX or 
busY.  However, each PowerTransformerEnd has a back-reference to the same 
Terminal, and it’s own reference to BaseVoltage (Figure 13); that’s 
how you link the matching buses and windings, which must have compatible 
voltages.  Terminals have a sequenceNumber, but the PowerTransformerEnd's 
endNumber is what establishes correct linkage to catalog data as discussed 
later.  By convention, ends with highest ratedU have the lowest endNumber, 
and endNumber establishes that end’s place in the vectorGroup.  

|imgcim16|

Figure 22: Power transformer impedances correspond to the three-winding
autotransformer example of Figure 15 and Figure 16. There are three
instances of TransformerMeshImpedance connected pair-wise between the
three windings / ends. The x and r values are in Ohms referred to the
end with highest ratedU in that pair. There is just one
TransformerCoreAdmittance, usually attached to the end with lowest
ratedU, and the attribute values are Siemens referred to that end’s
ratedU.

|imgcim17|

Figure 23: Open wye - open delta transformer banks are used to provide 
inexpensive three-phase service to loads, by using only two single-phase 
transformers.  This is an unbalanced transformer, and as such it requires 
tank modeling in CIM.  Physically, the two transformers would be in 
separate tanks.  Note that Tank A is similar to the residential 
center-tapped secondary transformer, except the CIM phases for the 
secondary would include s1 and s2 instead of A and B.  

|imgcim18|

Figure 24: Unbalanced PowerTransformer instances comprise one or more
TransformerTanks, which own the TransformerTankEnds. Through the ends,
wdgHi collects phases ABN and busLo collects phases ABCN. Typically,
phase C will also exist at wdgHi, but this transformer doesn’t require
it. We still assign vectorGroup Yd1 to the supervising PowerTransformer,
as this is the typical case. The modeler should determine that. By
comparison to Figure 23, there is a possible ambiguity in how endA3
represents the polarity dot at the neutral end of Wdg A3. An earlier CIM
proposal would have assigned phaseAngleClock = 6 on wdgA3, but the
attribute was removed from TransformerTankEnd. It may not be possible to
infer the correct winding polarities from the vectorGroup in all cases.
There is a phaseAngleClock attribute on TransformerTankEndInfo, but that
represents a shelf state of the tank, not necessarily connections in the
field. Therefore, it may be necessary to propose the phaseAngleClock
attribute for TransformerTankEnd.

|imgcim19|

Figure 25: This Asset catalog example defines the impedances for Tank B of 
the open wye – open delta bank.  This is a 50 kVA, 7200 / 240 V 
single-phase transformer.  It has 1% exciting current and 0.4 kW loss in 
the no-load test, plus 2.1% reactance and 0.5 kW loss in the short-circuit 
test.  A multi-winding transformer could have more than one grounded end 
in a short-circuit test, but this is not common.  The catalog data is 
linked with an AssetDataSheet association shown to the left.  Furthermore, 
endNumber on the TransformerEndInfo has to match endNumber on the 
TransformerTankEnd instances associated to Tank B.  Instead of catalog 
information, we could have used mesh impedance and core admittance as in 
Figure 17, but we’d have to convert the test sheets to SI units and we 
could not share data with other TransformerTank instances, both of which 
are inconvenient.  

Figure 26 through Figure 32 illustrate the query tasks for
ACLineSegments and Switches, which will define most of the circuit’s
connectivity. The example sequence impedances were based on Z\ :sub:`1`
= 0.1 + j0.8 Ω/mile and Z\ :sub:`0` = 0.5 + j2.0 Ω /mile. For
distribution systems, use of the shared catalog data is more common,
either pre-calculated matrix (Figure 30) or spacing and conductor
(Figure 31 and Figure 32). In both cases, impedance calculation is
outside the scope of CIM (e.g. GridLAB-D internally calculates line
impedance from spacing and conductor data).

|imgcim20|

Figure 26: An ACLineSegment with two phases, A and C. If there are no
ACLineSegmentPhase instances that associate to it, assume it’s a
three-phase ACLineSegment. This adds phases AC to bus671 and bus684.

|imgcim21|

Figure 27: This 50-Amp load break switch connects phases AC between 
busLeft and busRight.  Without associated SwitchPhase instances, it would 
be a three-phase switch.  This switch also transposes the phases; A on 
side 1 connects with C on side 2, while C on side 1 connects with A on 
side 2.  This is the only way of transposing phases in CIM.  Note the 
Terminal.sequenceNumber is essential to differentiate phaseSide1 from 
phaseSide2.  Also note that LoadBreakSwitch has the open attribute 
inherited from Switch, while SwitchPhase has the converse closed 
attribute.  In order to open and close the switch, these attributes would 
be toggled appropriately.  See Figure 4 for other types of switch.  

|imgcim22|

Figure 28: This is a balanced three-phase ACLineSegment between bus632
and bus671, 2000 feet or 609.6 m long. Sequence impedances are specified
in ohms, as attributes on the ACLineSegment. This is a typical pattern
for transmission lines, but not distribution lines.

|imgcim23|

Figure 29: The impedances from Figure 23 were divided by 609.6 m, to
obtain ohms per meter for seqCat1. Utilities often call this a “line
code”, and other ACLineSegment instances can share the same
PerLengthImpedance. A model imported into the CIM could have many line
codes, not all of them used in that particular model. However, those
line codes should be available for updates by reassigning
PerLengthImpedance.

|imgcim24|

Figure 30: This is a two-phase line segment from bus671 to bus684 using a 
line code, which has been specified using a 2x2 symmetric matrix of phase 
impedances per meter, instead of sequence impedances per meter.  This is 
more common for distribution than either Figure 28 or Figure 29.  It’s 
distinguished from Figure 29 by the fact that PerLengthImpedance 
references an instance of PerLengthPhaseImpedance, not 
PerLengthSequenceImpedance.  The conductorCount attribute tells us it’s 
a 2x2 matrix, which will have two unique diagonal elements and one 
distinct off-diagonal element.  The elements are provided in three 
PhaseImpedanceData instances, which are named here for clarity as Z11, Z12 
and Z22.  However, only the row and column attributes are meaningful to 
identify the matrix element.  In this example, Z11 and Z22 are slightly 
different.  In order to swap phases A and C, we would swap the 
sequenceNumber values on the ACLineSegmentPhase instaces.  As presented 
here, mtx604 can apply to phasing AB, BC or AC.  

|imgcim25|

Figure 31: The two-phase ACLineSegment impedance defined by sharing wire 
and spacing data from a catalog.  Each ACLineSegmentPhase links to an 
OverheadWireInfo instance via the AssetDataSheet association.  If the 
neutral (N) is present, we have to specify its wire information for a 
correct impedance calculation.  In this case, ACN all use the same wire 
type, but they can be different, especially for the neutral.  Similarly, 
the WireSpacingInfo associates to the ACLineSegment itself via a 
AssetDataSheet assocation.  

|imgcim26|

Figure 32: The upper five instances define catalog attributes for Figure 
26.  The WirePosition xCoord and yCoord units are meters, not feet, and 
they include sequenceNumber assignments to match ACLineSegmentPhase 
sequenceNumbers.  The phaseWireSpacing and phaseWireCount attributes are 
for sub-conductor bundling on EHV and UHV transmission lines; bundling is 
not used on distribution.  The number of WirePositions that reference 
spc505acn determine how many wires need to be assigned.  Eliminating the 
neutral, this would produce a 2x2 phase impedance matrix.  Although the 
pattern appears general enough to support multiple neutrals and 
transmission overbuild, the CIM doesn’t actually have the required 
phasing codes.  When isCable is true, the WirePosition yCoord values would 
be negative for underground depth.  To find overhead wires of a certain 
size or ampacity, we can put query conditions on the ratedCurrent 
attribute.  To find underground conductors, we query the 
ConcentricNeutralCableInfo or TapeShieldCableInfo instead of 
OverheadWireInfo.  All three inherit the ratedCurrent attribute from 
WireInfo.  Cables don’t yet have a voltage rating in CIM AssetInfo, but 
you can use insulationThickness as a proxy for voltage rating in queries.  
Here, 5.588 mm corresponds to 220 mils, which is a common size for 
distribution.  

Figure 33 illustrates the loads, which are called EnergyConsumer in CIM.
The houses and appliances from GridLAB-D are not supported in CIM. Only
ZIP loads can be represented. Further, any load schedules would have to
be defined outside of CIM. Assume that the CIM loads are peak values.

Figure 34 illustrates the voltage regulator function. Note that
GridLAB-D combines the regulator and transformer functions, while CIM
separates them. Also, the CIM provides voltage and current transducer
ratios for tap changer controls, but not for capacitor controls.

Figure 35 through Figure 37 illustrate how solved values can be attached 
to buses or other components.  

|imgcim27|

Figure 33: The three-phase load (aka EnergyConsumer) on bus671 is balanced 
and connected in delta.  It has no ratedU attribute, so use the referenced 
BaseVoltage (Figure 18) if a voltage level is required.  On the right, a 
three-phase wye-connected unbalanced load on bus675 is indicated by the 
presence of three EnergyConsumerPhase instances referencing 
UnbalancedLoad.  For consistency in searches and visualization, 
UnbalancedLoad.p should be the sum of the three phase values, and likewise 
for UnbalancedLoad.q.  In power flow solutions, the individual phase 
values would be used.  Both loads share the same LoadResponse instance, 
which defines a constant power characteristic for both P and Q, because 
the percentages for constant impedance and constant current are all zero.  
The two other most commonly used LoadResponseCharacteristics have 100% 
constant current, and 100% constant impedance.  Any combination can be 
used, and the units don’t have to be percent (i.e., use a summation to 
determine the denominator for normalization).  

|imgcim28|

Figure 34: In CIM, the voltage regulator function is separated from the 
tap-changing transformer.  The IEEE 13-bus system has a bank of three 
independent single-phase regulators at busRG60, and this example shows a 
RatioTapChanger attached to the regulator on phase A, represented by the 
TransformerTankEnd having phases=A or phases=AN.  See Figure 24 for a more 
complete picture of TransformerTankEnds, or Figure 21 for a more complete 
picture of PowerTransformerEnds.  Either one can be the TransformerEnd in 
this figure, but with a PowerTransformerEnd, all three phase taps would 
change in unison (i.e.  they are “ganged”).  Most regulator attributes 
of interest are found in RatioTapChanger or TapChangerControl instances.  
However, we need the AssetDataSheet mechanism to specify ctRatio, ptRatio 
and ctRating values.  These are inherent to the equipment, whereas the 
attributes of RatioTapChanger and TapChangerControl are all settings per 
instance.  For the IEEE 13-bus example, there would be separate 
RatioTapChanger and TapChangerControl instances for phases B and C.  

|imgcim29|

Figure 35: In this profile, a solved voltage value attaches to 
ConnectivityNode in GridAPPS-D.  Positive sequence or phase A is implied, 
unless the phase attribute is specified.  

|imgcim30|

Figure 36: SvTapStep links to a TransformerEnd indirectly, through the
RatioTapChanger. There is no phasing ambiguity because
TransformerTankEnd has its phases attribute, while PowerTransformerEnd
always includes ABC. Units for SvTapStep.position are per-unit.

|imgcim31|

Figure 37: The on/off value for a capacitor bank attaches directly to 
LinearShuntCompensator.  If the phase attribute is not specified, then 
this value applies to all phases.  

Metering Relationship to Loads in the CIM
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These UML class relationships in Figure 33 through Figure 35 have not
been planned for implementation in RC1, but in a future version of
GridAPPS-D, they can be used to link automated meter readings with loads
in the distribution system model.

|imgcim33|

Figure 39: Trouble Calls route through Metering Usage Points to EnergyConsumers

CIM Enhancements for RC4
^^^^^^^^^^^^^^^^^^^^^^^^

Possible CIM enhancements to support volt-var feeder modeling:

1. Different on and off delay parameters for RegulatingControl (Figure
   5)

2. Current ratings for PerLengthImpedance (Figure 2). At present, some
   users rely on associated WireInfo, ignoring all attributes except
   currentRating.

3. Transducers for RegulatingControl (Figure 5)

4. Dielectric constant and soil resistivity (Figure 10)

5. Clock angles for TransformerTankEnd (i.e. move phaseAngleClock from
   PowerTransformerEnd to TransformerEnd (Figure 6)

CIM Profile in CIMTool
^^^^^^^^^^^^^^^^^^^^^^

CIMTool was used to develop and test the profile for RC1, because it:

1. Generates SQL for the MySQL database definition

2. Validates instance files against the profile

The CIMTool developer will not be able to support the tool in future, so
eventually we will use the new Schema Composer feature in Enterprise
Architect.

In order to view the profile, import the archived Eclipse project
*OSPRREYS\_CIMTOOL.zip* into CIMTool. Please see the CIM tutorial slides
provided by Margaret Goodrich for user instructions.

Four instance files were validated against the profile in CIMTool. In
order to generate them, we use a current version of OpenDSS with the
*Export CDPSMcombined* command on four IEEE test feeders that come with
OpenDSS:

1. **~/src/opendss/Test/IEEE13\_CDPSM.dss** is the IEEE 13-bus test
   feeder with per-length phase impedance matrices and a delta tertiary
   added to the substation transformer.

2. **~/src/opendss/Test/IEEE13\_Assets.dss** is the IEEE 13-bus test
   feeder with catalog data for overhead lines, cables and transformers.
   Capacitor controls have also been added.

3. **~/src/opendss/Distrib/IEEETestCases/8500-Node/Master.dss** is the
   IEEE 8500-node test feeder with balanced secondary loads.

4. **~/src/opendss/Distrib/IEEETestCases/8500-Node/Master-unbal.dss** is
   the IEEE 8500-node test feeder with unbalanced secondary loads.

Either the 3\ :sup:`rd` or 4\ :sup:`th` feeder will be used for the
volt-var application. The 1\ :sup:`st` and 2\ :sup:`nd` feeders are used
to validate more parts of the CIM profile used in RC1. In all four
cases, CIMTool reports only two kinds of validation error:

1. **Isolated connectivity node**: CIMTool expects two or more Terminals
   per ConnectivityNode, but dead ended feeder segments will have only
   one on the last node. This is not really an error, at least for
   distribution systems.

2. **Minimum cardinality**: For TapChangerControl instances, the
   inherited RegulatingControl.RegulatingCondEq association is not
   specified. This is not really an error, as the association is only
   needed for shunt capacitor controls. Figure 36 shows that
   RegulatingCondEq was not selected for TapChangerControl in the
   profile, so this may reflect a defect in the validation code. Efforts
   to circumvent it were not successful.

With these caveats, the profile and instances validate against each
other, for feeder models that solve in OpenDSS.

Legacy Data Definition Language (DDL) for MySQL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As shown at the top of Figure 36, CIMTool builds *RC1.sql* to create
tables in a relational database, but the syntax doesn’t match that
required for MySQL. The following manual edits were made:

1.  Globally change **CHAR VARYING(30)** to **varchar(50)** with a blank
    space pre-pended before the varchar

2.  Globally change **“** to **\`**

3.  In foreign keys to enumerations, change the referenced attribute
    from **mRID** to **name**

4.  In foreign keys to **EquipmentContainer** or
    **ConnectivityNodeContainer**, change the referenced table to
    **Line**

5.  In foreign keys to **ShuntCompensator**, change the referenced table
    to **LinearShuntCompensator**

6.  In foreign keys to **TapChanger**, change the referenced table to
    **RatioTapChanger**.

7.  The CIM UML incorporates several polymorphic associations, which
    can’t be implemented directly in SQL. Base parent class tables were
    added for:

    a. **AssetInfo**, which can be referenced via the Parent attribute
       from ConcentricNeutralCableInfo, TapeShieldCableInfo,
       OverheadWireInfo, WireSpacingInfo, TapChangerInfo and
       TransformerTankInfo

    b. **TransformerEnd**, which can be referenced via the Parent
       attribute from PowerTransformerEnd and TransformerTankEnd

    c. **PerLengthImpedance**, which can be referenced via the Parent
       attribute from PerLengthSequenceImpedance and
       PerLengthPhaseImpedance

    d. **Switch**, which can be referenced via the SwtParent attribute
       from Breaker, Fuse, Sectionaliser, Recloser, Disconnector, Jumper
       and LoadBreakSwitch.

    e. **ConductingEquipment**, which can be referenced via the Parent
       attribute from ACLineSegment, EnergySource, EnergyConsumer,
       LinearShuntCompensator, PowerTransformer, and all of the Switch
       types.

8.  The catalog data mechanism in Figure 8 required two new tables, one
    for polymorphic associations and another for many-to-many joins:

    a. **PowerSystemResource**, which can be referenced via the PSR
       attribute from ACLineSegment, ACLineSegmentPhase, RatioTapChanger
       and TransformerTank.

    b. **AssetInfoJoin**, which references AssetInfo and
       PowerSystemResource. This table actually supplants the Asset
       class in Figure 8.

9.  The ShortCircuitTest in Figure 9 has a one-to-many association to
    TransformerEndEnfo, and we need to implement the many side by
    adding:

    a. **GroundedEndJoin**, which references TransformerEndInfo and
       ShortCircuitTest.

10. The ToTransformerEnd association in Figure 6 is one-to-many, so
    CIMTool did not export it to SQL. Rather than create a join table, a
    ToTransformerEnd attribute was added to TransformerMeshImpedance.
    This supports only one-to-one association, which is justified
    because the one-to-many case is very rare, and GridLAB-D cannot
    model transformers having the one-to-many association. This
    restriction may be removed in future versions having a semantic or
    graph database.

    Except for the first two items, all of these adjustments arose from
    the absence of inheritance or polymorphism in SQL. These adjustments
    will make the updates, queries and views more complicated. However,
    they allow referential integrity to be enforced, which is one of the
    most important reasons to use SQL and relational databases. Other
    types of data store could be a more natural fit to the CIM UML, but
    they may not have the performance of a relational database.

    In GitHub:

1. *RC1.sql* is the manually adjusted SQL export from CIMTool

2. *LoadRC1.sql* will **re-create the GridAPPS-D database in MySQL**,
   incorporate *RC1.sql*, and finally document the foreign keys. It
   should run without error.

.. [1]
   See http://cimug.ucaiug.org/default.aspx and the EPRI CIM Primer at:
   http://www.epri.com/abstracts/Pages/ProductAbstract.aspx?ProductId=000000003002006001

.. [2]
   Suggest “Corporate Edition” from http://www.sparxsystems.com/ for
   working with CIM UML. The free CIMTool is still available at
   http://wiki.cimtool.org/index.h tml, but support is being phased out.

.. [3]
   OSPRREYS is an older name for GridAPPS-D

.. [4]
   https://github.com/GRIDAPPSD/Powergrid-Models/CIM

.. |imgcim0| image:: CDPSM/media/cim_FeederContext.png
.. |imgcim1| image:: CDPSM/media/cim_LineModel.png
.. |imgcim2| image:: CDPSM/media/cim_LoadsAndSources.png
.. |imgcim3| image:: CDPSM/media/cim_SwitchingEquipment.png
.. |imgcim4| image:: CDPSM/media/cim_CapacitorClass.png
.. |imgcim5| image:: CDPSM/media/cim_Transformer.png
.. |imgcim6| image:: CDPSM/media/cim_TapChangerClass.png
.. |imgcim7| image:: CDPSM/media/cim_AssetInfoOverview.png
.. |imgcim8| image:: CDPSM/media/cim_DCIMTransformerInfo.png
.. |imgcim9| image:: CDPSM/media/cim_DCIMWireInfo.png
.. |imgcim10| image:: CDPSM/media/cim_StateVariables.png
.. |imgcim11| image:: CDPSM/media/cim_BusNavigation.png
.. |imgcim12| image:: CDPSM/media/cim_ConductingEquipmentContexts.png
.. |imgcim13| image:: CDPSM/media/cim_Capacitors.png
.. |imgcim14| image:: CDPSM/media/cim_Autotransformer.png
.. |imgcim15| image:: CDPSM/media/cim_PowerTransformerNavigation.png
.. |imgcim16| image:: CDPSM/media/cim_PowerTransformerImpedance.png
.. |imgcim17| image:: CDPSM/media/cim_OpenWyeOpenDelta.png
.. |imgcim18| image:: CDPSM/media/cim_TankNavigation.png
.. |imgcim19| image:: CDPSM/media/cim_TankImpedance.png
.. |imgcim20| image:: CDPSM/media/cim_LinePhaseNavigation.png
.. |imgcim21| image:: CDPSM/media/cim_SwitchPhaseNavigation.png
.. |imgcim22| image:: CDPSM/media/cim_LineInstance.png
.. |imgcim23| image:: CDPSM/media/cim_LineSequence.png
.. |imgcim24| image:: CDPSM/media/cim_LineMatrix.png
.. |imgcim25| image:: CDPSM/media/cim_LineAssetInfo.png
.. |imgcim26| image:: CDPSM/media/cim_LineCatalog.png
.. |imgcim27| image:: CDPSM/media/cim_Loads.png
.. |imgcim28| image:: CDPSM/media/cim_TapChanger.png
.. |imgcim29| image:: CDPSM/media/cim_VoltageValues.png
.. |imgcim30| image:: CDPSM/media/cim_TapValues.png
.. |imgcim31| image:: CDPSM/media/cim_CapacitorValues.png
.. |imgcim33| image:: CDPSM/media/ext_TroubleCalls.png
.. |imgcim35| image:: CDPSM/media/cim_CIMTool.png
.. |imgfaults| image:: CDPSM/media/cim_Faults.png
.. |imginverters| image:: CDPSM/media/cim_PowerElectronics.png
.. |imgmachines| image:: CDPSM/media/cim_DERMachines.png
.. |imghouses| image:: CDPSM/media/ext_Houses.png
.. |imgmeas| image:: CDPSM/media/cim_MeasurementGRIDAPPSD.png

