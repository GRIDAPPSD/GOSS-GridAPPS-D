'''
Created on Sep 12, 2017

@author: thay838
'''
import time
import json
import cmath
import math
import re

# Compile some regular expressions for detection of complex number forms
RECT_EXP = re.compile(r'[+-]([0-9])+(\.)*([0-9])*(e[+-]([0-9])+)*[+-]([0-9])+(\.)*([0-9])*(e[+-]([0-9])+)*j')
FIRST_EXP = re.compile(r'[+-]([0-9])+(\.)*([0-9])*(e[+-]([0-9])+)*')
SECOND_EXP = re.compile(r'[+-]([0-9])+(\.)*([0-9])*(e[+-]([0-9])+)*[dr]')
    
def getComplex(s):
    """Function to take a string which represents a complex number and convert
    it into a Python complex type. This is specifically intended to work with
    output from GridLAB-D. The string can have units.
    
    INPTUS:
        s: string representing the complex number. 
            Ex: +12.34-1.2j VA
            Ex: +15-20d V
            Ex: +12-3.14r I
            
    TODO: unit test! There are some sample inputs in the __main__ section.
    """
    # First, strip whitespace, then split on whitespace to strip off the unit
    t = s.strip().split()
    
    # Detect form and take action
    if RECT_EXP.fullmatch(t[0]):
        # If it's already in rectangular form, there's not much work to do
        n = complex(t[0])
    else:
        # Extract the first and second terms
        magFloat = float(FIRST_EXP.match(t[0]).group())
        phaseStr = SECOND_EXP.search(t[0]).group()
        # If the number doesn't fit the form, raise exception.
        if (not magFloat) or (not phaseStr):
            raise ValueError(('Inputs to getComplex must have a sign defined '
                  + 'for both components.\nNo space is allowed, '
                  + 'except between the number and the unit.\n'
                  + 'Decimals are optional.\n'
                  + 'Number must end in j, d, or r.'))
        # Extract the unit and phase from the phase string
        phaseUnit = phaseStr[-1]
        phaseFloat = float(phaseStr[:-1])
        # If the unit is degrees, convert to radians
        if phaseUnit == 'd':
            phaseFloat = math.radians(phaseFloat)
        
        # Convert to complex.
        n = (magFloat * cmath.exp(1j * phaseFloat))
        
    return n, t[1]

def powerFactor(n):
    """Function to compute power factor given a complex power value
    TODO: unit test! Will this work if we're exporting power? I think so...
    """
    # Real divided by apparent
    pf = n.real / n.__abs__()
    # Determine lagging vs leading (negative).
    # NOTE: cmath.phase returns counter-clockwise angle on interval [-pi, pi],
    # so checking sign should be reliable for determining lead vs. lag 
    p = cmath.phase(n)
    if p < 0:
        return (pf, 'lead')
    else:
        return (pf, 'lag')
    
def bin2int(binList):
        """Take list representing binary number (ex: [0, 1, 0, 0, 1]) and 
        convert to an integer
        """
        # TODO: unit test
        # initialize number and counter
        n = 0
        k = 0
        for b in reversed(binList):
            n += b * (2 ** k)
            k += 1
            
        return n
    
def rotateVVODicts(reg, cap, deleteFlag=False):
    """Helper function to take in the 'control' dictionaries (described in
    docstring of gld module) and shift 'new' positions/statuses to 'old' 
    """
    # Rotate the reg and cap dictionaries.
    reg = _rotate(reg, deleteFlag)
    cap = _rotate(cap, deleteFlag)
    
    return reg, cap

def _rotate(d, deleteFlag):
    """Function called by rotateVVODicts - it parses a dictionary in the format
    of 'reg' or 'cap' as defined in the gld module docstring and replaces old
    with new. new is deleted if deleteFlag is True.
    """
    # Loop through dictionary
    for _d in d:
        # Loop through phases of each element
        for p in d[_d]['phases']:
            # Replace prevState with newState
            d[_d]['phases'][p]['prevState'] = d[_d]['phases'][p]['newState']
            
            # Delete 'newState' and 'chromInd' if deleteFlag set to True
            if deleteFlag:
                del d[_d]['phases'][p]['newState']
                
                if 'chromInd' in d[_d]['phases'][p]:
                    del d[_d]['phases'][p]['chromInd']
                    
        # Delete control scheme
        if deleteFlag:
            if 'Control' in d[_d]:
                del d[_d]['Control']
            elif 'control' in d[_d]:
                del d[_d]['control']
                
                    
    return d

def _update(dOld, dNew, dType):
    """Function to update the 'prevState' in dOld with the 'prevState' in dNew
    
    Unfortunately this is really similiar to '_rotate', but I didn't want to 
    jam a square peg in a round hole.
    """
    c = 0
    # Loop through dictionary
    for _d in dOld:
        # Loop through phases of each element
        for p in dOld[_d]['phases']:
            # Update dOld with value from dNew
            dOld[_d]['phases'][p]['prevState'] = \
                dNew[_d]['phases'][p]['prevState']
                
            # Update count
            if dType == 'reg':
                # previous position minus new position
                c += abs(dOld[_d]['phases'][p]['prevState']
                         - dOld[_d]['phases'][p]['newState'])
            elif dType == 'cap':
                if (dOld[_d]['phases'][p]['prevState']
                        != dOld[_d]['phases'][p]['newState']):
                    
                    c += 1
            else:
                assert False, 'dType must be reg or cap'
                
    return dOld, c
                
