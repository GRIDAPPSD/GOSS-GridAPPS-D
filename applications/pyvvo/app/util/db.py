'''
Helper module for basic database functionality.

Prerequisites: mysql-connector. NOTE: Need to install 2.1.6 since
    2.3.3 fails to install. 'pip install mysql-connector==2.1.6'

Created on Aug 29, 2017

@author: thay838
'''
import mysql.connector.pooling
from mysql.connector import errorcode
from util import helper

def connectPool(user='gridlabd', password='', host='localhost',
            database='gridlabd', pool_name='mypool', pool_size=1):
            #use_pure = False
            # It would appear that 'use_pure' doesn't work for pools.
    """Method to create and return mysql connection pool. This is threadsafe.
    
    NOTE: This pooling technique doesn't allow the 'use_pure' option. For
        GridLAB-D we already have to install the MySQL Connector C library,
        so if it's thread-safe we should use the faster option.
    
    INPUTS:
        user: database user
        password: user's password
        host: database host address
        database: database to use
        pool_name: name of pool object to create
        pool_size: number of connections in the pool. Since we're using this
            to be threadsafe, may as well make the size equal to the number
            of threads.
    """
    try:
        cnx = mysql.connector.pooling.MySQLConnectionPool(user=user,
                                                          password=password,
                                                          host=host,
                                                          database=database,
                                                          pool_name=pool_name,
                                                          pool_size=pool_size)
    except mysql.connector.Error as err:
        _printError(err)
    else:
            return cnx 
        
def connect(user='gridlabd', password='', host='localhost',
            database='gridlabd', use_pure=False):
    """Method to create and return a single database connection. This
        implementation allows us to use the C connector, while the pool
        doesn't.
        
    INPUTS:
    user: database user
    password: user's password
    host: database host address
    database: database to use
    use_pure: True to use pure Python implementation, False to use C connector.
        NOTE: C connector must be installed, and likely on the PATH.
    """
    try:
        cnx = mysql.connector.connect(user=user, password=password, host=host,
                                      database=database, use_pure=use_pure)
    except mysql.connector.Error as err:
        _printError(err)
    else:
        return cnx
    
def truncateTable(cursor, table):
    """Method to truncate a given table.
    """
    cursor.execute('TRUNCATE TABLE {}'.format(table))
    
def _printError(err):
    """Print a connection error.
    """
    if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
        print("Something is wrong with your user name or password")
    elif err.errno == errorcode.ER_BAD_DB_ERROR:
        print("Database does not exist")
    else:
        print(err)
'''
import pyodbc

def connect(driver='MySQL ODBC 5.3 Unicode Driver', usr='gridlabd',
                  host='localhost', schema='gridlabd'):
        """Connect to database with pyodbc.
        
        System will need an odbc driver for MySQL.
        """
        # TODO: Accomodate Linux and maybe Mac
        # Build the connection string.
        c = ('Driver={{{driver}}};Login Prompt=False;UID={usr};'
             'Data Source={host};Database={schema};CHARSET=UTF8')
        
        # Initialize connection
        cnxn = pyodbc.connect(c.format(driver=driver, usr=usr, host=host,
                                            schema=schema))
        
        # pyodbc documentation says we need to configure decoding/encoding: 
        # https://github.com/mkleehammer/pyodbc/wiki/Connecting-to-MySQL
        cnxn.setdecoding(pyodbc.SQL_WCHAR, encoding='utf-8')
        cnxn.setencoding(encoding='utf-8')
        
        return cnxn
'''
    
def dropTable(cursor, table):
    """ Simple method to drop a table from the database.
    
    INPUTS:
        cursor: pyodbc cursor object made from a pyodbc connection
    """
    # TODO: Figure out why pyodbc binding isn't working.
    cursor.execute('DROP TABLE {}'.format(table))

