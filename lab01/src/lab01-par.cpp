#include <iostream>
#include <fstream>
#include <cstdlib>
#include <cmath>
#include <mpi.h>

double*** createDataBuffer(int K, int N, int M) {
    double*** data = new double**[K];
    for (int k=0; k<K; ++k) {
        data[k] = new double*[N];
        for (int n=0; n<N; ++n) {
            data[k][n] = new double[M];
            for (int m=0; m<M; ++m) {
                data[k][n][m] = 0;
            }
        }
    }
    return data;
}

void deleteDataBuffer(double*** data, int K, int N, int M) {
    for (int k=0; k<K; ++k) {
        for (int n=0; n<N; ++n) {
            delete[] data[k][n];
        }
        delete[] data[k];
    }
    delete[] data;
}

void dumpBuffer(double*** data, int K, int N, int M, std::ostream& stream) {
    for (int k=0; k<K; ++k) {
        for (int n=0; n<N; ++n)
            for (int m=0; m<M; ++m)
                stream << k << " " << n << " " << m << " "
                       << data[k][n][m] << "\n";
        stream << "\n\n";
    }
}

class Solver {
private:

    int world_rank, world_size;
    int K, N, M;
    double t_min, t_max, x_min, x_max, y_min, y_max;
    double dt, dx, dy;
    double c;
    int N_local, N_min, N_max;

public:

    Solver(int _world_rank, int _world_size, int _K, int _N, int _M) :
        world_rank(_world_rank),
        world_size(_world_size),
        K(_K), N(_N), M(_N),
        t_min(0), t_max(30),
        x_min(0), x_max(30),
        y_min(0), y_max(30),
        c(1.001) {

        dt = (t_max-t_min)/K;
        dx = (x_max-x_min)/N;
        dy = (y_max-y_min)/M;

        N_local = (int) ceil((double) N / world_size);

        N_min = world_rank * N_local; // inclusive
        N_max = N_min + N_local;      // exclusive

        // last one may get fewer elements
        if (world_rank + 1 == world_size) {
            N_max = N;
            N_local = N_max - N_min;
        }
    }

    void solve() {

        // +2 for neighbour buffers (left + right)
        double*** p = createDataBuffer(K, N_local + 2, M);

        setInitialConditions(p);

        for (int k=1; k < K-1; ++k) {
            MPI_Barrier(MPI_COMM_WORLD);
            exchangeBuffers(p, k);
            performStep(p, k);
        }

        const int root = 0;
        double* results = collectResults(p, root);

        if (world_rank == root) {
            constructSolution(results, true);
        }

        if (results != nullptr) {
            delete[] results;
        }

        deleteDataBuffer(p, K, N_local + 2, M);
    }

private:

