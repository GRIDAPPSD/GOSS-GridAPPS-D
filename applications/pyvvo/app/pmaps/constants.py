'''
Created on Oct 24, 2017

@author: thay838
'''
# Costs
# .25 per tap
# cap --> cheaper
# Maybe $1 per violation?
# Because we used ZIP, influence penalties based on what we know about loss of
# fidelity 
COSTS = {'realEnergy': 0.00008, 'reactiveEnergy': 0.00001,
         'powerFactorLead': {'limit': 0.99, 'cost': 0.1},
         'powerFactorLag': {'limit': 0.99, 'cost': 0.1},
         'tapChange': 0.5, 'capSwitch': 2, 'undervoltage': 0.05,
         'overvoltage': 0.05}
# Paths
#"""
BASE_PATH = r'C:\Users\thay838\git_repos\GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\models'.replace('\\', '/')
INCLUDE_DIR = r'C:\Users\thay838\git_repos\GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\models\include'.replace('\\', '/')
ZIP_DIR = r'C:\Users\thay838\git_repos\GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\zip'.replace('\\', '/')
OUTPUT_DIR = r'C:\Users\thay838\git_repos\GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\output'.replace('\\', '/')
#"""
"""
BASE_PATH = r'/home/thay838/GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\models'.replace('\\', '/')
INCLUDE_DIR = r'/home/thay838/GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\models\include'.replace('\\', '/')
ZIP_DIR = r'/home/thay838/GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\zip'.replace('\\', '/')
OUTPUT_DIR = r'/home/thay838/GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\output'.replace('\\', '/')
"""
MODEL = 'R2_12_47_2'
# Define timing:
# Define recorder intervals (s)
RECORD_INT = 60
# Define model runtime for genetic algorithm (s)
MODEL_RUNTIME = 60 * 15
# Define AMI_averaging_interval (s)
AMI_INTERVAL = 60 * 15
# Define the ZIP modeling interval (s)
ZIP_INTERVAL = 60 * 60
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
# NOTE: Initial states come from running the baseline model (tmy2) from
# '2016-01-01 00:00:00' to '2016-01-01 01:00:00' and getting the final setpoints
REG={'R2-12-47-2_reg_1': {
                          'raise_taps': 16, 
                          'lower_taps': 16,
                          'phases': {'A': {'prevState': -10},
                                     'B': {'prevState': -15},
                                     'C': {'prevState': -13}
                                    },
                         },
     'R2-12-47-2_reg_2': {
                          'raise_taps': 16, 
                          'lower_taps': 16,
                          'phases': {'A': {'prevState': -1},
                                     'B': {'prevState': -1},
                                     'C': {'prevState': -2}
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

if __name__ == '__main__':
    try:
        raise UserWarning('warning!')
    except UserWarning as w:
        print(w)
        print('2 + 2 = {}'.format(2+2))