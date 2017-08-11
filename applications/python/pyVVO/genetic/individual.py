"""
Created on Aug 9, 2017

@author: thay838
"""
import random

class individual:
    
    def __init__(self, uid, reg={}, cap={}):
        """Initialize and individual by building their "chromosome" based
        on available voltage control devices
        
        INPUTS:
            reg: Dictionary describing on-load tap changers.
                Keys should be regulator names, values dictionaries which have
                the following keys:
                    raise_taps
                    lower_taps
                    phases
                Note possible tap positions be in interval [-(lower_taps - 1),
                raise_taps]. 
                    
            cap: Dictionary describing capacitors.
                Keys should be capacitor names, values a list of phases.
                
            uid: Unique ID of individual. Should be an int.
            
        TODO: add controllable DERs
                
        """
        
        # Assign the unique identifier
        self.uid = uid
        
        # Get an instance of the random class and seed it
        rand = random.Random()
        rand.seed()
        
        # Initialize chromosome for regulator and dict to store list indices.
        self.regChrom = []
        self.reg = dict()
        # Intialize index counters.
        s = 0;
        e = 0;
        # Loop through the regs and create binary representation of taps.
        for r, v in reg.items():
            
            # Initialize dict for tracking regulator indices in the chromosome
            self.reg[r] = dict()
            
            # Define the upper tap bound (tb).
            tb = v['raise_taps'] + v['lower_taps'] - 1
            
            # Loop through the phases
            for tap in v['taps']:
                # Initialize dict for this tap.
                self.reg[r][tap] = dict()
                
                # Randomly select a tap.
                tapPos = rand.randint(0, tb)
                
                # Express tap setting as binary list.
                binList = [int(x) for x in "{0:b}".format(tapPos)]
                
                # Extend the regulator chromosome.
                self.regChrom.extend(binList)
                
                # Increment end index.
                e += len(binList) + 1
                
                # Translate tapPos for GridLAB-D.
                if tapPos <= (v['lower_taps'] - 1):
                    self.reg[r][tap]['pos'] = tapPos - v['lower_taps'] + 1
                else:
                    self.reg[r][tap]['pos'] = tapPos - v['raise_taps'] + 1
                
                # Assign indices for this regulator
                self.reg[r][tap]['ind'] = (s, e)
                
                # Increment start index.
                s += len(binList) + 1
            
        # Initialize chromosome for capacitors and dict to store list indices.
        self.capChrom = []
        self.cap = dict()
        # Initialize index counters.
        s = 0;
        e = 0;
        # Loop through the capacitors, randomly assign state for each phase
        # 0 --> open, 1 --> closed
        for c, p in cap.items():
            # Increment end index based on number of phases
            e += len(p) + 1
            
            # Assign indices for this capacitor
            self.cap[c] = (s, e)
            
            # Increment the starting index for the next iteration
            s += len(p) + 1
            
            # Loop through the phases and randomly decide state
            for _ in p:
                # Note that random() is on interval [0.0, 1.0). Thus, we'll
                # consider [0.0, 0.5) and [0.5, 1.0) for our intervals 
                if rand.random() < 0.5:
                    self.capChrom.append(0)
                else:
                    self.capChrom.append(1)
            
    def writeModel(self):
        pass

obj = individual(reg={'reg1': {'raise_taps': 8, 'lower_taps': 8, 'taps': ['tap_A', 'tap_B', 'tap_C']}, 'reg2': {'raise_taps': 16, 'lower_taps': 16, 'taps': ['tap_A']}}, cap={'cap1': ['switchA', 'switchB', 'switchC'], 'cap2': ['switchA']}, uid=1)
print('hooray')
            
        
        