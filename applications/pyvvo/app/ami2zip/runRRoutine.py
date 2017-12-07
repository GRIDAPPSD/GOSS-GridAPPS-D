'''
Created on Dec 6, 2017

@author: thay838
'''
# Add one directory up to Python path. Seems hacky. Oh well, it works.
import os
import sys
upDir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
if upDir not in sys.path:
    sys.path.append(upDir)
import util.helper
import util.constants
import subprocess
import shutil
from queue import Queue
import dateutil.relativedelta as rd
import threading
import pmaps.constants as CONST

def writeCommandFile(starttime, stoptime, thistime, scriptDir, amiDir, 
                     amiFiles, zipOutDir, f='ZIP.cmd'):
    """Function to write the command file for Dave Engel's R routine
    
    INPUTS:
        starttime: aware datetime object indicating the start time which 
            the R routine should read from the file. This time is inclusive.
        stoptime: "..." stop time "..." This time is inclusive.
        thistime: aware datetime object indicating the time we want to have
            analysis ready for. This is typically stoptime + AMI interval
        scriptDir: Directory of command file
        amiDir: Directory of AMI files
        amiFiles: List of the AMI files to read. It looks like they should be
            in order of voltage, real power, reactive power.
        zipOutDir: Directory where ZIP models will be written.
        f: name of command file
    """
    # Open up the file
    with open(scriptDir + '/' + f, 'w') as cmdFile:
        # Write a comment
        cmdFile.write('# Control file for running moving average ZIP model in (R) batch mode\n')
        # Write the AMI files
        for a in amiFiles:
            cmdFile.write(amiDir + '/' + a + '\n')
            
        # Write the output folder
        cmdFile.write(zipOutDir + '\n')
            
        # Write the times
        for t in [starttime, stoptime, thistime]:
            cmdFile.write(t.strftime(util.constants.DATE_TZ_FMT) + '\n')
            
    # That's it.

def runRoutine(scriptDir, script='ZIP-MA-Unconst-Batch.R'):
    """Function to run the R script routine. When R is installed on Linux,
    it would seem that both R and Rscript are added to the path. Not so on
    Windows. Brandon manually added C:\Program Files\R\R-3.4.3\bin to his
    system path. We could put an os.name switch in here, but meh.
    
    NOTE: At the time of writing, the R routine needs one package installed.
    Fire up a terminal as an administrator, and start R.
    Type install.packages("Rsolnp")
    That's it.
    """
    # Formulate the command. Apparently Rscript is preferred.
    #cmd = 'Rscript ' + scriptDir + '/' + script
    #print(cmd)
    # Run it.
    output = subprocess.run(['Rscript', (scriptDir + '/' + script)],
                            cwd=scriptDir, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    return output

def writeYear(numThreads, scriptDir, amiDir, amiFiles, zipOutDir):
    """Function to write a year's worth of 'two week average' data. The R
    program is quite slow, so we're going to need to do this ahead of time.
    """
    # Initialize queue
    q = Queue()
    # Fire up threads.
    threadList = []
    for _ in range(numThreads):
        t = threading.Thread(target=threadedRoutine,
                             args=(q, scriptDir, amiDir, amiFiles, zipOutDir))
        threadList.append(t)
        t.start()
    
    # Create tuples of time, pass to queue.
    startStr = CONST.STARTTIME
    start_dt = (util.helper.tsToDT(startStr, CONST.TIMEZONE)
                - rd.relativedelta(weeks=2))
    thistime = util.helper.tsToDT(startStr, CONST.TIMEZONE)
    stop_dt = thistime - rd.relativedelta(minutes=15)
    
    final_dt = util.helper.tsToDT(CONST.STOPTIME, CONST.TIMEZONE)
    
    # we'll be incrementing times by a day.
    d = rd.relativedelta(days=1)

    while thistime <= final_dt:
        # TODO: put tuple in queue rather than list.
        q.put_nowait((start_dt, stop_dt, thistime))
        
        # Bump times by a day
        start_dt = start_dt + d
        stop_dt = stop_dt + d
        thistime = thistime + d
    
    # Wait for completion.
    q.join()
    
    # Close threads
    for _ in threadList: q.put_nowait(None)
    for t in threadList: t.join(timeout=10)
        
    
    
def threadedRoutine(q, scriptDir, amiDir, amiFiles, zipOutDir,
                    script='ZIP-MA-Unconst-Batch.R'):
    """Helper designed to work with threading
    """
    while True:
        # Get dates from the queue.
        d = q.get()
        
        if d is None:
            # All done.
            q.task_done()
            break
        
        # Delete directory if it exists.
        zipOut = zipOutDir + '/' + d[-1].strftime('%Y-%m-%d')
        if os.path.isdir(zipOut):
            shutil.rmtree(zipOut)
        
        # Create directory
        os.mkdir(zipOut)
        
        # Copy the script into the directory (I hate doing this, but the
        # script assumes the command file is co-located, and we'll have
        # multiple processes working on the commands.
        shutil.copyfile((scriptDir + '/' + script), (zipOut + '/' + script))
        
        # Write the command file.
        writeCommandFile(starttime=d[0], stoptime=d[1], thistime=d[2],
                         scriptDir=zipOut, amiDir=amiDir, amiFiles=amiFiles,
                         zipOutDir=zipOut)
        
        # Execute the script
        runRoutine(scriptDir=zipOut)
        
        # Mark task as complete
        q.task_done()
            
             

if __name__ == '__main__':
    writeYear(numThreads=20, scriptDir=CONST.R_DIR, amiDir=CONST.AMI_IN_DIR,
              amiFiles=CONST.AMI_FILES, zipOutDir=CONST.ZIP_DIR)
    """
    s = '2016-01-01 00:00:00 PST'
    e = '2016-01-14 23:45:00 PST'
    n = '2016-01-15 00:00:00 PST'
    sdt = util.helper.tsToDT(s)
    edt = util.helper.tsToDT(e)
    ndt = util.helper.tsToDT(n)
    writeCommandFile(starttime=sdt, stoptime=edt, thistime=ndt,
                     scriptDir=CONST.R_DIR, amiDir=CONST.AMI_IN_DIR,
                     amiFiles=CONST.AMI_FILES, zipOutDir=CONST.R_DIR)
    
    out = runRoutine(scriptDir=CONST.R_DIR)
    print('hooray')
    """
    '''
    print(sdt.strftime(util.constants.DATE_TZ_FMT))
    print(edt.strftime(util.constants.DATE_TZ_FMT))
    '''
    
