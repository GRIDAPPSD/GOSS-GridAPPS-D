'''
Created on Nov 20, 2017

@author: thay838
'''
from pmaps import constants as CONST
#import util.constants
import util.helper
import numpy as np
import csv
import time
import matplotlib.pyplot as plt
#import datetime
import pickle

def plotPMAPS():
    """Function to read the read the csv which contains the results from the 
    ZIP vs. houses run (experiment.evaluateZIP)
    """
    # Create dictionary of the different files and models.
    # TODO: add twoWeek results when complete.
    d = {'base': {'dir': CONST.OUTPUT_BASELINE,
                  'files': [CONST.COST_FILES[CONST.IND_2],
                            CONST.COST_FILES[CONST.IND_3]]},
         'sConst': {'dir': CONST.OUTPUT_CONSTRAINED,
                    'files': [CONST.COST_FILES[CONST.IND_Z]]},
         'sUnConst': {'dir': CONST.OUTPUT_UNCONSTRAINED,
                      'files': [CONST.COST_FILES[CONST.IND_Z]]},
         'twoWeek': {'dir': CONST.OUTPUT_2WEEK,
                     'files': [CONST.COST_FILES[CONST.IND_Z]]}}
    
    cols = CONST.COST_COLS
    # Remove non-numeric columns
    cols.remove('time')
    
    # HARD-CODE number of rows (hours in leap year, 2016)
    numRows = 8784
    
    # Loop over and read the files.
    # Time the reading!
    t0 = time.time()
    for b in d:
        d[b]['data'], d[b]['time'] = readResultFiles(inDict=d[b],
                                                     numRows=numRows,
                                                     cols=cols, tCol='time')
        
    # We now have all the data! Let's do stuff
    t1 = time.time()
    print('Files read and data ingested in {:.1f} seconds.'.format(t1-t0),
          flush=True)
    
    # Plot base_2 real energy and ZIP (constrained) real energy on the same plot
    x = d['base']['time'][:, 0]
    y = [d['base']['data'][:, cols.index('realEnergy'), 0],
         d['sConst']['data'][:, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time', yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base2_test1')
    
    # Plot base_3 real energy and ZIP (unconstrained) real energy on the same plot
    y = [d['base']['data'][:, cols.index('realEnergy'), 1],
         d['sUnConst']['data'][:, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time', yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base3_test2')
    
    # Plot base_3 lagging pf and ZIP (unconstrained) lagging on the same plot
    y = [d['base']['data'][:, cols.index('powerFactorLag'), 1],
         d['sUnConst']['data'][:, cols.index('powerFactorLag'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time', yl='PF Lag Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/pfLag_base3_test2')
    
    # Plot base_2 real energy and two week ZIP
    y = [d['base']['data'][:, cols.index('realEnergy'), 0],
         d['twoWeek']['data'][:, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time', yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base2_test3')
        
    print('hooray')
    
def readResultFiles(inDict, numRows, cols, tCol='time'):
    """Helper function to put .csv file data into a numpy array.
    
    NOTE: array columns are returned in order of inDict['files']
    """
    # Initialize array.
    a = np.zeros( (numRows, len(cols), len(inDict['files'])) )
    # We'll track time as a list for ease of use later
    t = np.empty((numRows, len(inDict['files'])), dtype='object')
    
    # We need to track column index
    fileIndex = 0
    
    # Loop over the files and read data into the array
    for file in inDict['files']:
        # Open the file
        with open(inDict['dir'] +'/' + file, newline='') as f:
            # Get a .csv reader
            r = csv.DictReader(f, quoting=csv.QUOTE_NONNUMERIC)
            # Read each row, and map values appropriately.
            rowInd = 0
            for row in r:
                # Loop over the column names, and assign to array a.
                colInd = 0
                for c in cols:
                    # Put data in the appropriate slot
                    a[rowInd, colInd, fileIndex] = row[c]
                    # Increment the column index.    
                    colInd += 1
                    
                # Put in the time.
                # Unfortunately we have to do some annoying formatting
                thisT = util.helper.tsToDT(row[tCol])
                t[rowInd, fileIndex] = thisT
                
                # Increment the row index.
                rowInd += 1
                
        # Increment the file index
        fileIndex += 1
        
    # Return
    return a, t

def plotTimeSeries(x, y, xl, yl, legend, file, xTickSize=40, yTickSize=40,
                   xLabelSize=52, yLabelSize=52, legendFontSize=40,
                   legendLineWidth=16):
    """Helper to make consistent plots
    
    INPUTS:
        x: x data, assumed to be the same length as the y data
        y: list of y data
        xl: xlabel
        yl: ylabel
        legned: list of legend entries corresponding to y
        file: file to save to
    """
    
    # Open figure, set all fonts to bold
    fig = plt.figure(figsize=(32.0, 15.0)) # in inches!
    plt.rc('font', weight='bold')
    
    # Plot each
    for yD in y:
        plt.plot(x, yD) 

    plt.tick_params(axis='x', labelsize=xTickSize)
    plt.tick_params(axis='y', labelsize=yTickSize)
    plt.xlabel(xl, fontsize=xLabelSize, fontweight='bold')
    plt.ylabel(yl, fontsize=yLabelSize, fontweight='bold')
    #plt.title('Real Energy Cost vs. Time for Base Case (TMY3) and Test 1 (Seasonal constrained)',
    #          fontsize=20, fontweight='bold')
    leg = plt.legend(legend, prop={'size': legendFontSize})
    # Increase legend thicknes
    for legobj in leg.legendHandles:
        legobj.set_linewidth(legendLineWidth)
        
    plt.grid(True)
    plt.savefig(file+'.png',bbox_inches='tight')
    # save pickle (from Demis in https://stackoverflow.com/questions/4348733/saving-interactive-matplotlib-figures)
    pickle.dump(fig, open(file+'.fig.pickle', 'wb'))
    # To open the pickle later:
    # import pickle
    # figx = pickle.load(open(file+'.fig.pickle', 'rb'))
    # figx.show()
    
def openFig(file):
    """For opening interactively
    """
    figx = pickle.load(open(file, 'rb'))
    figx.show()
    
    
if __name__ == '__main__':
    plotPMAPS()