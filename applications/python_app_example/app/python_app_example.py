from datetime import datetime
import os
import sys
import time

import json
import stomp
import yaml
from copy_reg import constructor


class PythonAppExample(object):
    """
    The PythonAppExample class is registered with GridAPPS-D outside of the
    class in the main function. The PythonAppExample class will demonstrate
    the following:
        1.) How to subscribe to output coming from the real world sensor 
        measurements.
        2.) How to send commands to the real world objects.
        3.) How to start an offline simulation of the distribution model.
        4.) How to retrieve output from offline simulations.
        5.) How to send custom queries and receive the results.
    """
    def __init__(self):
        """ 
            constructor
        """    
        pass
    
    
    def query_for_real_world_id(self):
        """ function for querying for the id of the real world simulation.
            
            Arguments: None.
            Returns: 
                real_world_id:
                    Type: int.
                    Description: The id of the real world simulation.
                    Default: None.
            Exceptions: None.
        """
        pass
    
    
    def query_for_measurements(self, model_id):
        """ function for querying for measurement object in the CIM.
        
        Arguments: 
            model_id:
                Type: int.
                Description: id for the CIM model to query.
                Default: None.
        Returns: 
            measurement_results:
                Type: dict.
                Description: a dictionary containing all the measurement mrid
                    as keys. the dictionary will store object type, 
                    object property, measurement unit for the value.
                Default: None.
        Exceptions: None.
        """
        pass
    
    
    