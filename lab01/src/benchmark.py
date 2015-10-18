#!/usr/bin/env python3

import subprocess
import statistics
import sys

get_arg = lambda n, default: int(sys.argv[n]) if n < len(sys.argv) else default

scaled_arg = get_arg(1, 0)
scaled = scaled_arg > 0

test_program = './lab01-par.o'

def run_test(nodes, K, N, M):
    try:
        process = subprocess.Popen([
            'mpiexec',
            # '-recvtimeout', '100',
            # '-machinefile', './mpihosts',
            '-n', str(nodes),
            test_program,
            str(K), str(N), str(M)
        ], stdout=subprocess.PIPE)
        return float(process.communicate()[0])
        # return map( float, process.communicate()[0].split() )
    except:
        return None

series = 4
nodes = range(1, 13)

# standard
problem_size = [300, 600, 1200]

# scaled
problem_per_node = [50, 100, 200]

K = 100

if not scaled:
    for N in problem_size:
        for nod in nodes:
            res = [run_test(nod, K, N, N) for _ in range(series)]
            if all(res):
                print('{0} {1} {2} {3} {4}'.format(
                    N, nod, ' '.join(map(str,res)),
                    statistics.mean(res), statistics.stdev(res)
                ))
            else:
                print('{0} {1} error', N, nod)
else:
    for N in problem_per_node:
        for nod in nodes:
            NN = N*nod
            res = [run_test(nod, K, NN, NN) for _ in range(series)]
            if all(res):
                print('{0} {1} {2} {3} {4}'.format(
                    NN, nod, ' '.join(map(str,res)),
                    statistics.mean(res), statistics.stdev(res)
                ))
            else:
                print('{0} {1} error', NN, nod)
