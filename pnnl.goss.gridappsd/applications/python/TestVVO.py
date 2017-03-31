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

VVO_static = open(r'C:\Users\liuy525\Desktop\VVO_Static.json').read()
VVO_static_dict = json.loads(VVO_static)

VVO_message = open(r'C:\Users\liuy525\Desktop\VVO_Message.json').read()
VVO_message_dict = json.loads(VVO_message)

test = VoltVarControl(VVO_static_dict, VVO_message_dict)
test.Input()



