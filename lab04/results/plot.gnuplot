
set terminal png

N = 2

total_nodes = 6

do for [i=0:N] {

    stats input_data every ::(i*total_nodes)::(i*total_nodes) using 1 nooutput
    d = STATS_min

    ## total time plot

    set output sprintf("plot-standard-time-%i.png", d)
    set title sprintf("total time, %i numbers", d)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:13:14 with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:13:14 smooth sbezier with lines \
            title 'time'

    ## speedup plot

    set output sprintf("plot-standard-speedup-%i.png", d)
    set title sprintf("speedup S(n,p) = T(n,1) / T(n,p), %i numbers", d)

    stats input_data every ::(i*total_nodes)::(i*total_nodes) using 13:14 nooutput
    t1 = STATS_min_x
    ut1 = STATS_min_y

    # print sprintf("t1 = %f", t1)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$13):(t1/($13**2)*$14) with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$13):(t1/($13**2)*$14) smooth bezier with lines \
            title 'speedup'

    ## efficiency

    set output sprintf("plot-standard-efficiency-%i.png", d)
    set title sprintf("efficiency E(n, p) = S(n,p) / p, %i numbers", d)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$13/$2):(0.0) with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$13/$2):(0.0) smooth bezier with lines \
            title 'efficiency'

}

set output
