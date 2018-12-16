Model Validation Application
----------------------------

The state estimator basically attempts to fit measured data to a power
flow model, usually assuming that the model is correct. However, a model
attribute (e.g. line impedance) could also be estimated by minimizing
its error residual in the state estimator’s power flow solution. This
process works best when applied to just one or a few suspect attributes,
and/or when an archive is available to provide enough redundant
measurements. The Model Validation Application will use these state
estimator features off-line to help identify and correct the following
types of model errors:

1. Unknown or incorrect service transformer sizes

2. Unknown or incorrect secondary circuit lengths

3. Incorrect phase identification of single-phase components

4. Phase wiring errors in line segments and switches

5. Transformer connection errors, especially reversed primary and
   secondary

6. Primary conductor sizes that don’t decrease monotonically with
   distance from the source

7. Missing regulator and capacitor control settings (i.e. supply
   defaults from heuristic rules)

8. More than one of these on the same pole: recloser, line regulator,
   capacitor

9. Substation transformer impedance and turns ratio

These types of errors often appear upon the initial model import from a
geographic information system (GIS), or in periodic model updates from
GIS. Other error types may be added later. Many utilities do not have
their secondary circuits modeled at all, but this has an important
impact on AMI data. The service transformers and secondary circuits
insert significant impedance between AMI meters and the primary circuit,
where most of the other sensors are installed. Therefore, the first two
items will require AMI data, and also enable its more effective use.

As shown in Figure 1, the Model Validator integrates with GridAPPS-D as
a hosted application on the GOSS bus. Internally, it will use some of
the same algorithms as the State Estimator and may share some code or
binary files, but this is an implementation detail. It will need to
access an archive of state-estimated VIPQS data, which may include AMI
data. It will also use or incorporate an off-line power flow model, not
the same one running in the GridAPPS-D distribution simulator. This may
be EPRI’s OpenDSS simulator [1]; compared to GridLAB-D, it’s more
tolerant of model errors and provides more diagnostic information about
model errors.

|mv_image1|

Figure 1: The Model Validator works with an archive from the state
estimator, and an off-line power flow model.

Design Objectives
~~~~~~~~~~~~~~~~~

The model validator will detect and attempt to correct unreasonable 
component interconnections and network parameters.  The model validation 
application will be implemented in Python.  

Use Cases
~~~~~~~~~

-  Valid transformer size and orientation (Utility): orientation is not
   captured explicitly in their GIS system.

-  Discover secondary line impedance parameters (Utility) conductor type
   and line length are currently based on generic assumptions.

-  Sanity check or estimate transformer size and impedance.

-  Verify that the nominal voltage of nodes matches the base voltage of
   the segment: generally the winding voltage of the upstream
   transformer or swing bus voltage.

-  Sanity check conductor sizes and line current ratings.

-  Validate and fill in regulator and capacitor control settings.

-  Check phase continuity (GridLAB-D may not model phase
   discontinuities)

Inputs
~~~~~~

The model validator will have access to the CIM database and archived
data from the state estimator.

Outputs
~~~~~~~

The model validator will one or both of the following outputs:

-  Model status: log file or GUI pipe for identified issues.

-  Model correction: CIM updates to correct identified issues.

Testing and Validation
~~~~~~~~~~~~~~~~~~~~~~

**Evaluation metrics**

-  Ability to detect known issues.

**Scenarios**

-  Utility merger: models with different format may be interpreted
   differently, creating issues a CIM model.

-  Data entry issue: model update does not match upgrade performed in
   the field

Operating/Running
~~~~~~~~~~~~~~~~~

The model validator script will execute once when called by the
platform.

At initialization, a configuration file will be read for:

-  Mode (status, quiet, verbose; see outputs section)

-  Selectable validation items (use cases)

References
~~~~~~~~~~

[1] R. C. Dugan and T. E. McDermott, "An open source platform for collaborating on smart grid research," in *Power and Energy Society General Meeting, 2011 IEEE*, 2011, pp. 1-7.

.. |mv_image1| image:: PNNL_Apps/media/MV_App.png