def dropAllTables(cnxn):
    """Drop all tables in database.
    INPUT: database connection.
    
    Note: my efficient dual-loop method kept giving an 'unread results' error.
    """
    # Get database name.
    db = cnxn.database
    # Get a cursor.
    cursor = cnxn.cursor()
    # Get the names of all tables in database.
    cursor.execute(("SELECT table_name "
                    "FROM information_schema.tables "
                    "WHERE table_schema = '{}'").format(db))
    tables = cursor.fetchall()
    # Loop through and drop.
    for table in tables:
        dropTable(cursor, table[0])
        
    '''
    # Loop through each table and drop it.
    row = cursor.fetchone()
    while row:
        # Drop the table.
        y = delCursor.execute("DROP TABLE {}".format(row[0]))
        # Must fetch all before executing another query.
        x = delCursor.fetchall()
        # Advance to the next row.
        row = cursor.fetchone()
        
    # Commit.
    delCursor.commit()
    '''
    
def sumComplexPower(cursor, cols, table, tCol='t', starttime=None, stoptime=None):
    """Sum complex power in table.
    
    INPUTS:
        cursor: cursor from database connection
        cols: names of columns to access and sum in a list
        table: table to query
        tCol: name of time column. GridLAB-D mysql defaults this to 't'
        starttime: date string formatted as yyyy-mm-dd HH:MM:SS.
        stoptime: date string formatted as yyyy-mm-dd HH:MM:SS.
        
    IMPORTANT NOTE: It is assumed table values are in the form of "x+yj units"
    where x is the real part of the complex number, y is the imaginary part,
    and units is the unit definition.
    
    IMPORTANT NOTE: All units in table are assumed to be the same.
    
    OUTPUT: tuple in form (sum, unit)
    """
    
    # Prepare query.
    q = "SELECT {} FROM {}".format(','.join(cols), table)
    # Add time bounds
    q += timeWhere(tCol=tCol, starttime=starttime, stoptime=stoptime)
        
    # Need to do a try catch here due to weird error:
    # pyodbc.Error: ('HY000', '[HY000] [MySQL][ODBC 5.3(w) Driver][mysqld-5.7.19-log]Table definition has changed, please retry transaction (1412) (SQLExecDirectW)')
    """
    try:
        cursor.execute(q)
    except:
        print('Error while trying to read table, attempting to execute query again.')
        #cursor.commit()
        cursor.execute(q)
    """
    # Execute the query.
    cursor.execute(q)
    
    # Fetch the first row.
    row = cursor.fetchone()
    
    # Initialize total.
    t = 0+0j
    # Loop over the rows and sum.
    while row:
        # Three phase power is simply the complex sum of the individual phases.
        for ind in range(len(cols)):
            # Get the complex value and its unit
            v, u = helper.getComplex(row[ind]) 
            # Add the value to the total.
            t += v

        # Advance the cursor.    
        row = cursor.fetchone()
    
    # Assign return. NOTE: All units assumed to be the same.
    out = {'rowCount': cursor.rowcount, 'sum': t, 'unit': u}
    return out

def sumMatrix(cursor, table, cols, tCol='t', starttime=None, stoptime=None):
    """Method to element-wise sum given rows and columns. Note that this also
        works for single columns or multiple columns.
    
    INPUTS:
        cursor: cursor from database connection
        cols: names of columns to access and sum in a list
        table: table to query
        tCol: name of time column. GridLAB-D mysql defaults this to 't'
        starttime: date string formatted as yyyy-mm-dd HH:MM:SS.
        stoptime: date string formatted as yyyy-mm-dd HH:MM:SS.
    """
    # Prepare query - start with getting 'col1' + 'col2' + 'col3'...
    s = "{}".format(cols[0])
    for c in cols[1:]:
        s += " + {}".format(c)
    
    q = "SELECT SUM({}) FROM {}".format(s, table)
    # Add time bounds
    q += timeWhere(tCol=tCol, starttime=starttime, stoptime=stoptime)
    
    # Execute query, return
    cursor.execute(q)
    r = cursor.fetchone()
    return r[0]
    
