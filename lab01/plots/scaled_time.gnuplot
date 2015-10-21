
set terminal png

N = 2

do for [i=0:N] {

    stats 'ar-lab01-results-scaled-2.txt' every ::(i*12)::(i*12) using 1 nooutput
    d = STATS_min

    ## total time plot

    set output sprintf("plot-scaled-time-%i.png", d)
    set title sprintf("total time, %i per processor", d)

    plot \
        'ar-lab01-results-scaled-2.txt' every ::(i*12)::((i+1)*12-1) \
            using 2:7:8 with yerrorbars \
            notitle, \
        'ar-lab01-results-scaled-2.txt' every ::(i*12)::((i+1)*12-1) \
            using 2:7:8 smooth sbezier with lines \
            title 'time'

    ## speedup plot

    set output sprintf("plot-scaled-speedup-%i.png", d)
    set title sprintf("speedup S(n,p) = p T(n,1) / T(np,p), %i per processor", d)

    stats 'ar-lab01-results-scaled-2.txt' every ::(i*12)::(i*12) using 7:8 nooutput
    t1 = STATS_min_x
    ut1 = STATS_min_y

    # print sprintf("t1 = %f", t1) (t1/($7**2)*$8)

    plot \
        'ar-lab01-results-scaled-2.txt' every ::(i*12)::((i+1)*12-1) \
            using 2:($2*t1/$7):(0.0) with yerrorbars \
            notitle, \
        'ar-lab01-results-scaled.txt' every ::(i*12)::((i+1)*12-1) \
            using 2:($2*t1/$7):(0.0) smooth bezier with lines \
            title 'speedup'

    ## efficiency

#    set output sprintf("plot-scaled-efficiency-%i.png", d)
#    set title sprintf("efficiency E(n, p) = S(n,p) / p, %i per processor", d)
#
#    plot \
#        'ar-lab01-results-scaled.txt' every ::(i*12)::((i+1)*12-1) \
#            using 2:(t1/$7/$2):(0.0) with yerrorbars \
#            notitle, \
#        'ar-lab01-results-scaled.txt' every ::(i*12)::((i+1)*12-1) \
#            using 2:(t1/$7/$2):(0.0) smooth bezier with lines \
#            title 'speedup'

}

set output
