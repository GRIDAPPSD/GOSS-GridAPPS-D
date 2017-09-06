'''
Helper module for basic database functionality.

Prerequisites: mysql-connector. NOTE: Need to install 2.1.6 since
    2.3.3 fails to install. 'pip install mysql-connector==2.1.6'

Created on Aug 29, 2017

@author: thay838
'''
import mysql.connector.pooling
from mysql.connector import errorcode

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

'''
WHY WON'T THIS WORK?!?!
def dropAllTables(cnxn):
    """Drop all tables in database.
    INPUT: database connection.
    """
    # Get database name.
    db = cnxn.database
    # Get a cursor.
    cursor = cnxn.cursor()
    delCursor = cnxn.cursor(buffered=True,dictionary=True)
    # Get the names of all tables in database.
    cursor.execute(("SELECT table_name "
                    "FROM information_schema.tables "
                    "WHERE table_schema = '{}'").format(db))
    # Loop through each table and drop it.
    row = cursor.fetchone()
    while row:
        # Drop the table.
        y = delCursor.execute("DROP TABLE '{}'".format(row[0]))
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
        cursor: pyodbc cursor object made from a pyodbc connection
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
    if starttime and stoptime:
        q += " WHERE {tCol}>='{starttime}' and {tCol}<='{stoptime}'".format(tCol=tCol,
                                                                           starttime=starttime,
                                                                           stoptime=stoptime)
        
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
    cursor.execute(q)
    
    # Fetch the first row.
    row = cursor.fetchone()
    
    # Extract the units (assume all units are the same).
    unit = row[0].split()[1]
    
    # Loop over the rows and sum.
    t = 0+0j
    while row:
        # Three phase power is simply the complex sum of the individual phases.
        for ind in range(len(cols)):
            # Strip off the unit included with the complex number and
            # add it to the total.
            t += complex(row[ind].split()[0])
        '''
        for col in cols:
            # Strip off the unit included with the complex number and
            # add it to the total.
            v = getattr(row, col)
            t += complex(v.split()[0])
        '''
        # Advance the cursor.    
        row = cursor.fetchone()
        
    return t, unit
    