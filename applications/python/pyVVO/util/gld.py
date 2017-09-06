'''
Created on Aug 29, 2017

@author: thay838
'''
import subprocess

def runModel(modelPath, gldPath='gridlabd'):
    """Function to run GridLAB-D model.
    
    If gridlabd is not setup to just run with 'gridlabd,' pass in
        the full path to the executable in gldPath
    
    TODO: Add support for command line inputs.
    """
    # Construct command and run model. 
    cmd = gldPath + ' ' + modelPath
    # Run command. Note with check=True exception will be thrown on failure.
    output = subprocess.run(cmd, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=True)
    return output

def translateTaps(lowerTaps, raiseTaps, pos):
    """Method to translate tap integer in range 
    [0, lowerTaps + raiseTaps - 1] to range [-(lower_taps - 1), raise_taps]
    """
    # TODO: unit test
    if pos <= (lowerTaps - 1):
        posOut = pos - lowerTaps + 1
    else:
        posOut = pos - raiseTaps + 1
        
    return posOut