def updateVVODicts(regOld, capOld, regNew, capNew):
    """Function to update 'prevState' in old dictionaries with the 'prevState'
    in new dictionaries. This is used when keeping an old individual from the
    previous genetic algorithm run to seed the next run's population.
    """
    regOld, tapChangeCount = _update(dOld=regOld, dNew=regNew, dType='reg')
    capOld, capSwitchCount = _update(dOld=capOld, dNew=capNew, dType='cap')
    
    return {'reg': regOld, 'cap': capOld, 'tapChangeCount': tapChangeCount,
            'capSwitchCount': capSwitchCount}

def incrementTime(t, fmt, interval):
        """Simple function to increment a time string by a specified amount.
        
        INPTUS: 
            t: string representation of a time, in the format given by 'fmt'
            fmt: Python time string format corresponding to 't'
            interval: interval in seconds to increment t by.
            
        TODO: unit test
        TODO: daylight savings safe?
        """
        # TODO: Daylight savings problems?
        # TODO: We're running an extra minute of simulation each run.
        tN = time.mktime(time.strptime(t, fmt)) + interval
        tOut = time.strftime(fmt, time.localtime(tN))
        return tOut

def timeDiff(t1, t2, fmt):
    """Simple function to get the difference (in seconds) of t2-t1.
    """
    delta = time.mktime(time.strptime(t2, fmt)) \
        - time.mktime(time.strptime(t1, fmt))
    return delta

def timeInfoForZIP(starttime, stoptime, fmt, zipInterval=3600):
    """Function to extract the necessary time information to layer ZIP models.
    """
    # Ensure times are within the same ZIP modeling window. For now, hard-code
    # interval to same hour.
    # TODO: Make the 'delta' an input? or a constant? Anyways, don't hide it
    # here
    delta = timeDiff(starttime, stoptime, fmt)
    assert delta <= zipInterval
    
    # Get times as struct_times
    ts = time.strptime(starttime, fmt)
    te = time.strptime(stoptime, fmt)
    
    # Again, hard-coding an hour check. Make sure the end time doesn't run into
    # the next hour
    if ts.tm_hour != te.tm_hour:
        assert (te.tm_min == 0) and (te.tm_sec == 0)
        
    # If we've made it here, our dates are valid and usable. Get the info.
    # Start by extracting season. For now, this is hard-coded to be 3 month
    # chunks. While we could do fancy math, may as well be explicit and do
    # stacked if/else
    if (ts.tm_mon >= 1) and (ts.tm_mon <= 3):
        season = 1
    elif (ts.tm_mon >= 4) and (ts.tm_mon <= 6):
        season = 2
    elif (ts.tm_mon >= 7) and (ts.tm_mon <= 9):
        season = 3
    elif (ts.tm_mon >= 10) and (ts.tm_mon <= 12):
        season = 4
        
    # Get the hour
    hour = ts.tm_hour
    
    # Get weekday vs weekend
    if ts.tm_wday <= 4:
        wday = 'day'
    else:
        wday = 'end'
        
    # Return
    out = {'season': season, 'hour': hour, 'wday': wday}
    return out
    
    
def getSummaryStr(costs, reg, cap, regChrom=None, capChrom=None, parents=None):
    """Helper method to create a string representation of a model.
        Information could come from genetic.individual or a benchmark run.
        
    INPUTS:
        costs: dictionary which comes from gld.computeCosts
        reg: dictionary described in docstring of gld
        cap: dictionary described in docstring of gld
        regChrom: regulator chromosome as described in individual.py
        capChrom: capacitor chromosome as described in individual.py
        parents: uid's of parents that made this model (none for benchmark)
    """
    s = ''
    if costs:
        s += json.dumps(costs) + '\n'
    
    # Add parents.
    if parents:
        s += 'Parents: {}\n'.format(parents)
        
    # Add the essential regulator elements to the string.
    for r in reg:
        s += r + ':\n'
        for p in reg[r]['phases']:
            s += '  ' + p + ': ' + 'newState={}, prevState={}\n'.format(
                reg[r]['phases'][p]['newState'],
                reg[r]['phases'][p]['prevState'])
            
    # Add the regulator chromosome.
    if regChrom:
        s += 'RegChrom: ' + json.dumps(regChrom) + '\n'
    
    # Add the essential capacitor elements to the string.
    for c in cap: 
        s += c + ':\n'
        for p in cap[c]['phases']:
            s += '  ' + p + ': ' + 'newState={}, prevState={}\n'.format(
                cap[c]['phases'][p]['newState'],
                cap[c]['phases'][p]['prevState'])
            
    # Add the capacitor chromosome.
    if capChrom:
        s += 'CapChrom: ' + json.dumps(capChrom)
    
    # That's all for now.
    return s
    
if __name__ == '__main__':
    """
    starttime= "2009-07-21 00:00:00"
    tFmt = "%Y-%m-%d %H:%M:%S"
    interval = 60
    s = incrementTime(t=starttime, fmt=tFmt, interval=interval)
    print(s)
    """
    
    """
    s1 = '+348863+13.716d VA'
    n1, u1 = getComplex(s1)
    s2 = '-12.2+13d I'
    n2, u2 = getComplex(s2)
    s3 = '+3.258-2.14890r kV'
    n3, u3 = getComplex(s3)
    s4 = '-1+2j VAr'
    n4, u4 = getComplex(s4)
    s5 = '+1.2e-003+1.8e-2j d'
    n5, u5 = getComplex(s5)
    s6 = '-1.5e02+12d f'
    n6, u6 = getComplex(s6)
    print('hooray')
    """
    
    c1 = 1+1j
    r1 = powerFactor(c1)
    c2 = 1-1j
    r2 = powerFactor(c2)
    c3 = -1+1j
    r3 = powerFactor(c3)
    c4 = -1-1j
    r4 = powerFactor(c4)
    print('hooray')