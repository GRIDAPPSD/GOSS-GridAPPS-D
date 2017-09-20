'''
Created on Sep 12, 2017

@author: thay838
'''
import time
import json

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
    starttime= "2009-07-21 00:00:00"
    tFmt = "%Y-%m-%d %H:%M:%S"
    interval = 60
    s = incrementTime(t=starttime, fmt=tFmt, interval=interval)
    print(s)