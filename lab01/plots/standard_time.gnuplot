
set terminal png

N = 0

total_nodes = 48

do for [i=0:N] {

    stats input_data every ::(i*total_nodes)::(i*total_nodes) using 1 nooutput
    d = STATS_min

    ## total time plot

    set output sprintf("plot-standard-time-%i.png", d)
    set title sprintf("total time, %ix%i grid", d, d)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:7:8 with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:7:8 smooth sbezier with lines \
            title 'time'

    ## speedup plot

    set output sprintf("plot-standard-speedup-%i.png", d)
    set title sprintf("speedup S(n,p) = T(n,1) / T(n,p), %ix%i grid", d, d)

    stats input_data every ::(i*total_nodes)::(i*total_nodes) using 7:8 nooutput
    t1 = STATS_min_x
    ut1 = STATS_min_y

    # print sprintf("t1 = %f", t1)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$7):(t1/($7**2)*$8) with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$7):(t1/($7**2)*$8) smooth bezier with lines \
            title 'speedup'

    ## efficiency

    set output sprintf("plot-standard-efficiency-%i.png", d)
    set title sprintf("efficiency E(n, p) = S(n,p) / p, %ix%i grid", d, d)

    plot \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$7/$2):(0.0) with yerrorbars \
            notitle, \
        input_data every ::(i*total_nodes)::((i+1)*total_nodes-1) \
            using 2:(t1/$7/$2):(0.0) smooth bezier with lines \
            title 'efficiency'

}

set output
