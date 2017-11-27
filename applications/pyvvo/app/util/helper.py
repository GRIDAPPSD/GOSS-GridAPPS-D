'''
Created on Sep 12, 2017

@author: thay838
'''
import json
import cmath
import math
import re
import datetime
import dateutil.tz
import util.constants
import copy

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
    
    NOTE: copy's of the inputs are made so as not to modify them elsewhere.
    """
    # Rotate the reg and cap dictionaries.
    regOut = _rotate(d=copy.deepcopy(reg), deleteFlag=deleteFlag)
    capOut = _rotate(d=copy.deepcopy(cap), deleteFlag=deleteFlag)
    
    return regOut, capOut

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

def toUTC(ts, timezone=None):
    """Helper function to take a given time string, parse it into a datetime
    object, then convert it to UTC
    
    INPUTS:
        ts: string representing a time. 
        timezone: timezone string as listed in GridLAB-D's tzinfo file.
        
    NOTE: if ts has a timezone specifier in it (like PDT or PST), the timezone
        input should be none. Likewise, if the timezone input is provided,
        the ts string should NOT have a timezone specifier in it. One or the
        other must be provided. Errors will be thrown otherwise.
    """
    
    # Check if the string came with a timezone. If so, infer both timezone and
    # daylight savings time
    tzMatch = util.constants.TZ_EXP.search(ts)
    
    # We're overdefined if the timestamp has a tz, and the timezone was given
    if tzMatch and timezone:
        assert False, ("Either supply a timezone string in the given timestamp"
                       + " or give the timezone input, but not both.")
    elif (timezone is None) and (not tzMatch):
        # We have no timezone information....
        assert False, ("There was no parseable timezone string in the given "
                       + "time string, and the timezone input was not "
                       + "given. One or the other must be provided.")
    
    if tzMatch:
        # Extract the match
        tzStr = tzMatch.group()
        if tzStr[0] == 'E':
            tz = util.constants.TZ['EST5EDT']
        elif tzStr[0] == 'C':
            tz = util.constants.TZ['CST6CDT']
        elif tzStr[0] == 'M':
            tz = util.constants.TZ['MST7MDT']
        elif tzStr[0] == 'P':
            tz = util.constants.TZ['PST8PDT']
        elif tzStr[0] == 'A':
            tz = util.constants.TZ['AST9ADT']
        elif tzStr[0] == 'H':
            tz = util.constants.TZ['HST10HDT']
        
        # Determine if we're in daylight savings time
        if tzStr[1] == 'D':
            daylight = True
        else:
            daylight = False
        
        # Remove the timezone portion of the string
        ts = util.constants.TZ_EXP.sub('', ts).strip()
        
    else:
        # Get timezone object
        tz = getTZObj(timezone=timezone)
        daylight = None
        
    # Create naive object.
    naive = datetime.datetime.strptime(ts, util.constants.DATE_FMT)
    # Add timezone information.
    dt = naive.replace(tzinfo=tz)
    
    # Ensure the time exists
    if not dateutil.tz.datetime_exists(dt=dt, tz=tz):
        raise ValueError('A time which does not exist was given!')
    
    # Check to see if it's ambiguous. If so, 'enfold' it based on 'daylight'
    # Times are ambiguous if they happen twice - so during 'fall back'
    # Note that during fall back we go from daylight to standard time, so fold
    # should go from 0 to 1.
    if tz.is_ambiguous(dt=dt):
        if daylight is None:
            raise ValueError('An ambiguous date was given, and daylight '
                             + 'savings could not be determined!')
        elif daylight:
            # Fold should be 0.
            dateutil.tz.enfold(dt, fold=0)
        else:
            # Fold should be 1.
            dateutil.tz.enfold(dt, fold=1)
    
    # Convert to UTC
    dtUTC = dt.astimezone(dateutil.tz.tzutc())
    
    return dtUTC

def getTZObj(timezone):
    """Helper function to grab a tzinfo object for a given timezone. Note that
    there is currently only support for timezones described in GridLAB-D's 
    tzinfo file. See util.constants.TZ
    """
    # Remove a '+' if present in the timezone
    timezone = timezone.replace('+', '')
    # The folowing will result in a key error if the given timezone is bad.
    tz = util.constants.TZ[timezone]
    
    return tz
    
def utcToTZ(dt, timezone):
    """Helper function to take a datetime object in UTC time and convert it
    to a timezone. Currently only supports timezones described in GridLAB-D's
    tzinfo file. See util.constants.TZ
    
    INPUTS:
        dt: datetime object in UTC time
        timezone: timezone string, as would be listed in GridLAB-D's tzinfo
            file. Ex: 'PST+8PDT'
    """
    tz = getTZObj(timezone=timezone)
    dtNew = dt.astimezone(tz=tz)
    return dtNew
    
def timeInfoForZIP(starttime, stoptime, zipInterval=3600):
    """Function to extract the necessary time information to layer ZIP models.
    
    NOTE: the 'hour' property needs to properly handle DST. This is done here.
    
    INPUTS:
        starttime: aware datetime object
        stoptime: aware datetime object
        zip
    """
    # Ensure times are within the same ZIP modeling window. For now, hard-code
    # interval to same hour.
    # TODO: Make the 'delta' an input? or a constant? Anyways, don't hide it
    # here
    delta = stoptime - starttime
    assert delta.total_seconds() <= zipInterval
    
    # Again, hard-coding an hour check. Make sure the end time doesn't run into
    # the next hour
    if starttime.hour != stoptime.hour:
        assert (starttime.minute == 0) and (stoptime.minute == 0)
        
    # If we've made it here, our dates are valid and usable. Get the info.
    # Start by extracting season. For now, this is hard-coded to be 3 month
    # chunks. While we could do fancy math, may as well be explicit and do
    # stacked if/else
    if (starttime.month >= 1) and (starttime.month <= 3):
        season = 1
    elif (starttime.month >= 4) and (starttime.month <= 6):
        season = 2
    elif (starttime.month >= 7) and (starttime.month <= 9):
        season = 3
    elif (starttime.month >= 10) and (starttime.month <= 12):
        season = 4
    
    # Get weekday vs weekend
    if starttime.weekday() <= 4:
        wday = 'day'
    else:
        wday = 'end'
        
    # Return
    out = {'season': season,
           'hour': starttime.hour,
           'wday': wday}
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
    
    """
    c1 = 1+1j
    r1 = powerFactor(c1)
    c2 = 1-1j
    r2 = powerFactor(c2)
    c3 = -1+1j
    r3 = powerFactor(c3)
    c4 = -1-1j
    r4 = powerFactor(c4)
    """
    
    """
    from util import gld
    t1 = '2016-03-13 01:00:00 Pacific Daylight Time'
    t2 = '2016-03-13 02:00:00 Pacific Daylight Time'
    fmt = gld.DATE_FMT
    o = timeInfoForZIP(starttime=t1, stoptime=t2, fmt=(fmt + ' %Z'))
    print('hooray')
    """
    f = '%Y-%m-%d %H:%M:%S %Z'
    tz = 'PST+8PDT'
    # Look at spring forward
    #t1 = datestrToDatetime('2016-03-13 00:00:00 PST')
    ts = '2016-03-13 00:00:00'
    tU = toUTC(ts, tz)
    for i in range(4):
        tn = tU + i*datetime.timedelta(seconds=3600)
        tP = utcToTZ(tn, tz)
        print(tP.strftime(util.constants.DATE_FMT + ' %Z'))
        #print('Using strftime   :', tn.strftime(f))
        #print('With print method: ' + printDatetime(tn, f))
        #tn2 = tn.astimezone(util.constants.TZ['PST8PDT'])
        #print('As timezone      :', tn.date(), tn.time(), tn.tzname())
        #print()
    
    print()
    # Look at fall back
    #t2 = datestrToDatetime('2016-11-06 00:00:00 PDT')
    t2 = '2016-11-06 00:00:00'
    tU = toUTC(t2, tz)
    #tU = toUTC(t2)
    for i in range(4):
        tn = tU + i*datetime.timedelta(seconds=3600)
        tP = utcToTZ(tn, tz)
        print(tP.strftime(util.constants.DATE_FMT + ' %Z'))
        print('Fold: {}'.format(tP.fold))
        #print('Using strftime   :', tn.strftime(f))
        #print('With print method: ' + printDatetime(tn, f))
        #tn2 = tn.astimezone(util.constants.TZ['PST8PDT'])
        #print('As timezone      :', tn.date(), tn.time(), tn.tzname())
        #print()
        
    print('hooray')
    #t2 = incrementTime(t=t1, fmt=gld.DATE_FMT + ' %Z', interval=3600)
    #Pacific  = USTimeZone(-8, "Pacific",  "PST", "PDT")
    