    void setInitialConditions(double*** p) const {

        for (int n=1; n<=N_local; ++n) {
            for (int m=0; m<M; ++m) {
                p[0][n][m] = 0;
                p[1][n][m] = 0;
            }
        }

        // P[n,m]

        int xc = N/2;
        int yc = M/2;

        for (int n : (const int[]){xc-2,xc-1,xc,xc+1,xc+2})
            for (int m : (const int[]){yc-2,yc-1,yc,yc+1,yc+2})
                if (n >= N_min && n < N_max) // here n is `global` index
                    p[0][n-N_min+1][m] = -5; // +1 for left recv buffer

        if (xc >= N_min && xc < N_max)
            p[0][xc-N_min+1][yc] = -7;

        exchangeBuffers(p, 0);

        // S[n,m]

        for (int n=1; n<=N_local; ++n) {
            for (int m=0; m<M; ++m) {
                if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
                    p[1][n][m] = 0;
                } else if(m == 0 || m == M-1) {
                    p[1][n][m] = 0;
                }
                else {
                    p[1][n][m] = p[0][n][m] + 0.5 * (dt*dt)*(c*c)*(
                        (p[0][n-1][m]-2*p[0][n][m]+p[0][n+1][m])/(dx*dx) +
                        (p[0][n][m-1]-2*p[0][n][m]+p[0][n][m+1])/(dy*dy)
                    );
                }
            }
        }
    }

    void exchangeBuffers(double*** p, int k) const {

        const int tag = 0;
        MPI_Status status;

        // left-transfer
        if (world_rank > 0) {
            MPI_Sendrecv(
                p[k][1], M, MPI_DOUBLE, world_rank-1, tag, // send
                p[k][0], M, MPI_DOUBLE, world_rank-1, tag, // recv
                MPI_COMM_WORLD, &status);
        }

        // right transfer
        if (world_rank < world_size-1) {
            MPI_Sendrecv(
                p[k][N_local], M, MPI_DOUBLE, world_rank+1, tag,   // send
                p[k][N_local+1], M, MPI_DOUBLE, world_rank+1, tag, // recv
                MPI_COMM_WORLD, &status);
        }
    }

    void performStep(double*** p, int k) const {
        for (int n=1; n<=N_local; ++n) {
            for (int m=0; m<M; ++m) {
                if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
                    p[k+1][n][m] = 0;
                } else if(m == 0 || m == M-1) {
                    p[k+1][n][m] = 0;
                }
                else {
                    p[k+1][n][m] = 2*p[k][n][m] - p[k-1][n][m]
                        + (dt*dt)*(c*c)*(
                            (p[k][n-1][m]-2*p[k][n][m]+p[k][n+1][m])/(dx*dx) +
                            (p[k][n][m-1]-2*p[k][n][m]+p[k][n][m+1])/(dy*dy)
                        );
                }
            }
        }
    }

    double* collectResults(double*** p, int root) const {

        double* sendbuff = new double[K*N_local*M];

        // linearize p
        for (int k=0; k<K; ++k)
            for (int n=0; n<N_local; ++n)
                for (int m=0; m<M; ++m)
                    sendbuff[k*N_local*M + n*M + m] = p[k][n+1][m];

        double* recvbuff = nullptr;

        if (world_rank == root) {
            recvbuff = new double[K*N*M];
        }

        MPI_Gather(
            sendbuff, K*N_local*M, MPI_DOUBLE,
            recvbuff, K*N_local*M, MPI_DOUBLE,
            root, MPI_COMM_WORLD);

        delete[] sendbuff;
        return recvbuff;
    }

    void constructSolution(double* results, bool print) const {

        double*** solution = createDataBuffer(K, N, M);

        for (int i=0; i<world_size; ++i) {

            for (int k=0; k<K; ++k)
                for (int n=0; n<N_local; ++n)
                    for (int m=0; m<M; ++m) {
                        solution[k][i*N_local + n][m] =
                            results[i*K*N_local*M + k*N_local*M + n*M + m];
                    }

        }

        if (print) {
            std::ofstream outfile ("results-par.txt");
            dumpBuffer(solution, K, N, M, outfile);
        }

        deleteDataBuffer(solution, K, N, M);
    }
};


