#-------------------------------------------------------------------------------
# Name:        module1
# Purpose:
#
# Author:      liuy525
#
# Created:     23/11/2016
# Copyright:   (c) liuy525 2016
# Licence:     <your licence>
#-------------------------------------------------------------------------------

import math
from VVO import VoltVarControl
import os
import csv
import json
from time import sleep

VVO_static = open(r'C:\Users\liuy525\Desktop\VVO_Static.json').read()
VVO_static_dict = json.loads(VVO_static)

VVO_message = open(r'C:\Users\liuy525\Desktop\VVO_Message.json').read()
VVO_message_dict = json.loads(VVO_message)

VVO_message2 = open(r'C:\Users\liuy525\Desktop\VVO_Message2.json').read()
VVO_message_dict2 = json.loads(VVO_message2)

test = VoltVarControl(VVO_static_dict)

current_ts = 2.0
while True:
    test.Input(VVO_message_dict)
    test.RegControl(current_ts)
    test.CapControl(current_ts)
    test.Output()
    sleep(1.0)
    test.Input(VVO_message_dict2)
    test.RegControl(current_ts)
    test.CapControl(current_ts)
    current_ts += 1
    if current_ts > 20:
        break

