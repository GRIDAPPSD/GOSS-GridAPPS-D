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
import matplotlib.dates as mdates
#import datetime
import pickle
from matplotlib.ticker import FuncFormatter
import util.gld

# Set some constants for plotting
FIGSIZE=(16, 7.5)
XTICKSIZE=20
YTICKSIZE=20
XLABELSIZE=26
YLABELSIZE=26
LEGENDFONTSIZE=20
LEGENDLINEWIDTH=8

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
                     'files': [CONST.COST_FILES[CONST.IND_Z]]}
         }
        
    
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
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base2_test1')
    
    # Plot base_3 real energy and ZIP (unconstrained) real energy on the same plot
    y = [d['base']['data'][:, cols.index('realEnergy'), 1],
         d['sUnConst']['data'][:, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base3_test2')
    
    # Plot base_3 lagging pf and ZIP (two week) lagging on the same plot for
    # the first week of June
    t1 = util.helper.tsToDT(ts='2016-05-19 00:00:00 PDT')
    t2 = util.helper.tsToDT(ts='2016-06-08 01:00:00 PDT')
    # Get the indices.
    ind1 = np.where(x == t1)[0][0]
    ind2 = np.where(x == t2)[0][0]
    
    y = [d['base']['data'][ind1:ind2, cols.index('powerFactorLag'), 0],
         d['twoWeek']['data'][ind1:ind2, cols.index('powerFactorLag'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='PF Lag Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/pfLag_base2_test3',
                   fmt='day')
    
    # Plot base_2 real energy and two week ZIP.
    y = [d['base']['data'][:, cols.index('realEnergy'), 0],
         d['twoWeek']['data'][:, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base2_test3')
    
    # Plot energy pct change between base_2 real energy and two week ZIP
    y = [((y[1] - y[0])*100)/y[0]]
    fig = plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                         yl='Pct. Difference in Energy Cost',
                         legend=None,
                         file='figures/pctDiffEnergy_base2_test3',
                         pctDiffFlag=True)
    
    # Compute and print some statistics for the percent difference
    print('Maximum Pct. Difference: {}'.format(np.max(y[0])))
    print('Minimum Pct. Difference: {}'.format(np.min(y[0])))
    print('Mean Pct. Difference:    {}'.format(np.mean(y[0])))
    
    # Get a boxplot
    plt.figure()
    plt.grid(True, which='both')
    plt.boxplot(y[0])
    
    # Get a historgram
    plt.figure(figsize=FIGSIZE)
    weights=np.ones_like(y[0])/float(len(y[0]))
    bins = np.arange(-45, 85, 5)
    n = plt.hist(y[0], bins=bins, weights=weights, ec='black')
    ind1 = int(np.where(bins == -15)[0][0])
    ind2 = int(np.where(bins == 20)[0][0])
    nr = np.sum(n[0][ind1:ind2])
    print('Percent within -15% Pct. Change to 15% Pct. Change: {}'.format(nr))
    formatter = FuncFormatter(to_percent)
    formatter2 = FuncFormatter(to_percent2)
    plt.xlabel('Pct. Difference in Energy Cost', fontsize=XLABELSIZE,
               fontweight='bold')
    plt.ylabel('Percent of Hours', fontsize=YLABELSIZE, fontweight='bold')
    # Create the formatter using the function to_percent. This multiplies all 
    # the default labels by 100, making them all percentages
    # Set the formatter
    plt.gca().yaxis.set_major_formatter(formatter)
    plt.gca().xaxis.set_major_formatter(formatter2)
    setStuff()
    # Set x limits
    ax = plt.gca()
    ax.set_xlim(left=bins[0], right=bins[-1])
    plt.grid(True, which='both')
    plt.savefig('figures/histEnergy_base2_test3.png',bbox_inches='tight')
    #plt.show()
    
    # Plot difference in powerfactor lag. Unfortunately, there are times where
    # the base case is 0, but the twoWeek isn't, so we can't do percent change.
    y = [(d['base']['data'][:, cols.index('powerFactorLag'), 0]
         - d['twoWeek']['data'][:, cols.index('powerFactorLag'), 0])]
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='PF Lag Cost Diff. (Base - ZIP)',
                   legend=None,
                   file='figures/pfLagDiff_base2_test3')
    
    # Plot base case undervoltage violations
    y = [d['base']['data'][:, cols.index('undervoltage'), 0]]
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='Undervoltage Costs ($)',
                   legend=None,
                   file='figures/undervoltage_base2')
    
    # Plot base case (TMY3) undervoltage violations
    y = [d['base']['data'][:, cols.index('undervoltage'), 1]]
    plotTimeSeries(x=x, y=y, xl='Time (Typical Year)',
                   yl='Undervoltage Costs ($)',
                   legend=None,
                   file='figures/undervoltage_base3')
    
    # Plot undervoltage and tap change on the same plot for the first week of
    # January.
    t1 = util.helper.tsToDT(ts='2016-01-05 00:00:00 PST')
    t2 = util.helper.tsToDT(ts='2016-01-06 01:00:00 PST')
    # Get the indices.
    ind1 = np.where(x == t1)[0][0]
    ind2 = np.where(x == t2)[0][0]
    y = [d['base']['data'][ind1:ind2, cols.index('undervoltage'), 0],
         d['base']['data'][ind1:ind2, cols.index('tapChange'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Costs ($)',
                   legend=['Undervoltage', 'Tap Change'],
                   file='figures/undervoltage_tap_base2',
                   fmt='hour')
    
    # Plot tap change for both base case (TMY2) and test 3 for first week of JAn
    y = [d['base']['data'][ind1:ind2, cols.index('tapChange'), 0],
         d['twoWeek']['data'][ind1:ind2, cols.index('tapChange'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Tap Change Costs ($)',
                   legend=['Base Case', 'ZIP'],
                   file='figures/tapChange_base2_test3',
                   fmt='hour')
    
    # Plot cap switch for both base case (TMY2) and test 3 for first week of JAn
    y = [d['base']['data'][ind1:ind2, cols.index('capSwitch'), 0],
         d['twoWeek']['data'][ind1:ind2, cols.index('capSwitch'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Capacitor Switching Costs ($)',
                   legend=['Base Case', 'ZIP'],
                   file='figures/capSwitch_base2_test3',
                   fmt='hour')
    
    # Plot two week energy on the same plot for first week of Jan
    y = [d['base']['data'][ind1:ind2, cols.index('realEnergy'), 0],
         d['twoWeek']['data'][ind1:ind2, cols.index('realEnergy'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Real Energy Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/energy_base2_test3_jan',
                   fmt='hour')
    
    # Plot pf for first week of Jan
    y = [d['base']['data'][ind1:ind2, cols.index('powerFactorLag'), 0],
         d['twoWeek']['data'][ind1:ind2, cols.index('powerFactorLag'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='PF Lag Cost ($)',
                   legend=['Base Case', 'ZIP Model'],
                   file='figures/pfLag_base2_test3_jan',
                   fmt='hour')
    
    # Read logs
    # Hard-code one week
    # weekRows = 24 * 7 + 1
    # Hard-code a day
    weekRows = 24 * 1 + 1
    # Hard-code tap position columns
    tapCols = ['R2-12-47-2_reg_1_A', 'R2-12-47-2_reg_1_B',
               'R2-12-47-2_reg_1_C', 'R2-12-47-2_reg_2_A',
               'R2-12-47-2_reg_2_B', 'R2-12-47-2_reg_2_C']
    
    baseTap = np.zeros( (weekRows, len(tapCols)) )
    test3Tap = np.zeros( (weekRows, len(tapCols)) )
    tapVals = [baseTap, test3Tap]
    logBase = CONST.OUTPUT_BASELINE + '/' + CONST.LOG_FILES[CONST.IND_2]
    logTest3 = CONST.OUTPUT_BASELINE + '/' + CONST.LOG_FILES[CONST.IND_Z]
    logs = [logBase, logTest3]
    
    for ind in range(len(logs)):
        file = logs[ind]
        with open(file, newline='') as f:
            # Get dict reader
            r = csv.DictReader(f, quoting=csv.QUOTE_NONNUMERIC)
            
            # Loop over the rows
            rowInd = 0
            while rowInd < weekRows:
                # Get the row
                row = r.__next__()
                thisTime = util.helper.tsToDT(row['time'])
                if (thisTime >= t1) and (thisTime <= t2):
                    # Loop over the tapCols and place value appropriately
                    colCount = 0
                    for k in tapCols:
                        tapVals[ind][rowInd, colCount] = row[k]
                        colCount += 1
                    
                    rowInd += 1
                elif thisTime > t2:
                    break
                
    # Plot tap positions for the base case
    y = [] 
    for ind in range(tapVals[0].shape[1]):
        y.append(tapVals[0][:, ind])
        
    legend = [x.replace('R2-12-47-2_', '') for x in tapCols]
    
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Tap Position',
                   legend=legend,
                   file='figures/tapPos_base2_jan',
                   fmt='hour')
    
    # Plot tap positions for ZIP
    y = [] 
    for ind in range(tapVals[1].shape[1]):
        y.append(tapVals[1][:, ind])
        
    legend = [x.replace('R2-12-47-2_', '') for x in tapCols]
    
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Tap Position',
                   legend=legend,
                   file='figures/tapPos_test3_jan',
                   fmt='hour')
    
    # Open temperature output file for plotting
    temp = []
    tempTime = []
    file = CONST.AMI_IN_DIR + '/' + 'R2_12_47_2_climate.csv'
    with open (file, newline='') as f:
        # Get .csv reader
        r = csv.reader(f)
        # Advance the reader through the headers
        _ = util.gld.getGLDFileHeaders(r=r)
        # Loop and gather data for the times we need
        while True:
            # Get the row, convert the time to datetime
            row = r.__next__()
            thisTime = util.helper.tsToDT(row[0])
            # If we're in the right range, keep temp and time
            if (thisTime >= t1) and (thisTime <= t2):
                temp.append(float(row[1]))
                tempTime.append(thisTime)
            elif thisTime < t1:
                # Move to the next iteration 
                continue
            elif thisTime > t2:
                # We're done
                break
            
    # Convert the arrays to numpy
    temp = np.asarray(temp)
    tempTime = np.asarray(tempTime)
    
    # Plot
    plotTimeSeries(x=tempTime, y=[temp], xl='Time (Typical Year)',
                   yl='Temperature (F)',
                   legend=None,
                   file='figures/temperature_base2_jan',
                   fmt='hour')
    
    """
    # Plot base case tap changes
    y = [d['base']['data'][ind1:ind2, cols.index('tapChange'), 0]]
    plotTimeSeries(x=x[ind1:ind2], y=y, xl='Time (Typical Year)',
                   yl='Tap Change Costs ($)',
                   legend=None,
                   file='figures/tapChange_base2',
                   fmt='hour')
    """
    
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

def plotTimeSeries(x, y, xl, yl, legend, file, xLabelSize=XLABELSIZE,
                   yLabelSize=YLABELSIZE, legendFontSize=LEGENDFONTSIZE,
                   legendLineWidth=LEGENDLINEWIDTH, fmt='month',
                   pctDiffFlag=False):
    """Helper to make consistent plots
    
    INPUTS:
        x: x data, assumed to be the same length as the y data
        y: list of y data
        xl: xlabel
        yl: ylabel
        legned: list of legend entries corresponding to y
        file: file to save to
    """
    # Get everything bold, get the timezone right
    plt.rc('font', weight='bold')
    plt.rcParams['timezone'] = 'US/Pacific'
    
    # Open figure, set all fonts to bold
    fig = plt.figure(figsize=FIGSIZE) # in inches!
    ax = plt.gca()
    setStuff()
    
    # Thicken the lines if we're looking at less than one week
    
    if (x[-1] - x[0]).total_seconds() > 3600 * 24 * 7:
        w = 1.5
    else:
        w = 4
    
    # Plot each y vector
    for yD in y:
        plt.plot(x, yD, linewidth=w)
        
    # Set the xlimits.
    ax.set_xlim(left=x[0], right=x[-1])
    
    if fmt == 'month':
        # Format the dates    
        months = mdates.MonthLocator()
        monthsFmt = mdates.DateFormatter('%b')
        ax.xaxis.set_major_locator(months)
        ax.xaxis.set_major_formatter(monthsFmt)
    elif fmt == 'day':
        # This is geared toward plotting a couple weeks.
        days = mdates.DayLocator()
        #hours = mdates.HourLocator()
        tFmt = mdates.DateFormatter('%m-%d')
        ax.xaxis.set_major_locator(days)
        #ax.xaxis.set_minor_locator(hours)
        ax.xaxis.set_major_formatter(tFmt)
        ax.get_xaxis().set_tick_params(which='major', width=3, length=10)
        #fig.autofmt_xdate()
        plt.xticks(rotation=-35)
    elif fmt == 'hour':
        # This is geared toward plotting one week.
        days = mdates.DayLocator()
        hours = mdates.HourLocator()
        tFmt = mdates.DateFormatter('%m-%d %H:%M')
        ax.xaxis.set_major_locator(days)
        ax.xaxis.set_minor_locator(hours)
        ax.xaxis.set_major_formatter(tFmt)
        ax.get_xaxis().set_tick_params(which='major', width=3, length=10)
        #fig.autofmt_xdate()
        #plt.xticks(rotation=-35)
        
    
    # Axis labels:
    plt.xlabel(xl, fontsize=xLabelSize, fontweight='bold')
    plt.ylabel(yl, fontsize=yLabelSize, fontweight='bold')
    #plt.xticks(rotation=-20)
    #plt.title('Real Energy Cost vs. Time for Base Case (TMY3) and Test 1 (Seasonal constrained)',
    #          fontsize=20, fontweight='bold')
    
    if legend:
        leg = plt.legend(legend, prop={'size': legendFontSize})
        # Increase legend thicknes
        for legobj in leg.legendHandles:
            legobj.set_linewidth(legendLineWidth)
            
    # rotates and right aligns the x labels, and moves the bottom of the
    # axes up to make room for them
    # fig.autofmt_xdate()
    
    if pctDiffFlag:
        formatter2 = FuncFormatter(to_percent2)
        ax.yaxis.set_major_formatter(formatter2)
    
    plt.savefig(file+'.png',bbox_inches='tight')
    # save pickle (from Demis in https://stackoverflow.com/questions/4348733/saving-interactive-matplotlib-figures)
    pickle.dump(fig, open(file+'.fig.pickle', 'wb'))
    # To open the pickle later:
    # import pickle
    # figx = pickle.load(open(file+'.fig.pickle', 'rb'))
    # figx.show()
    
    return fig
    
def openFig(file):
    """For opening interactively
    """
    figx = pickle.load(open(file, 'rb'))
    figx.show()
    
def to_percent(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(int(100 * y))

    # The percent symbol needs escaping in latex
    if plt.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'
    
def to_percent2(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(int(y))

    # The percent symbol needs escaping in latex
    if plt.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'
    
def setStuff():
    """Call this after plotting stuff
    """
    # Set tick label sizes
    plt.tick_params(axis='x', labelsize=XTICKSIZE)
    plt.tick_params(axis='y', labelsize=YTICKSIZE)
    # Turn the grid on.
    plt.grid(True, which='both')
    
def voltageViolations(t1, t2):
    """Helper function to find out when voltage violations happened.
    """
    fileDir = CONST.OUTPUT_BASELINE + '/' + 'base2'
    files = ['voltage_1.csv', 'voltage_2.csv']
    violations = util.gld.violationsFromRecorderFiles(fileDir=fileDir,
                                                      files=files)
    
    # Loop over the violations, return what we want.
    t = []
    c = []
    ind = 0
    while True:
        thisT = util.helper.tsToDT(violations['time'][ind])
        if thisT >= t1 and thisT <= t2:
            t.append(thisT)
            c.append(violations['low'][ind])
        elif thisT > t2:
            break
        
        # Up the counter.
        ind += 1

    plotTimeSeries(x=np.asarray(t), y=[np.asarray(c)], xl='Time (Typical Year)',
                   yl='Undervoltage Count',
                   legend=['Undervoltage', 'Tap Change'],
                   file='figures/undervoltage_base2_jan',
                   fmt='hour')
    
if __name__ == '__main__':
    #plotPMAPS()
    # Get voltage violations
    voltageViolations(t1 = util.helper.tsToDT(ts='2016-01-05 00:00:00 PST'),
                      t2 = util.helper.tsToDT(ts='2016-01-06 01:00:00 PST'))