
set hidden3d
set dgrid3d 50,50 qnorm 2

set terminal gif animate delay 10
set output "output.gif"

stats 'results-par.txt' nooutput
N = int(STATS_records) - 1

set zrange [-5:5]

do for [i=0:N] {
# every :::i::i
    splot "results-par.txt" index(i) \
        using 2:3:4 with lines \
        title sprintf("t=%i",i)
}

set output
