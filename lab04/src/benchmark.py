#!/usr/bin/env python3

import subprocess
import statistics
import sys

test_program = './lab04-par.o'

def run_test(nodes, dataPerProcessor, dataRange, seed):
    try:
        process = subprocess.Popen([
            'mpiexec',
            # '-recvtimeout', '100',
            # '-machinefile', './mpihosts',
            '-n', str(nodes),
            test_program,
            str(dataPerProcessor),
            str(dataRange),
            str(seed)
        ], stdout=subprocess.PIPE)
        return float(process.communicate()[0])
    except:
        return None

series = 10
nodes = [1, 2, 4, 8, 16, 32]

totalData = [3200000, 16000000, 32000000]

seed = 1
dataRange = 100000000

for data in totalData:
    for nod in nodes:
        procData = data/nod
        res = [run_test(nod, data/nod, dataRange, seed) for _ in range(series)]
        if all(res):
            print('{0} {1} {2} {3} {4}'.format(data,
                                               nod,
                                               ' '.join(map(str,res)),
                                               statistics.mean(res),
                                               statistics.stdev(res)))
        else:
            print('{0} {1} error', data, nod)