int main(int argc, char** argv) {

    if (argc < 4) {
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    const int K = strtol(argv[1], nullptr, 10);
    const int N = strtol(argv[2], nullptr, 10);
    const int M = strtol(argv[3], nullptr, 10);

    const double t_min = 0;
    const double t_max = 30;

    const double x_min = 0;
    const double x_max = 30;

    const double y_min = 0;
    const double y_max = 30;

    const double c = 1.0;

    const double dt = (t_max-t_min)/K;
    const double dx = (x_max-x_min)/N;
    const double dy = (y_max-y_min)/M;

    MPI_Init (&argc, &argv);  /* starts MPI */

    int world_rank = 0;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
    int world_size = 1;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);

    Solver solver(world_rank, world_size, K, N, M);
    solver.solve();

    // const int root = 0;
    //
    // int N_local = (int) ceil((float) N / world_size);
    //
    // int N_min = world_rank * N_local; // inclusive
    // int N_max = N_min + N_local;      // exclusive
    //
    // // last one may get fewer elements
    // if (world_rank + 1 == world_size) {
    //     N_max = N;
    //     N_local = N_max - N_min;
    // }
    //
    // // +2 for neighbour buffers (left + right)
    // double*** p = createDataBuffer(K, N_local + 2, M);
    //
    // // INITIAL conditions ---------------------
    //
    // for (int n=1; n<=N_local; ++n) {
    //     for (int m=0; m<M; ++m) {
    //         p[0][n][m] = 0;
    //         p[1][n][m] = 0;
    //     }
    // }
    //
    // // P[n,m]
    //
    // int xc = N/2;
    // int yc = M/2;
    //
    // for (int n : (const int[]){xc-2,xc-1,xc,xc+1,xc+2})
    //     for (int m : (const int[]){yc-2,yc-1,yc,yc+1,yc+2})
    //         if (n >= N_min && n < N_max) // here n is `global` index
    //             p[0][n-N_min+1][m] = -5; // +1 for left recv buffer
    //
    // if (xc >= N_min && xc < N_max)
    //     p[0][xc-N_min+1][yc] = -7;
    //
    // MPI_Status status;
    // const int tag = 0;
    //
    //     // left-transfer
    //     if (world_rank > 0) {
    //         MPI_Sendrecv(
    //             p[0][1], M, MPI_DOUBLE, world_rank-1, tag,
    //             p[0][0], M, MPI_DOUBLE, world_rank-1, tag,
    //             MPI_COMM_WORLD, &status);
    //     }
    //
    //     // right transfer
    //     if (world_rank < world_size-1) {
    //         MPI_Sendrecv(
    //             p[0][N_local], M, MPI_DOUBLE, world_rank+1, tag,
    //             p[0][N_local+1], M, MPI_DOUBLE, world_rank+1, tag,
    //             MPI_COMM_WORLD, &status);
    //     }
    //
    // // S[n,m]
    //
    // for (int n=1; n<=N_local; ++n) {
    //     for (int m=0; m<M; ++m) {
    //         if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
    //             p[1][n][m] = 0;
    //         } else if(m == 0 || m == M-1) {
    //             p[1][n][m] = 0;
    //         }
    //         else {
    //             p[1][n][m] = p[0][n][m] + 0.5 * (dt*dt)*(c*c)*(
    //                 (p[0][n-1][m]-2*p[0][n][m]+p[0][n+1][m])/(dx*dx) +
    //                 (p[0][n][m-1]-2*p[0][n][m]+p[0][n][m+1])/(dy*dy)
    //             );
    //         }
    //     }
    // }
    //
    //
    // // ALGORITHM ---------------------------
    //
    // for (int k=1; k < K-1; ++k) {
    //
    //     MPI_Barrier(MPI_COMM_WORLD);
    //
    //     // left-transfer
    //     if (world_rank > 0) {
    //         MPI_Sendrecv(
    //             p[k][1], M, MPI_DOUBLE, world_rank-1, tag,
    //             p[k][0], M, MPI_DOUBLE, world_rank-1, tag,
    //             MPI_COMM_WORLD, &status);
    //     }
    //
    //     // right transfer
    //     if (world_rank < world_size-1) {
    //         MPI_Sendrecv(
    //             p[k][N_local], M, MPI_DOUBLE, world_rank+1, tag,
    //             p[k][N_local+1], M, MPI_DOUBLE, world_rank+1, tag,
    //             MPI_COMM_WORLD, &status);
    //     }
    //
    //     for (int n=1; n<=N_local; ++n) {
    //         for (int m=0; m<M; ++m) {
    //             if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
    //                 p[k+1][n][m] = 0;
    //             } else if(m == 0 || m == M-1) {
    //                 p[k+1][n][m] = 0;
    //             }
    //             else {
    //                 p[k+1][n][m] = 2*p[k][n][m] - p[k-1][n][m]
    //                     + (dt*dt)*(c*c)*(
    //                         (p[k][n-1][m]-2*p[k][n][m]+p[k][n+1][m])/(dx*dx) +
    //                         (p[k][n][m-1]-2*p[k][n][m]+p[k][n][m+1])/(dy*dy)
    //                     );
    //             }
    //         }
    //     }
    //
    // }
    //
    // if (world_rank == 0) {
    //     for (int k=0; k<K; ++k)
    //         for (int n=0; n<N_local+2; ++n)
    //             for (int m=0; m<M; ++m)
    //                 std::cout << p[k][n][m] << "\n";
    // }
    //
    // // END --------------------------------------
    //
    // double* sendbuff = new double[K*N_local*M];
    // for (int k=0; k<K; ++k)
    //     for (int n=0; n<N_local; ++n)
    //         for (int m=0; m<M; ++m)
    //             sendbuff[k*N_local*M + n*M + m] = p[k][n+1][m];
    //
    // double* recvbuff = nullptr;
    //
    // if (world_rank == root) {
    //     recvbuff = new double[K*N*M];
    // }
    //
    // MPI_Gather(
    // sendbuff,
    // K*N_local*M,
    // MPI_DOUBLE,
    // recvbuff,
    // K*N_local*M,
    // MPI_DOUBLE,
    // root,
    // MPI_COMM_WORLD);
    //
    //
    // // RECONSTRUCT ----------------------------------------------
    //
    // double*** final = nullptr;
    //
    // if (world_rank == root) {
    //     double*** final = createDataBuffer(K, N, M);
    //
    //
    //     for (int i=0; i<world_size; ++i) {
    //
    //         for (int k=0; k<K; ++k)
    //             for (int n=0; n<N_local; ++n)
    //                 for (int m=0; m<M; ++m) {
    //                     final[k][i*N_local + n][m] =
    //                         recvbuff[i*K*N_local*M + k*N_local*M + n*M + m];
    //                 }
    //     }
    //
    //     std::ofstream outfile ("results-par.txt");
    //     dumpBuffer(final, K, N, M, outfile);
    //     deleteDataBuffer(final, K, N, M);
    // }
    //
    // deleteDataBuffer(p, K, N_local + 2, M);
    // delete[] sendbuff;
    // if (nullptr != recvbuff) {
    //     delete[] recvbuff;
    // }

    MPI_Finalize();

    return 0;
}
