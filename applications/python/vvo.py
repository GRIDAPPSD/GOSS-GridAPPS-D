#-------------------------------------------------------------------------------
# Name:        VVO Application
# Purpose:
#
# Author:      liuy525
#
# Created:     23/02/2017
# Copyright:   (c) liuy525 2017
# Licence:     <your licence>
#-------------------------------------------------------------------------------

import cmath
import math
import copy
import json
import logging

class VoltVarControl():

    def __init__(self, VVO_static_dict, VVO_message_dict, outputfn):

        ##########################
        ## Initialize variables ##
        ##########################

        # Static Configuration and Dynamic Message
        self.VVC_static = VVO_static_dict
        self.VVC_message = VVO_message_dict
        self.outputfn = outputfn


        # Dict #
        self.VVC = {}
        self.RegConfig = {}
        self.RegTap = {}
        self.CapConfig = {}
        self.CapState = {}
        self.MeasNodes = {} # Initialize voltage measurement inputs in dict - 'sensor_name': [complex_volt_A, complex_volt_B, complex_volt_C]
        self.RegToNodes = {} # Initialize regulator to-side voltages
        self.SubLink = {}

        self.OutputDict = {}

        self.RegTap_ChangeFlag = {}

        # Regulator #
        self.RegList = []  # a sequential list of regulators
        self.RegConfigList = []  # a sequential list of regulator configuration
        self.num_regs = 0  # Number of regulators under our control
        self.reg_step_up = []  # Regulator step for upper taps, associated with num_regs, idx = num_regs
        self.reg_step_down = []  # Regulator step for lower taps, (may be same as reg_step_up), idx = num_regs
        self.RegUpdateTimes = []  # Regulator progression times (differential), relative time, idx = num_regs
##        self.PrevRegState = [] # sequential list of regulators' previous states: #['MANUAL', 'OUTPUT_VOLTAGE', 'REMOTE_NODE', 'LINE_DROP_COMP'], idx = num_regs
        self.Regulator_Change = True  # regulator change flag
        self.TUpdateStatus = True  # flag for control method switch of the voltage regulator

        # Capacitor #
        self.CapList = [] # a sequential list of capacitors
        self.num_caps = 0   # Number of capacitors under our control
        self.CapUpdateTimes = [] # Capacitor progression times (differential), relative time, idx = num_cap
##        self.PrevCapState = [] # sequential list of capacitors' previous states: # ['MANUAL', 'VAR', 'VOLT', 'VARVOLT', 'CURRENT'], idx = num_caps

        # Measurements #
        self.num_meas = []  # Number of voltage measurements to monitor, list num_meas for each reg, idx = num_regs
        self.MeasList = []  # list of measurement sensor names, [[sensor_name list for reg1], [sensor_name list for reg2]] = [[sensor_name1, sensor_name2...], [], [], []...]
        self.MeasPhases = []  # list of phase connections for sensors associated with each regulator, 'ABC' 'AC' 'BC' 'C', similar to MeasList, [['ABC', 'BC'...], [], [], []...]

        # Timestamp
        self.TS_NEVER = 9e999  # Infinite timestamp
        self.TRegUpdate = []  # Initialize absolute tap change time for each regulator
        self.TCapUpdate = 0.0  # Capacitor state update time, Notice: it is a double instead of a list  !!
##        self.prev_time = 0

        # Other
##        self.first_cycle = True  # if it is the first cycle of simulation
##        self.prev_mode = 'ACTIVE'    # 'ACTIVE','STANDBY'
        self.pf_signed = False  # Flag to indicate if a signed pf value should be maintained, or just a "deadband around 1"
##        self.pf_phase = 'ABC'  # Phases for power factor monitoring to occur, could be 'AB', 'BC', 'C'...
        self.react_pwr = 1e4  # Reactive power quantity at the substation
        self.curr_pf = 0.95   # Current pf at the substation
        self.simulation_name = 'sim1'  # simulation identifier
        self.changed_cap = ''  # Name of the capacitor that has changed its state in current time step

        self.log = logging.getLogger('vvo')

        ###############################################
        ## Default Dicts (Configuration and Dynamic) ##
        ###############################################