def timeWhere(tCol, starttime, stoptime):
    """Helper function to get WHERE clause for time-based query. Both times 
        are considered inclusive.
    
    INPUTS:
        tCol: name of time column
        starttime: date string formatted as yyyy-mm-dd HH:MM:SS
        stoptime: date string formatted as yyy-mm-dd HH:MM:SS
    """
    if starttime and stoptime:
        if starttime != stoptime:
            # Times aren't equal, use range
            s = (" WHERE {tCol}>='{starttime}' and "
                 "{tCol}<='{stoptime}'").format(tCol=tCol, starttime=starttime,
                                                  stoptime=stoptime)
        else:
            # Times are equal, use equality
            s = " WHERE {tCol}='{starttime}'".format(tCol=tCol,
                                                     starttime=starttime)
    else:
        s = ''
        
    return s

def voltageViolations(cursor, table, vLow=228, vHigh=252, vCols=['voltage_12'],
                      tCol='t', starttime=None, stoptime=None):
    """Loop through a table of voltages and count voltage violations.
    
    TODO: Ideally, finding the violation count would be coded in SQL - 
        something like 'SELECT COUNT(voltage_12) FROM table WHERE 
        voltage_12 > 252'
        Unfortunately, GridLAB-D records the values as strings....
        
    TODO: Track number of meters that create the violation.
    """
    # Create query
    q = "SELECT {} FROM {}".format(','.join(vCols), table)
    
    # Add time bounds
    q += timeWhere(tCol=tCol, starttime=starttime, stoptime=stoptime)
    
    # Execute query.
    cursor.execute(q)
    
    # Track high and low violations.
    high = 0
    low = 0
    
    # Fetch the first row, loop until all have been consumed.
    row = cursor.fetchone()
    while row:
        # Only count one violation per object (don't count multiple times if
        # multiple phases are out of bounds)
        violation = False
        # Loop over the columns (phases)
        for ind in range(len(vCols)):
            if not violation:
                # Get the complex value
                v, _ = helper.getComplex(row[ind])
    
                # Increment counters if necessary
                if v.__abs__() >= vHigh:
                    high += 1
                    violation = True
                elif v.__abs__() <= vLow:
                    low += 1
                    violation = True
        
        # Fetch next row.    
        row = cursor.fetchone()
        
    return {'high': high, 'low': low}

def updateStatus(inDict, dictType, cursor, table, phaseCols, t,
                 nameCol='name', tCol='t'):
    """
    """
    # Formulate query. Note how the name is intentionally put first.
    q = "SELECT {}, {} FROM {}".format(nameCol, ','.join(phaseCols), table)
    # Add timestamp
    q += timeWhere(tCol=tCol, starttime=t, stoptime=t)
    
    # Based on the dictType, determine what to strip from the phaseCols to
    # just leave the phase behind.
    if dictType == 'reg':
        s = 'tap_'
    elif dictType == 'cap':
        s = 'switch'
        
    # TODO: DERs.
    
    # Query the database.
    cursor.execute(q)
    
    # Iterate through the rows
    row = cursor.fetchone()
    while row:
        # extract the name
        n = row[0]
        # Loop through the rest of the row.
        for k in range(1, len(phaseCols)+1):
            # Infer the phase.
            p = phaseCols[k-1].replace(s, '')
            
            # If this phase is in the dictionary, update status.
            # Recall that we had to make a column for every phase, even if
            # this device isn't connected to the phase.
            if p in inDict[n]['phases']:    
                # Assign to 'newStatus'
                inDict[n]['phases'][p]['newState'] = row[k]
        
        # Get next row.
        row = cursor.fetchone()
    
    # Done. 
    return inDict

if __name__ == '__main__':
    cnxn = connect()
    dropAllTables(cnxn)
    cursor = cnxn.cursor()
    # dropTable(cursor, 'swing_benchmark')
    """
    r1 = sumMatrix(cursor=cursor, table='capcount_benchmark',
                  cols=['cap_A_switch_count', 'cap_B_switch_count',
                        'cap_C_switch_count'],
                  tCol='t',
                  starttime='2009-07-21 00:00:00',
                  stoptime='2009-07-21 00:15:00')
    
    r2 = sumMatrix(cursor=cursor, table='regcount_benchmark',
                  cols=['tap_A_change_count', 'tap_B_change_count',
                        'tap_C_change_count'], tCol='t',
                  starttime='2009-07-21 00:00:00',
                  stoptime='2009-07-21 00:15:00')
    
    print(r1)
    print(r2)
    """
        