'''
Created on Nov 20, 2017

@author: thay838
'''
from pmaps import constants as CONST
import util.constants
import numpy as np
import csv
import time
import matplotlib.pyplot as plt
import datetime

def readZIPResults():
    """Function to read the read the csv which contains the results from the 
    ZIP vs. houses run (experiment.evaluateZIP)
    """
    # Get model and column names
    mn = CONST.MNAMES
    col = CONST.COST_COLS
    # Remove non-numeric columns
    col.remove('time')
    col.remove('model')
    
    # HARD-CODE number of rows (hours in leap year, 2016)
    numRows = 8784
    
    # Initialize array.
    a = np.zeros( (numRows, len(mn), len(col)) )
    # We'll track time as a list for ease of use later
    t = np.empty(numRows, dtype='S23')
        
    # Time the reading!
    t0 = time.time()
    # Open and read the files. NOTE: With 3 models running each hour, it's 
    # around 3MB, so no problem to read the whole thing into memory.
    modelInd = -1
    for m in CONST.COST_FILES:
        modelInd += 1
        with open((CONST.OUTPUT_DIR +'/' + m), newline='') as f:
            # Get csv reader
            r = csv.DictReader(f, quoting=csv.QUOTE_NONNUMERIC)
            # Read each row, and map values appropriately.
            rowInd = 0
            for row in r:
                # Loop over the column names, and assign to array a.
                colInd = 0
                for c in col:
                    # Put data in the appropriate slot
                    a[rowInd, modelInd, colInd] = row[c]
                    # Increment the column index.    
                    colInd += 1
                
                    
                # Increment the row index.
                rowInd += 1
        
    # We now have all the data! Let's do stuff
    t1 = time.time()
    print('File read in {:.1f} seconds.'.format(t1-t0), flush=True)
    
    # Convert time to datetime.
    # WARNING: HARD-CODING stupid windows timezone stuff
    tDT = np.array([datetime.datetime.strptime(x.decode("utf-8").replace(' PST', '').replace(' PDT', '').strip(),
                                               util.constants.DATE_FMT)\
                    for x in t])
    
    # Get model indices
    ind2 = CONST.MNAMES.index('base_2') # 'control' model
    ind3 = CONST.MNAMES.index('base_3') # TMY3 (as opposed to 2) model
    indZ = CONST.MNAMES.index('ZIP') # ZIP model
    
    for colInd in range(len(col)):
        colName = col[colInd][0].upper() + col[colInd][1:]
        for mInd in [ind3, indZ]:
            modelName = CONST.MNAMES[mInd]
            # Determine whether or not to compute absolute difference or 
            # percent change.
            zeroFlag = 0 in a[:, ind2, colInd]
            if zeroFlag:
                # Compute absolute change
                y = a[:, mInd, colInd] - a[:, ind2, colInd]
                t = 'Absolute Change ({}-Control)'
                yl = 'Absolute Change ($)'
            else:  
                # Compute pct change. Don't forget to multiply by 100!
                y = ((a[:, mInd, colInd] - a[:, ind2, colInd]) /
                    (a[:, ind2, colInd])) * 100
                t = 'Percent Change (({}-Control)/Control)'
                yl = 'Percent Change'
                
            # Open figure and plot
            plt.figure()
            plt.plot(tDT, y)
            # Set axis labels
            plt.xlabel('Time')
            plt.ylabel(yl)
            plt.title(colName + ' ' + t.format(modelName) + ' vs. Time')
            
    # Plot control real energy and ZIP real energy on the same plot
    plt.figure()
    plt.plot(tDT, a[:, ind2, col.index('realEnergy')])
    plt.plot(tDT, a[:, indZ, col.index('realEnergy')])
    plt.xlabel('Time')
    plt.ylabel('Real Energy Cost ($)')
    plt.title('Real Energy Cost vs. Time for Control (TMY2) and ZIP')
    plt.legend(['Control', 'ZIP'])
    
    # Plot control reactive energy and ZIP reactive energy on the same plot
    plt.figure()
    plt.plot(tDT, a[:, ind2, col.index('reactiveEnergy')])
    plt.plot(tDT, a[:, indZ, col.index('reactiveEnergy')])
    plt.xlabel('Time')
    plt.ylabel('Reactive Energy Cost ($)')
    plt.title('Reactive Energy Cost vs. Time for Control (TMY2) and ZIP')
    plt.legend(['Control', 'ZIP'])
           
    plt.show()
    
    """
    # Get 'total' index
    indT = col.index('total')
    # Compute the total cost percent change for 3 and zip
    # (new - expected) / expected
    pct3 = (a[:, ind3, indT] - a[:, ind2, indT]) / (a[:, ind2, indT]) 
    pctZ = (a[:, indZ, indT] - a[:, ind2, indT]) / (a[:, ind2, indT])
    
    # Let's plot it as a timeseries to see if there is a pronounced temporal
    # difference
    figZ, axZ = plt.subplots()
    axZ.plot(tDT, pctZ)
    axZ.set(xlabel='Time', ylabel='Percent Change',
            title='Total Cost Percent Change ((ZIP-Control)/Control) vs. Time')
    
    fig3, ax3 = plt.subplots()
    ax3.plot(tDT, pct3)
    ax3.set(xlabel='Time', ylabel='Percent Change',
            title='Total Cost Percent Change ((TMY3-Control)/Control) vs. Time')

    plt.show()
    """
    
    print('hooray')
    
    
if __name__ == '__main__':
    readZIPResults()