##        self.VVC = {
##        'control_method' : 'ACTIVE',                   # ['ACTIVE','STANDBY']
##        'capacitor_delay': 60.0,                     # Default delay for capacitors
##        'regulator_delay': 60.0,                    # Default delay for regulator
##        'desired_pf': 0.99,                       # Desired power factor for the system
##        'd_max': 0.9,                              # Scaling constant for capacitor switching on - typically 0.3 - 0.6
##        'd_min': 0.1,                              # Scaling constant for capacitor switching off - typically 0.1 - 0.4
##        'substation_link': 'xf_hvmv_sub',
##        'regulator_list': ['reg_FEEDER_REG','reg_VREG2','reg_VREG3','reg_VREG4'],             # List of regulators, separated by commas
##        'regulator_configuration_list': ["rcon_FEEDER_REG", "rcon_VREG2", "rcon_VREG3", "rcon_VREG4"],       # List of regulator configurations
##        'capacitor_list': ['cap_capbank0a', 'cap_capbank0b', 'cap_capbank0c', 'cap_capbank1a', 'cap_capbank1b', 'cap_capbank1c', 'cap_capbank2a', 'cap_capbank2b', 'cap_capbank2c', 'cap_capbank3'],    # List of controllable capacitors, separated by commas
##        'voltage_measurements': ['nd_l2955047,1','nd_l3160107,1','nd_l2673313,2','nd_l2876814,2','nd_m1047574,3','nd_l3254238,4'],
##        # For example, 'L2955047,1' indicates sensor 'L2955047' is communicated with reg.# 1, which is 'FEEDER_REG'
##        'maximum_voltages': [7500, 7500, 7500, 7500],              # Minimum allowable voltage of the system
##        'minimum_voltages': [6500, 6500, 6500, 6500],
##        'max_vdrop': [5200, 5200, 5200, 5200],
##        'high_load_deadband': [100, 100, 100, 100],
##        'desired_voltages': [7000, 7000, 7000, 7000],             # List of desired, or target voltages for the volt_var_control object to maintain.
##        'low_load_deadband': [100, 100, 100, 100],
##        'pf_phase': 'ABC'
##        }
##
##        self.RegConfig = { # in this case, four regulators
##        'connect_type': ['1', '1', '1', '1'],
##        'control': ['MANUAL', 'MANUAL', 'MANUAL', 'MANUAL'],   # 'MANUAL', 'OUTPUT_VOLTAGE', 'REMOTE_NODE', 'LINE_DROP_COMP'
##        'control_level': ['INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL'],   # 'INDIVIDUAL','BANK'
##        'PT_phase' : ['ABC', 'ABC', 'ABC', 'ABC'],
##        'band_center' : [7500, 7500, 7500, 7500], # associated with num_regs, V
##        'band_width' : [120, 120, 120, 120], # V
##        'regulation' : [0.1, 0.1, 0.1, 0.1], # pu, 10%
##        'raise_taps': [16, 16, 16, 16],
##        'lower_taps': [16, 16, 16, 16],
##        'dwell_time' : [60, 60, 60, 60],
##        # The below should belong to regulator properties, but be moved to regulator_configuration properties
##        'phases' : ['ABC', 'ABC', 'ABC', 'ABC'],
##        'to' : ['nd__hvmv_sub_lsb', 'nd_190-8593', 'nd_190-8581', 'nd_190-7361']
##        }
##
##        self.CapConfig = {
##        'phases' : ['AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'ABCN'],
##        'pt_phase' :  ['A', 'B', 'C', 'A', 'B', 'C', 'A', 'B', 'C', 'ABC'],
##        'phases_connected' :  ['AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'ABCN'],
##        'control': ['MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL'],   # 'MANUAL', 'VAR', 'VOLT', 'VARVOLT', 'CURRENT'
##        'control_level' : ['INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'BANK'],   # 'INDIVIDUAL','BANK'
##        'cap_size' : [4e5, 3e5, 3e5, 3e5, 4e5, 3e5, 3e5, 3e5, 3e5, 3e5],
##        'dwell_time' : [480, 300, 180, 60, 480, 300, 180, 60, 30, 40]
##        }
##
##
##        self.RegTap = {
##        'FEEDER_REG': [1, -1, 2], # [TapA, TapB, TapC]
##        'VREG2': [2, -2, 3 ],
##        'VREG3': [3, -3, 4 ],
##        'VREG4': [4, -4, 5 ]
##        }
##
##        self.CapState = {
##        'cap_capbank0a' : {'switchA': 'OPEN'},
##        'cap_capbank0b' : {'switchB': 'OPEN'},
##        'cap_capbank0c' : {'switchC': 'OPEN'},
##        'cap_capbank1a' : {'switchA': 'OPEN'},
##        'cap_capbank1b' : {'switchB': 'OPEN'},
##        'cap_capbank1c' : {'switchC': 'OPEN'},
##        'cap_capbank2a' : {'switchA': 'OPEN'},
##        'cap_capbank2b' : {'switchB': 'OPEN'},
##        'cap_capbank2c' : {'switchC': 'OPEN'},
##        'cap_capbank3' : {'switchA': 'OPEN', 'switchB': 'OPEN', 'switchC': 'OPEN'}
##        }
##
##        self.SubLink = {
##        'xf_hvmv_sub': [10000 + 10000j, 10000 + 10000j, 10000 + 10000j]  # W + j VAr for phase A, B, C
##        }
##
##        self.MeasNodes = {
##            'nd_l2955047': {'voltage_A': 7000.91 + 0.9j, 'voltage_B': 7031.07 + 0.0j, 'voltage_C': 7011.013 + 0.1j },
##            'nd_l2673313': {'voltage_A': 7010.92 + 0.9j, 'voltage_B': 7031.07 + 0.0j, 'voltage_C': 7041.018 + 0.1j },
##            'nd_l3160107': {'voltage_A': 7020.93 + 0.9j, 'voltage_B': 7021.09 + 0.0j, 'voltage_C': 7021.014 + 0.1j },
##            'nd_l2876814': {'voltage_A': 7000.94 + 0.9j, 'voltage_B': 7001.01 + 0.0j, 'voltage_C': 7011.013 + 0.1j },
##            'nd_l3254238': {'voltage_A': 7011.05 + 0.0j, 'voltage_B': 7011.01 + 0.1j, 'voltage_C': 7011.012 + 0.1j },
##            'nd_m1047574': {'voltage_A': 7021.01 + 0.1j, 'voltage_B': 7021.09 + 0.0j, 'voltage_C': 7021.091 + 0.0j }
##        }  # This is a non-sequential list of measurement inputs
##            # Need to be stored in the same sequence with BasicConfig['voltage_measurements'] to facilitate operation
##
##        self.RegToNodes = {
##				'nd__hvmv_sub_lsb': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],   # voltage_A, voltage_B, voltage_C     Note: Regulator to-side must have 3-phase voltages
##				'nd_190-8593': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],
##				'nd_190-8581': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],
##				'nd_190-7361': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ]
##        }

        ####################################################
        ## End Default Dicts (Configuration and Dynamic)  ##
        ####################################################



        #######################################
        ## Initialize Configuration dicts ##
        #######################################

        if self.VVC_static.keys() != self.VVC_message.keys():
            raise ValueError('Simulation names mismatch for VVC static configuration and dynamic message!')

        self.simulation_name = self.VVC_static.keys()[0]

        # Extract VVC configuration from static input dictionary
        self.VVC['control_method'] = self.VVC_static[self.simulation_name]['control_method']
        self.VVC['capacitor_delay'] = self.VVC_static[self.simulation_name]['capacitor_delay']
        self.VVC['regulator_delay'] = self.VVC_static[self.simulation_name]['regulator_delay']
        self.VVC['desired_pf'] = self.VVC_static[self.simulation_name]['desired_pf']
        self.VVC['d_max'] = self.VVC_static[self.simulation_name]['d_max']
        self.VVC['d_min'] = self.VVC_static[self.simulation_name]['d_min']
        self.VVC['substation_link'] = self.VVC_static[self.simulation_name]['substation_link']
        self.VVC['regulator_list'] = self.VVC_static[self.simulation_name]['regulator_list']
        self.VVC['regulator_configuration_list'] = self.VVC_static[self.simulation_name]['regulator_configuration_list']
        self.VVC['capacitor_list'] = self.VVC_static[self.simulation_name]['capacitor_list']
        self.VVC['voltage_measurements'] = self.VVC_static[self.simulation_name]['voltage_measurements']
        self.VVC['maximum_voltages'] = self.VVC_static[self.simulation_name]['maximum_voltages']
        self.VVC['minimum_voltages'] = self.VVC_static[self.simulation_name]['minimum_voltages']
        self.VVC['max_vdrop'] = self.VVC_static[self.simulation_name]['max_vdrop']
        self.VVC['high_load_deadband'] = self.VVC_static[self.simulation_name]['high_load_deadband']
        self.VVC['desired_voltages'] = self.VVC_static[self.simulation_name]['desired_voltages']
        self.VVC['low_load_deadband'] = self.VVC_static[self.simulation_name]['low_load_deadband']
        self.VVC['pf_phase'] = self.VVC_static[self.simulation_name]['pf_phase']

        # Extract regulator list, regulator configuration list and capacitor list
        self.RegList = self.VVC['regulator_list']
        self.RegConfigList = self.VVC['regulator_configuration_list']
        self.CapList = self.VVC['capacitor_list']

        # Count the number of regulators
        self.num_regs = len(self.VVC['regulator_list'])  # Number of regulators under our control

        # Update VVC configuration based on input dict, make sure inside this python script, the below configuration is related to regulator number
        if type(self.VVC['maximum_voltages']) is not list:
            self.VVC['maximum_voltages'] = [self.VVC['maximum_voltages']] * self.num_regs
        elif type(self.VVC['maximum_voltages']) is list and len(self.VVC['maximum_voltages']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.VVC['minimum_voltages']) is not list:
            self.VVC['minimum_voltages'] = [self.VVC['minimum_voltages']] * self.num_regs
        elif type(self.VVC['minimum_voltages']) is list and len(self.VVC['minimum_voltages']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.VVC['max_vdrop']) is not list:
            self.VVC['max_vdrop'] = [self.VVC['max_vdrop']] * self.num_regs
        elif type(self.VVC['max_vdrop']) is list and len(self.VVC['max_vdrop']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.VVC['high_load_deadband']) is not list:
            self.VVC['high_load_deadband'] = [self.VVC['high_load_deadband']] * self.num_regs
        elif type(self.VVC['high_load_deadband']) is list and len(self.VVC['high_load_deadband']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.VVC['desired_voltages']) is not list:
            self.VVC['desired_voltages'] = [self.VVC['desired_voltages']] * self.num_regs
        elif type(self.VVC['desired_voltages']) is list and len(self.VVC['desired_voltages']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.VVC['low_load_deadband']) is not list:
            self.VVC['low_load_deadband'] = [self.VVC['low_load_deadband']] * self.num_regs
        elif type(self.VVC['low_load_deadband']) is list and len(self.VVC['low_load_deadband']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        # Initialize regulator configuration, should be related to number of regulators
        self.RegConfig['connect_type'] = [''] * self.num_regs
        self.RegConfig['control'] = [''] * self.num_regs
        self.RegConfig['control_level'] = [''] * self.num_regs
        self.RegConfig['PT_phase']= [''] * self.num_regs
        self.RegConfig['band_center'] = [0] * self.num_regs
        self.RegConfig['band_width'] = [0] * self.num_regs
        self.RegConfig['regulation'] = [0] * self.num_regs
        self.RegConfig['raise_taps']= [0] * self.num_regs
        self.RegConfig['lower_taps'] = [0] * self.num_regs
        self.RegConfig['dwell_time']= [0] * self.num_regs
        # The below should belong to regulator properties, but be moved to regulator_configuration properties
        self.RegConfig['phases'] = [''] * self.num_regs
        self.RegConfig['to'] = [''] * self.num_regs
        # Extract and update regulator configuration
        for reg_index in range(self.num_regs):
            self.RegConfig['connect_type'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['connect_type']
            self.RegConfig['control'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['Control']  # Note the message is using uppercase Control
            self.RegConfig['control_level'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['control_level']
            self.RegConfig['PT_phase'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['PT_phase']
            self.RegConfig['band_center'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['band_center']
            self.RegConfig['band_width'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['band_width']
            self.RegConfig['regulation'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['regulation']
            self.RegConfig['raise_taps'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['raise_taps']
            self.RegConfig['lower_taps'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['lower_taps']
            self.RegConfig['dwell_time'][reg_index] = self.VVC_message[self.simulation_name][self.RegConfigList[reg_index]]['dwell_time']
            # The below should belong to regulator properties, but be moved to regulator_configuration properties
            self.RegConfig['phases'][reg_index] = self.VVC_message[self.simulation_name][self.RegList[reg_index]]['phases']
            self.RegConfig['to'][reg_index] = self.VVC_message[self.simulation_name][self.RegList[reg_index]]['to']
            # Check validity
            if self.RegConfig['control'][reg_index] != 'MANUAL':
                raise ValueError('At least one regulators are not in MANUAL control. Simulation aborted')


        # Count the number of capacitors
        self.num_caps = len(self.VVC['capacitor_list'])   # Number of capacitors under our control

        # Initialize capacitor configuration, should be related to number of capacitors
        self.CapConfig['phases'] = [''] * self.num_caps
        self.CapConfig['pt_phase'] = [''] * self.num_caps
        self.CapConfig['phases_connected'] = [''] * self.num_caps
        self.CapConfig['control']= [''] * self.num_caps
        self.CapConfig['control_level'] = [''] * self.num_caps
        self.CapConfig['cap_size'] = [0] * self.num_caps
        self.CapConfig['dwell_time'] = [0] * self.num_caps

        # Extract and update capcacitor configuration
        for cap_index in range(self.num_caps):
            self.CapConfig['phases'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['phases']
            self.CapConfig['pt_phase'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['pt_phase']
            self.CapConfig['phases_connected'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['phases_connected']
            self.CapConfig['control'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['control']
            self.CapConfig['control_level'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['control_level']
            self.CapConfig['dwell_time'][cap_index] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['dwell_time']

            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('capacitor_A'):
                self.CapConfig['cap_size'][cap_index] = self.CapConfig['cap_size'][cap_index] + self.VVC_message[self.simulation_name][self.CapList[cap_index]]['capacitor_A']

            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('capacitor_B'):
                self.CapConfig['cap_size'][cap_index] = self.CapConfig['cap_size'][cap_index] + self.VVC_message[self.simulation_name][self.CapList[cap_index]]['capacitor_B']

            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('capacitor_C'):
                self.CapConfig['cap_size'][cap_index] = self.CapConfig['cap_size'][cap_index] + self.VVC_message[self.simulation_name][self.CapList[cap_index]]['capacitor_C']

                        # Check validity
            if self.CapConfig['control'][cap_index] != 'MANUAL':
                raise ValueError('At least one capacitors are not in MANUAL control. Simulation aborted.')

        # sort capacitors based on size (from largest to smallest) - assumes they are banked in operation (only 1 size provided)
        temp_CapBankSize = copy.deepcopy(self.CapConfig['cap_size'])

        MaxValue = 0
        temp_pos = 0
        temp_value = None
        for i1 in range(len(temp_CapBankSize)):
            MaxValue = temp_CapBankSize[i1]
            for i2 in range(i1, len(temp_CapBankSize)):
                if temp_CapBankSize[i2] >= MaxValue:
                    MaxValue = temp_CapBankSize[i2]
                    temp_pos = i2
            temp_value = temp_CapBankSize[temp_pos]
            temp_CapBankSize[temp_pos] = temp_CapBankSize[i1]
            temp_CapBankSize[i1] = temp_value

            temp_value = self.VVC['capacitor_list'][temp_pos]
            self.VVC['capacitor_list'][temp_pos] = self.VVC['capacitor_list'][i1]
            self.VVC['capacitor_list'][i1] = temp_value

            temp_value = self.CapConfig['phases'][temp_pos]
            self.CapConfig['phases'][temp_pos] = self.CapConfig['phases'][i1]
            self.CapConfig['phases'][i1] = temp_value

            temp_value = self.CapConfig['pt_phase'][temp_pos]
            self.CapConfig['pt_phase'][temp_pos] = self.CapConfig['pt_phase'][i1]
            self.CapConfig['pt_phase'][i1] = temp_value

            temp_value = self.CapConfig['phases_connected'][temp_pos]
            self.CapConfig['phases_connected'][temp_pos] = self.CapConfig['phases_connected'][i1]
            self.CapConfig['phases_connected'][i1] = temp_value

            temp_value = self.CapConfig['control'][temp_pos]
            self.CapConfig['control'][temp_pos] = self.CapConfig['control'][i1]
            self.CapConfig['control'][i1] = temp_value

            temp_value = self.CapConfig['control_level'][temp_pos]
            self.CapConfig['control_level'][temp_pos] = self.CapConfig['control_level'][i1]
            self.CapConfig['control_level'][i1] = temp_value

            temp_value = self.CapConfig['cap_size'][temp_pos]
            self.CapConfig['cap_size'][temp_pos] = self.CapConfig['cap_size'][i1]
            self.CapConfig['cap_size'][i1] = temp_value

            temp_value = self.CapConfig['dwell_time'][temp_pos]
            self.CapConfig['dwell_time'][temp_pos] = self.CapConfig['dwell_time'][i1]
            self.CapConfig['dwell_time'][i1] = temp_value

        # Update capacitor list
        self.CapList = self.VVC['capacitor_list']




    def Input(self, VVO_message_dict):

        self.VVC_message = VVO_message_dict



        #######################################
        ## Initialize Dynamic dicts ##
        #######################################


        # Initialize regulator tap dict
        for reg_index in range(self.num_regs):
            self.RegTap[self.RegList[reg_index]] = [0] * 3        # 3-phase taps
        # Update regulator taps
        for reg_index in range(self.num_regs):
            self.RegTap[self.RegList[reg_index]][0] = self.VVC_message[self.simulation_name][self.RegList[reg_index]]['tap_A']
            self.RegTap[self.RegList[reg_index]][1] = self.VVC_message[self.simulation_name][self.RegList[reg_index]]['tap_B']
            self.RegTap[self.RegList[reg_index]][2] = self.VVC_message[self.simulation_name][self.RegList[reg_index]]['tap_C']


        # Initialize regulator to-side voltage dict
        for reg_index in range(self.num_regs):
            self.RegToNodes[self.RegConfig['to'][reg_index]] = [0] * 3     # 3-phase voltages
        # Update regulator to-side voltages
        for reg_index in range(self.num_regs):   # regulator to-side must have 3-phase voltages
            self.RegToNodes[self.RegConfig['to'][reg_index]][0] = complex( self.VVC_message[self.simulation_name][self.RegConfig['to'][reg_index]]['voltage_A'][:-1].replace(' ','') )
            self.RegToNodes[self.RegConfig['to'][reg_index]][1] = complex( self.VVC_message[self.simulation_name][self.RegConfig['to'][reg_index]]['voltage_B'][:-1].replace(' ','') )
            self.RegToNodes[self.RegConfig['to'][reg_index]][2] = complex( self.VVC_message[self.simulation_name][self.RegConfig['to'][reg_index]]['voltage_C'][:-1].replace(' ','') )


        # Initialize capacitor state dict
        for cap_index in range(self.num_caps):
            self.CapState[self.CapList[cap_index]] = {}
        # Update capacitor states
        for cap_index in range(self.num_caps):
            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('switchA'):
                self.CapState[self.CapList[cap_index]]['switchA'] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['switchA']

            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('switchB'):
                self.CapState[self.CapList[cap_index]]['switchB'] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['switchB']

            if self.VVC_message[self.simulation_name][self.CapList[cap_index]].has_key('switchC'):
                self.CapState[self.CapList[cap_index]]['switchC'] = self.VVC_message[self.simulation_name][self.CapList[cap_index]]['switchC']


        # Initialize SubLink dict
        self.SubLink[self.VVC['substation_link']] = [0] * 3  # 3-phase power
        # Update substation_link power measurement
        self.SubLink[self.VVC['substation_link']][0] = complex( self.VVC_message[self.simulation_name][self.VVC['substation_link']]['power_in_A'][:-2].replace(' ','') )
        self.SubLink[self.VVC['substation_link']][1] = complex( self.VVC_message[self.simulation_name][self.VVC['substation_link']]['power_in_B'][:-2].replace(' ','') )
        self.SubLink[self.VVC['substation_link']][2] = complex( self.VVC_message[self.simulation_name][self.VVC['substation_link']]['power_in_C'][:-2].replace(' ','') )


        # Extract measurement data
        # Initialize num_meas, MeasList
        self.num_meas = [0] * self.num_regs
        self.MeasList = [[] for k in range(self.num_regs)]  # Very Important !!, How to initialize TWO-Dimention list !!
        # Don't use a=[[]] * 2 !! If used, try a[0].append('sasd') and you will find 'sasd' is also appended to a[1] !

        # Count the number of measurement sensors for each regulator
        # Record list of names of measurement sensors for each regulator
        for sensor_index in range(len(self.VVC['voltage_measurements'])):
            c = self.VVC['voltage_measurements'][sensor_index]
            reg_index = int(c[-1]) - 1
            self.num_meas[reg_index] += 1
            self.MeasList[reg_index].append(c[:-2])
            # Initialize MeasNodes dict
            self.MeasNodes[c[:-2]] = {}

        # Update measurement dict
        for MeasKeys in self.MeasNodes.keys():
            self.MeasNodes[MeasKeys] = self.VVC_message[self.simulation_name][MeasKeys]

        # convert "string" complex number into complex type
        for MeasKeys1 in self.MeasNodes.keys():
            for MeasKeys2 in self.MeasNodes[MeasKeys1].keys():
                if isinstance(self.MeasNodes[MeasKeys1][MeasKeys2], str):
                    self.MeasNodes[MeasKeys1][MeasKeys2] = complex( self.MeasNodes[MeasKeys1][MeasKeys2][:-1].replace(' ','') )

        # print self.MeasNodes
        # Record the connection phases of each measurement sensor in sequential order as BasicConfig['voltage_measurements']
        self.MeasPhases = copy.deepcopy(self.MeasList)  # initialize phases connection list to Measurement sensor names list because both are in the same structure
        # Very Important !!  Don't use list_a = list_b to copy list !! Once this is used, if list_a changes, list_b will also change

        for reg_index in range(len(self.MeasPhases)):
            for MeasIdx in range(len(self.MeasPhases[reg_index])):
                if self.MeasNodes.has_key(self.MeasList[reg_index][MeasIdx]):  # find specific sensor name in VoltSensor
                    temp_dict = self.MeasNodes[self.MeasList[reg_index][MeasIdx]]
                    if len(temp_dict) == 3:  # contain 3-phase measurements
                        self.MeasPhases[reg_index][MeasIdx] = 'ABC'
                    elif len(temp_dict) == 2: # # contain 2-phase measurements
                        if temp_dict.has_key('voltage_A') and temp_dict.has_key('voltage_B'):
                            self.MeasPhases[reg_index][MeasIdx] = 'AB'
                        elif temp_dict.has_key('voltage_B') and temp_dict.has_key('voltage_C'):
                            self.MeasPhases[reg_index][MeasIdx] = 'BC'
                        else:
                            self.MeasPhases[reg_index][MeasIdx] = 'AC'
                    elif len(temp_dict) == 1: # only contain 1-phase measurement
                        if temp_dict.has_key('voltage_A'):
                            self.MeasPhases[reg_index][MeasIdx] = 'A'
                        elif temp_dict.has_key('voltage_B'):
                            self.MeasPhases[reg_index][MeasIdx] = 'B'
                        else:
                            self.MeasPhases[reg_index][MeasIdx] = 'C'
                    else:
                        raise ValueError('More than 3 phases for this sensor !')

        # example: self.MeasPhases = [['ABC', 'ABC'], ['AC', 'AB'], ['C'], ['BC']], corresponding to the four regulators

        # Examine if all dicts are filled
##        print self.VVC
##        print self.RegConfig
##        print self.RegTap
##        print self.CapConfig
##        print self.CapState
##        print self.SubLink
##        print self.MeasNodes
##        print self.RegToNodes

        # Calculate volt change per step for raise and lower range
        self.reg_step_up = [0.0] * self.num_regs  # Regulator step for upper taps, associated with num_regs
        self.reg_step_down = [0.0] * self.num_regs  # Regulator step for lower taps, (may be same as reg_step_up)
        for reg_index in range(self.num_regs):
            self.reg_step_up[reg_index] = self.RegConfig['band_center'][reg_index] * self.RegConfig['regulation'][reg_index] /self.RegConfig['raise_taps'][reg_index] # V/tap
            self.reg_step_down[reg_index] = self.RegConfig['band_center'][reg_index] * self.RegConfig['regulation'][reg_index] /self.RegConfig['lower_taps'][reg_index] # V/tap

        # Set voltage regulators and capacitor banks response (progression) time
        self.RegUpdateTimes = self.RegConfig['dwell_time']  # Assume the time delays of regulators and capacitors are given, otherwise use default in VVC configuration
        self.CapUpdateTimes = self.CapConfig['dwell_time']

        # Initialize regulators tap change times, TRegUpdate
        self.TRegUpdate = [self.TS_NEVER] * self.num_regs

        # Initialize capacitors tap change times, TCapUpdate.  Notice: It is a double instead of a list !!
        self.TCapUpdate = 0.0





    def RegControl(self, t0):
        # Initialize some local variables
        vmin = [0.0, 0.0, 0.0]   # 3-phase voltages
        VDrop = [0.0, 0.0, 0.0]
        VSet = [0.0, 0.0, 0.0]
        VRegTo = [0.0, 0.0, 0.0]
        temp_var_u = 0x00
        temp_var_d = 0x00
        prop_tap_changes = [0, 0, 0] # integer, store proposed tap changes of three phases for each regulator
        LimitExceed = 0x00  # U_D - XCBA_XCBA.   0xUD -> U: flag upperbound exceed, D: flag lowerbound exceed
                            # U=1->Ph_A, U=2->Ph_B, U=4->Ph_C, the same for D
        limit_hit = False  # mainly for banked operations
##        treg_min = 0.0  # define a timestamp, Need to ask someone else what this means

        self.Regulator_Change = False # Start out assuming a regulator change hasn't occurred

        # Initialize regulator tap change flag dict
        for reg_index in range(self.num_regs):
            self.RegTap_ChangeFlag[self.RegList[reg_index]] = False

        ###########################################
        ## From here, the core implementation begins
        ###########################################
        if self.VVC['control_method'] == 'ACTIVE':  # turned on

            for reg_index in range(self.num_regs):

                if self.TRegUpdate[reg_index] <= t0 or self.TRegUpdate[reg_index] == self.TS_NEVER: # see if we're allowed to update, current time t0 exceeds RegUpdate time

                    LimitExceed = 0x00
                    vmin = [1e13] * 3  # initialize vmin to something big, 3-phase
                    VDrop = [0.0] * 3  # initialize VDrop, 3-phase
                    VSet = [self.VVC['desired_voltages'][reg_index]] * 3  # default VSet to where we want, 3-phase
                    prop_tap_changes = [0.0] * 3  # initialize tap changes, 3-phase

                    # Parse through the measurement list - find the lowest voltage
                    for meas_index in range(self.num_meas[reg_index]):
                        temp_dict = self.MeasNodes[self.MeasList[reg_index][meas_index]]

                        if self.MeasPhases[reg_index][meas_index].find('A') >= 0:  # Has Phase-A
                            vmin[0] = min(vmin[0], abs(temp_dict['voltage_A']) )  # New Min
                        if self.MeasPhases[reg_index][meas_index].find('B') >= 0:
                            vmin[1] = min(vmin[1], abs(temp_dict['voltage_B']) )
                        if self.MeasPhases[reg_index][meas_index].find('C') >= 0:
                            vmin[2] = min(vmin[2], abs(temp_dict['voltage_C']) )

                    # May need to check if vmin[i] is still large. May lack measurements for some phases

                    # Populate VRegTo (to end voltages), Here it is assumed that each regulator has all 3-phases voltages monitored
                    temp_list = self.RegToNodes[self.RegConfig['to'][reg_index]]

                    if len(temp_list) != 3:
                        raise ValueError('Regulator: ' + self.VVC['regulator_list'][reg_index] + ' is not monitored for all 3 phases !')

                    VRegTo[0] = abs(temp_list[0])
                    VRegTo[1] = abs(temp_list[1])
                    VRegTo[2] = abs(temp_list[2])


                    # Populate VDrop and VSet for Phase-A
                    if self.RegConfig['PT_phase'][reg_index].find('A') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        VDrop[0] = VRegTo[0] - vmin[0] # calculate the drop
                        VSet[0] = self.VVC['desired_voltages'][reg_index] + VDrop[0]  # calculate where we want to be

                        if VSet[0] > self.VVC['maximum_voltages'][reg_index]:
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase A will exceed the maximum allowed voltage!")
                            # The set point necessary to maintain the end point voltage exceeds the maximum voltage limit specified by the system.  Either
    						# increase this maximum_voltage limit, or configure your system differently.

                            if self.RegTap[self.RegList[reg_index]][0] > 0:  # Tap>0, in raise range
                                if VRegTo[0] + self.reg_step_up[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x10 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][0] < 0:  # must be in lower range
                                if VRegTo[0] + self.reg_step_down[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x10 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif VSet[0] < self.VVC['minimum_voltages'][reg_index]:
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase A will exceed the minimum allowed voltage!")

                            if self.RegTap[self.RegList[reg_index]][0] > 0:  # Tap>0, in raise range
                                if VRegTo[0] - self.reg_step_up[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x01 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][0] < 0:  # must be in lower range
                                if VRegTo[0] - self.reg_step_down[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x01 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Populate VDrop and VSet for Phase-B
                    if self.RegConfig['PT_phase'][reg_index].find('B') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        VDrop[1] = VRegTo[1] - vmin[1] # calculate the drop
                        VSet[1] = self.VVC['desired_voltages'][reg_index] + VDrop[1]  # calculate where we want to be

                        if VSet[1] > self.VVC['maximum_voltages'][reg_index]:  # exceed upperbound
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase B will exceed the maximum allowed voltage!")

                            if self.RegTap[self.RegList[reg_index]][1] > 0:  # Tap>0, in raise range
                                if VRegTo[1] + self.reg_step_up[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x20 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][1] < 0:  # must be in lower range
                                if VRegTo[1] + self.reg_step_down[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x20 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif VSet[1] < self.VVC['minimum_voltages'][reg_index]:  # exceed lowerbound
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase B will exceed the minimum allowed voltage!")

                            if self.RegTap[self.RegList[reg_index]][1] > 0:  # Tap>0, in raise range
                                if VRegTo[1] - self.reg_step_up[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x02 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][1] < 0:  # must be in lower range
                                if VRegTo[1] - self.reg_step_down[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x02 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Populate VDrop and VSet for Phase-C
                    if self.RegConfig['PT_phase'][reg_index].find('C') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        VDrop[2] = VRegTo[2] - vmin[2] # calculate the drop
                        VSet[2] = self.VVC['desired_voltages'][reg_index] + VDrop[2]  # calculate where we want to be

                        if VSet[2] > self.VVC['maximum_voltages'][reg_index]:  # exceed upperbound
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase C will exceed the maximum allowed voltage!")

                            if self.RegTap[self.RegList[reg_index]][2] > 0:  # Tap>0, in raise range
                                if VRegTo[2] + self.reg_step_up[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x40 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][2] < 0:  # must be in lower range
                                if VRegTo[2] + self.reg_step_down[reg_index] > self.VVC['maximum_voltages'][reg_index]:
                                    LimitExceed |= 0x40 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif VSet[2] < self.VVC['minimum_voltages'][reg_index]:  # exceed lowerbound
                            print("Warning: " + "for regulator " + self.RegList[reg_index] + \
                                   ". The set point for phase C will exceed the minimum allowed voltage!")

                            if self.RegTap[self.RegList[reg_index]][2] > 0:  # Tap>0, in raise range
                                if VRegTo[2] - self.reg_step_up[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x04 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.RegTap[self.RegList[reg_index]][2] < 0:  # must be in lower range
                                if VRegTo[2] - self.reg_step_down[reg_index] < self.VVC['minimum_voltages'][reg_index]:
                                    LimitExceed |= 0x04 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Now determine what kind of regulator we are (right now only 'INDIVIDUAL' is implemented)
                    if self.RegConfig['control_level'][reg_index] == 'INDIVIDUAL':
                        # handle phases
                        for phase_index in range(3):  # loop through phases
                            LimitExceed &= 0x7F  # Use bit 8 as a validity flag (to save a variable)
                            if phase_index == 0 and self.RegConfig['PT_phase'][reg_index].find('A') >= 0: # We have phase A
     							temp_var_d = 0x01		# A base lower "Limit" checker
    							temp_var_u = 0x10		# A base upper "Limit" checker
    							LimitExceed |= 0x80	# Valid phase
                            if phase_index == 1 and self.RegConfig['PT_phase'][reg_index].find('B') >= 0: # We have phase B
     							temp_var_d = 0x02		# B base lower "Limit" checker
    							temp_var_u = 0x20		# B base upper "Limit" checker
    							LimitExceed |= 0x80	# Valid phase
                            if phase_index == 2 and self.RegConfig['PT_phase'][reg_index].find('C') >= 0: # We have phase C
     							temp_var_d = 0x04		# C base lower "Limit" checker
    							temp_var_u = 0x40		# C base upper "Limit" checker
    							LimitExceed |= 0x80	# Valid phase

                            if (LimitExceed & 0x80) == 0x80: # valid phase
                                # Make sure we aren't below the minimum or above the maximum first (***** This below here \/ \/ ********) - sub with step check! *****                        # can go down (lower limit is not hit)
                                if ( (vmin[phase_index] > self.VVC['maximum_voltages'][reg_index]) or (VRegTo[phase_index] > self.VVC['maximum_voltages'][reg_index]) ) and ( (LimitExceed & temp_var_d) != temp_var_d ):
                                    prop_tap_changes[phase_index] = -1 # Flag us for a down tap
                                elif ( (vmin[phase_index] < self.VVC['minimum_voltages'][reg_index]) or (VRegTo[phase_index] < self.VVC['minimum_voltages'][reg_index]) ) and ( (LimitExceed & temp_var_u) != temp_var_u ):
                                    prop_tap_changes[phase_index] = 1 # Flag us for a up tap
                                else:  # normal operation
                                    # See if we are in high load or low load conditions
                                    if VDrop[phase_index] > self.VVC['max_vdrop'][reg_index]:  # high loading
                                        # See if we're outside our range
                                        if ( (VSet[phase_index] + self.VVC['high_load_deadband'][reg_index]) < VRegTo[phase_index] ) and ( (LimitExceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                            # Above deadband, Need to Tap down.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( VRegTo[phase_index] - self.reg_step_up[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # more more step decrease (in step_up region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( VRegTo[phase_index] - self.reg_step_down[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # more more step decrease (in step_down region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down

                                        elif ( (VSet[phase_index] - self.VVC['high_load_deadband'][reg_index]) > VRegTo[phase_index] ) and ( (LimitExceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                            # Below deadband, Need to Tap up.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step increase will get us theoretically
                                                if ( VRegTo[phase_index] + self.reg_step_up[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step increase will get us theoretically
                                                if ( VRegTo[phase_index] + self.reg_step_down[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up

                                        # else:  (default, inside the deadband, so we don't care)
                                    # Endif   # high load band


                                    else:  # low loading
                                        # See if we're outside our range
                                        if ( (VSet[phase_index] + self.VVC['low_load_deadband'][reg_index]) < VRegTo[phase_index] ) and ( (LimitExceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                            # Above deadband, Need to Tap down.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( VRegTo[phase_index] - self.reg_step_up[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # more more step decrease (in step_up region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( VRegTo[phase_index] - self.reg_step_down[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # more more step decrease (in step_down region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down

                                        elif ( (VSet[phase_index] - self.VVC['low_load_deadband'][reg_index]) > VRegTo[phase_index] ) and ( (LimitExceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                            # Below deadband, Need to Tap up.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step increase will get us theoretically
                                                if ( VRegTo[phase_index] + self.reg_step_up[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step increase will get us theoretically
                                                if ( VRegTo[phase_index] + self.reg_step_down[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up

                                        #else:  (default, inside the deadband, so we don't care)
                                    #Endif   # low load band
                                #Endif  # normal operation
                            #Endif  # valid phase
                        #Endfor  # End phase FOR


    			        #Apply the taps - loop through phases (nonexistant phases should just be 0
    			        #Default assume no change will occur
                        self.Regulator_Change = False
                        self.TRegUpdate[reg_index] = self.TS_NEVER

                        for phase_index in range(3):  # loop through phases
                            if prop_tap_changes[phase_index] > 0:  # want to tap up
                                if self.RegTap[self.RegList[reg_index]][phase_index] >= self.RegConfig['raise_taps'][reg_index]: # cannot exceed raise taps range
                                    self.RegTap[self.RegList[reg_index]][phase_index] = self.RegConfig['raise_taps'][reg_index]
                                else:  # must have room to tap up
                                    self.RegTap[self.RegList[reg_index]][phase_index] += 1  # increment
                                    self.Regulator_Change = True  # Flag as change
                                    self.RegTap_ChangeFlag[self.RegList[reg_index]] = True  # Flag as change if at least one phase tap changes
                                    self.TRegUpdate[reg_index] = t0 + self.RegUpdateTimes[reg_index]  # set return time

                            elif prop_tap_changes[phase_index] < 0:  # want to tap down
                                if self.RegTap[self.RegList[reg_index]][phase_index] <= -self.RegConfig['lower_taps'][reg_index]: # cannot exceed lower taps range
                                    self.RegTap[self.RegList[reg_index]][phase_index] = -self.RegConfig['lower_taps'][reg_index]
                                else:  # must have room to tap down
                                    self.RegTap[self.RegList[reg_index]][phase_index] -= 1  # decrement
                                    self.Regulator_Change = True  # Flag as change
                                    self.RegTap_ChangeFlag[self.RegList[reg_index]] = True  # Flag as change if at least one phase tap changes
                                    self.TRegUpdate[reg_index] = t0 + self.RegUpdateTimes[reg_index]  # set return time

                            #else:  # default else, no change
                        #Endfor   # end phase FOR
                    #Endif    # end individual


                    else:  #  self.RegConfig['control_level'][reg_index] == 'BANKED':
                        # Banked will take first PT_PHASE it matches.  If there's more than one, I don't want to know
                        if self.RegConfig['PT_phase'][reg_index].find('A') >= 0: # We have phase A
                            phase_index = 0     # Index for A-based voltages
                            temp_var_d = 0x01		# A base lower "Limit" checker
                            temp_var_u = 0x10		# A base upper "Limit" checker

                        elif self.RegConfig['PT_phase'][reg_index].find('B') >= 0: # We have phase B
                            phase_index = 1     # Index for B-based voltages
                            temp_var_d = 0x02		# B base lower "Limit" checker
                            temp_var_u = 0x20		# B base upper "Limit" checker

                        else:   # self.RegConfig['PT_phase'][reg_index].find('C') >= 0:: # We have phase C
                            phase_index = 2     # Index for C-based voltages
                            temp_var_d = 0x04 	# C base lower "Limit" checker
                            temp_var_u = 0x40		# C base upper "Limit" checker

                        # Make sure we aren't below the minimum or above the maximum first
                        if ( (vmin[phase_index] > self.VVC['maximum_voltages'][reg_index]) or (VRegTo[phase_index] > self.VVC['maximum_voltages'][reg_index]) ) and ( (LimitExceed & temp_var_d) != temp_var_d ):
                            prop_tap_changes[0] = -1 # Flag us for a down tap
                            prop_tap_changes[1] = -1
                            prop_tap_changes[2] = -1
                        elif ( (vmin[phase_index] < self.VVC['minimum_voltages'][reg_index]) or (VRegTo[phase_index] < self.VVC['minimum_voltages'][reg_index]) ) and ( (LimitExceed & temp_var_u) != temp_var_u ):
                            prop_tap_changes[0] = 1 # Flag us for a up tap
                            prop_tap_changes[1] = 1
                            prop_tap_changes[2] = 1
                        else:  # normal operation
                            # See if we are in high load or low load conditions

                            if VDrop[phase_index] > self.VVC['max_vdrop'][reg_index]:  # high loading
                                # See if we're outside our range
                                if ( (VSet[phase_index] + self.VVC['high_load_deadband'][reg_index]) < VRegTo[phase_index] ) and ( (LimitExceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                    # Above deadband, Need to Tap down.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( VRegTo[phase_index] - self.reg_step_up[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # one more step decrease (in step_up region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( VRegTo[phase_index] - self.reg_step_down[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # one more step decrease (in step_down region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1

                                elif ( (VSet[phase_index] - self.VVC['high_load_deadband'][reg_index]) > VRegTo[phase_index] ) and ( (LimitExceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                    # Below deadband, Need to Tap up.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step increase will get us theoretically
                                        if ( VRegTo[phase_index] + self.reg_step_up[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # one more step increase (in step_up region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step increase will get us theoretically
                                        if ( VRegTo[phase_index] + self.reg_step_down[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # one more step increase (in step_down region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1

                                #Else:  (default, inside the deadband, so we don't care)

                            else:    #  low loading
                                # See if we're outside our range
                                if ( (VSet[phase_index] + self.VVC['low_load_deadband'][reg_index]) < VRegTo[phase_index] ) and ( (LimitExceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                    # Above deadband, Need to Tap down.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( VRegTo[phase_index] - self.reg_step_up[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # one more step decrease (in step_up region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( VRegTo[phase_index] - self.reg_step_down[reg_index] ) < self.VVC['minimum_voltages'][reg_index]: # one more step decrease (in step_down region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1

                                elif ( (VSet[phase_index] - self.VVC['low_load_deadband'][reg_index]) > VRegTo[phase_index] ) and ( (LimitExceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                    # Below deadband, Need to Tap up.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.RegTap[self.RegList[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step increase will get us theoretically
                                        if ( VRegTo[phase_index] + self.reg_step_up[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step increase will get us theoretically
                                        if ( VRegTo[phase_index] + self.reg_step_down[reg_index] ) > self.VVC['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1

                                #Else:  (default, inside the deadband, so we don't care)
                            #Endif high or low loading band
                        #Endif  # normal operation


                        # Check on the assumption of differential banked (offsets can be present, just all move simultaneously)
                        # We'll only check prop->A, since it is banked
                        self.Regulator_Change = False
                        self.TRegUpdate[reg_index] = self.TS_NEVER

                        if prop_tap_changes[0] > 0:  # want to tap up
                            if self.RegTap[self.RegList[reg_index]][0] >= self.RegConfig['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.RegTap[self.RegList[reg_index]][0] = self.RegConfig['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.RegTap[self.RegList[reg_index]][1] >= self.RegConfig['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.RegTap[self.RegList[reg_index]][1] = self.RegConfig['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.RegTap[self.RegList[reg_index]][2] >= self.RegConfig['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.RegTap[self.RegList[reg_index]][2] = self.RegConfig['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if limit_hit == False:  # we can still proceed
                                self.RegTap[self.RegList[reg_index]][0] += 1   # increment them all
                                self.RegTap[self.RegList[reg_index]][1] += 1
                                self.RegTap[self.RegList[reg_index]][2] += 1
                                self.Regulator_Change = True   # Flag the change
                                self.RegTap_ChangeFlag[self.RegList[reg_index]] = True  # Flag as change if at least one phase tap changes
                                self.TRegUpdate[reg_index] = t0 + self.RegUpdateTimes[reg_index]   # set return time
                            #Else   # limit hit, so "no change"


                        elif prop_tap_changes[0] < 0:  # want to tap down
                            # Check individually - set to rail if they are at or exceed - this may lose the offset, but I don't know how they'd ever exceed a limit anyways
                            if self.RegTap[self.RegList[reg_index]][0] <= -self.RegConfig['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.RegTap[self.RegList[reg_index]][0] = -self.RegConfig['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.RegTap[self.RegList[reg_index]][1] <= -self.RegConfig['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.RegTap[self.RegList[reg_index]][1] = -self.RegConfig['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.RegTap[self.RegList[reg_index]][2] <= -self.RegConfig['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.RegTap[self.RegList[reg_index]][2] = -self.RegConfig['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if limit_hit == False:  # we can still proceed
                                self.RegTap[self.RegList[reg_index]][0] -= 1   # increment them all
                                self.RegTap[self.RegList[reg_index]][1] -= 1
                                self.RegTap[self.RegList[reg_index]][2] -= 1
                                self.Regulator_Change = True   # Flag the change
                                self.RegTap_ChangeFlag[self.RegList[reg_index]] = True  # Flag as change if at least one phase tap changes
                                self.TRegUpdate[reg_index] = t0 + self.RegUpdateTimes[reg_index]   # set return time
                            #Else   # limit hit, so "no change"

                        #Else  # either want to tap up or tap down, no change requested
                    #Endif  INDIVIDUAL or BANKED mode
                #Endif   allowed to update, TRegUpdate[reg_index] < t0
            #Endfor   # End regulator traversion FOR
        #Endif    # End VVC 'ACTIVE' control,  self.BasicConfig['control_method'] == 'ACTIVE'


##        # Find the minimum update first
##        treg_min = self.TS_NEVER
##        for reg_index in range(self.num_regs):
##            if self.TRegUpdate[reg_index] < treg_min:
##                treg_min = self.TRegUpdate[reg_index]



    def CapControl(self, t0):
        # Initialize some local variables
        change_requested = False
        temp_size = 0.0
        link_power_vals = 0.0 + 0.0j

        pf_check = False
        bank_status = 'OPEN'
        self.changed_cap = ''

        # Grab power values and all of those related calculations
        if self.VVC['control_method'] == 'ACTIVE' and self.Regulator_Change == False:  # no regulator changes in progress and we're active
            link_power_vals = 0.0 + 0.0j  # zero the power

            # Calculate total complex power at the link, assume all three phases are monitored
            link_power_vals += self.SubLink[self.VVC['substation_link']][0]
            link_power_vals += self.SubLink[self.VVC['substation_link']][1]
            link_power_vals += self.SubLink[self.VVC['substation_link']][2]

            # Populate variables of interest
            self.react_pwr = link_power_vals.imag  # pull in reactive power

            if self.pf_signed == False:
                self.curr_pf = abs(link_power_vals.real) / abs(link_power_vals)  # pull in power factor

            # Update proceeding variables
            if self.pf_signed == False:
                if self.curr_pf < self.VVC['desired_pf']:
                    pf_check = True   # Outside the range, make a change
                else:
                    pf_check = False  # Inside the deadband, don't care

            if pf_check == True and self.TCapUpdate <= t0:
                change_requested = False  # start out assuming no change

                # Parse through the capacitor list - see where they sit in the categories - break after one switching operation
                for cap_index in range(self.num_caps):
                    # Find the phases being watched, check their switch
                    if self.CapConfig['pt_phase'][cap_index].find('A') >= 0:
                        bank_status = self.CapState[self.CapList[cap_index]]['switchA']
                    elif self.CapConfig['pt_phase'][cap_index].find('B') >= 0:
                        bank_status = self.CapState[self.CapList[cap_index]]['switchB']
                    else:  # must be C
                        bank_status = self.CapState[self.CapList[cap_index]]['switchC']

                    if self.pf_signed == False:  # Don't consider the sign, just consider it a range
                        # Now perform logic based on where it is
                        if bank_status == 'CLOSED':   # we are on
                            temp_size = self.CapConfig['cap_size'][cap_index] * self.VVC['d_min']

                            if self.react_pwr < temp_size:
                                for switch_key in self.CapState[self.CapList[cap_index]].keys():
                                    self.CapState[self.CapList[cap_index]][switch_key] = 'OPEN'     # Turn all off
                                change_requested = True
                                self.changed_cap = self.CapList[cap_index]
                                break  # No more loop, only one control per loop

                        else:   # Must be false, so we're off
                            temp_size = self.CapConfig['cap_size'][cap_index] * self.VVC['d_max']

                            if self.react_pwr > temp_size:
                                for switch_key in self.CapState[self.CapList[cap_index]].keys():
                                    self.CapState[self.CapList[cap_index]][switch_key] = 'CLOSED'     # Turn all on
                                change_requested = True
                                self.changed_cap = self.CapList[cap_index]
                                break  # No more loop, only one control per loop

                    # Endif pf_signed
                # End for cap_index

                if change_requested == True:   # Something changed
                    self.TCapUpdate = t0 + self.CapUpdateTimes[cap_index]  # Figure out where we want to go

            # Endif pf_check and self.TCapUpdate <= t0


    def Output(self):

        self.OutputDict = {}
        self.OutputDict[self.simulation_name] = {}

        temp_RegTapDict = {}
        temp_CapSwitchDict = {}

        # Update regulator related outputs
        temp_RegTapKeys = ['tap_A', 'tap_B', 'tap_C']

        for reg_index in range(self.num_regs):
            if self.RegTap_ChangeFlag[self.RegList[reg_index]] == True:
               temp_RegTapDict[self.RegList[reg_index]] = dict(zip(temp_RegTapKeys, self.RegTap[self.RegList[reg_index]]))

        self.OutputDict[self.simulation_name].update(temp_RegTapDict)

        # Update capacitor related outputs
        if self.changed_cap != '':
            temp_CapSwitchDict[self.changed_cap] = self.CapState[self.changed_cap]
            self.OutputDict[self.simulation_name].update(temp_CapSwitchDict)

        self.outputfn(self.OutputDict.copy())
