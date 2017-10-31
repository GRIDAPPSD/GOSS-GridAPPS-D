'''
Created on Oct 24, 2017

@author: thay838
'''
# Costs
COSTS = {'energy': 0.00008, 'tapChange': 0.5, 'capSwitch': 2, 'volt': 0.05}
# Paths
BASE_PATH = r'\\pnl\projects\VVO-GridAPPS-D\pmaps\experiment\R2_12_47_2'
INCLUDE_DIR = r'E:/pmaps/experiment/include'
MODEL = 'R2_12_47_2'
# Define timing:
# Define recorder intervals (s)
RECORD_INT = 60
# Define model runtime (s)
MODEL_RUNTIME = 60 * 15
# Define AMI_averaging_interval (s)
AMI_INTERVAL = 60 * 15
# Define start/stop times for running the full model. Recent times just because
STARTTIME = '2016-01-01 00:00:00'
STOPTIME = '2017-01-01 00:00:00'
TIMEZONE = 'PST+8PDT' # Pulled right from tzinfo.txt

# Database for baseline.
BASELINE_DB = {'schema': 'baseline'}

# For now, we'll only measure voltages for residential meters (note that any 
# triplex meter with the commercial designation was switched to residential)
TRIPLEX_GROUP = 'AMI_group'

# Definition of regulators. 
# NOTE: Initial state not defined in original model
REG={'R2-12-47-2_reg_1': {
                          'raise_taps': 16, 
                          'lower_taps': 16,
                          'phases': {'A': {'prevState': 0},
                                     'B': {'prevState': 0},
                                     'C': {'prevState': 0}
                                    },
                         },
     'R2-12-47-2_reg_2': {
                          'raise_taps': 16, 
                          'lower_taps': 16,
                          'phases': {'A': {'prevState': 0},
                                     'B': {'prevState': 0},
                                     'C': {'prevState': 0}
                                    },
                         },
     }

# Definition of capacitors. 
# NOTE: Initial state not defined in original model. GridLAB-D defaults to
# OPEN (checked source code)
CAP={
    'R2-12-47-2_cap_1': {'phases': {
                                    'A': {'prevState': 'OPEN'},
                                    'B': {'prevState': 'OPEN'},
                                    'C': {'prevState': 'OPEN'}}},
    'R2-12-47-2_cap_2': {'phases': {
                                    'A': {'prevState': 'OPEN'},
                                    'B': {'prevState': 'OPEN'},
                                    'C': {'prevState': 'OPEN'}}},
    'R2-12-47-2_cap_3': {'phases': {
                                    'A': {'prevState': 'OPEN'},
                                    'B': {'prevState': 'OPEN'},
                                    'C': {'prevState': 'OPEN'}}},
    'R2-12-47-2_cap_4': {'phases': {
                                    'A': {'prevState': 'OPEN'},
                                    'B': {'prevState': 'OPEN'},
                                    'C': {'prevState': 'OPEN'}}},
    }