
set terminal png

set xlabel 'nodes'

# =============================================================================
# time

set output sprintf("images/%s-time.png", INPUT_FILE)
set title sprintf("total time, [%s]", INPUT_FILE)
set ylabel 'time [s]'

plot \
    INPUT_FILE \
        using 1:2:3 with yerrorbars axes x1y1 \
        notitle, \
    INPUT_FILE \
        using 1:2:3 smooth sbezier with lines \
        title 'time'

# =============================================================================
# speedup

set output sprintf("images/%s-speedup.png", INPUT_FILE)
set title sprintf("speedup S(x,p) = T(x,1) / T(x,p), [%s]", INPUT_FILE)
set ylabel 'speedup'


stats INPUT_FILE every ::0::0 using 2:3 nooutput
TIME_1 = STATS_min_x
# U_TIME_1 = STATS_min_y

plot \
    INPUT_FILE \
        using 1:(TIME_1/$2):(TIME_1/($2**2)*$3) with yerrorbars axes x1y1 \
        notitle, \
    INPUT_FILE \
        using 1:(TIME_1/$2):(TIME_1/($2**2)*$3) smooth bezier with lines \
        title 'speedup'

# =============================================================================
# efficiency

set output sprintf("images/%s-efficiency.png", INPUT_FILE)
set title sprintf("efficiency E(x,p) = S(x,p) / p, [%s]", INPUT_FILE)
set ylabel 'efficiency'

plot \
    INPUT_FILE \
        using 1:(TIME_1/$2/$1):(TIME_1/($2**2)*$3/$1) with yerrorbars \
                                                                    axes x1y1 \
        notitle, \
    INPUT_FILE \
        using 1:(TIME_1/$2/$1):(TIME_1/($2**2)*$3/$1) smooth bezier \
                                                                   with lines \
        title 'efficiency'

# =============================================================================
# cleanup

set output
