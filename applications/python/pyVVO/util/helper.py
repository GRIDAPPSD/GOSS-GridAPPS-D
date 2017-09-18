'''
Created on Sep 12, 2017

@author: thay838
'''
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
        