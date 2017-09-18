'''
Created on Sep 14, 2017

@author: thay838
'''
#import openpyxl as xl
import numpy as np
from vvo import test
#import copy
import math

# Initialize best inputs.
bestInputs = {}

# Create range of [0.1, 0.9] for testing various probabilities
pctList = np.arange(0.1, 1, 0.1)  # @UndefinedVariable

populationInputs = {'topPct': 0.2, 'weakProb': 0.2, 'popMutateProb': 0.2, 
                    'crossProb': 0.7, 'capGeneMutateProb': 0.1,
                    'regGeneMutateProb': 0.05}

# Loop over the different knobs.
for prob in populationInputs:
    # Track the original input
    original = populationInputs[prob]
    
    # Initialize best score
    bestScore = math.inf
    
    print('Looping over {}.'.format(prob))
    
    # Loop over the range and run the genetic algorithm for each combination
    for p in pctList:
        populationInputs[prob] = p
        scoreOut = test.main(populationInputs)
        print('Score for {} at {:.1f}: {:.2f}'.format(prob, p, scoreOut),
              flush=True)
        # If this score was best, record it.
        if scoreOut < bestScore:
            bestScore = scoreOut
            bestInputs[prob] = p
        
    # Reset.
    populationInputs[prob] = original
    print('Best probability for {} might be {}.'.format(prob, bestInputs[prob]))

# Report all results.
print('Best results:')
for prob in bestInputs:
    print('{}: {:.1f}'.format(prob, bestInputs[prob]), flush=True)
with open("C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output/sensitivity/results.txt", "w") as f:
    f.write(bestInputs.__str__())