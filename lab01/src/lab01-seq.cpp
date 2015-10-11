#include <iostream>
#include <fstream>

double t_min = 0;
double t_max = 30;

double x_min = 0;
double x_max = 30;

double y_min = 0;
double y_max = 30;

int K = 100;
int N = 30;
int M = 30;

const double c = 1.0;

double dt = (t_max-t_min)/K;
double dx = (x_max-x_min)/N;
double dy = (y_max-y_min)/M;

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

void dumpBuffer(double*** data, int K, int N, int M, std::ostream& stream) {
    for (int k=0; k<K; ++k) {
        for (int n=0; n<N; ++n)
            for (int m=0; m<M; ++m)
                stream << k << " " << n << " " << m << " "
                       << data[k][n][m] << "\n";
        stream << "\n\n";
    }
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

void performStep(double*** p, int k) {

    // sets k+1 layer

    // enforce boundary conditions

    for (int n : (const int[]){0, N})
        for (int m=0; m<=M; ++m)
            p[k+1][n][m] = 0;

    for (int m : (const int[]){0, M})
        for (int n=0; n<=N; ++n)
            p[k+1][n][m] = 0;

    // interior

    for (int n=1; n<N; ++n) {
        for (int m=1; m<M; ++m) {
            p[k+1][n][m] = 2*p[k][n][m] - p[k-1][n][m]
                + (dt*dt)*(c*c)*(
                    (p[k][n-1][m]-2*p[k][n][m]+p[k][n+1][m])/(dx*dx) +
                    (p[k][n][m-1]-2*p[k][n][m]+p[k][n][m+1])/(dy*dy)
                );
        }
    }
}

void setInitialConditions(double*** p) {

    // set P[x,y]

    for (int n=0; n<=N; ++n)
        for (int m=0; m<=M; ++m) {
            p[0][n][m] = 0;
            p[1][n][m] = 0;
        }

    int xc = N/2;
    int yc = M/2;

    for (int n : (const int[]){xc-2,xc-1,xc,xc+1,xc+2})
        for (int m : (const int[]){yc-2,yc-1,yc,yc+1,yc+2})
            p[0][n][m] = 5;

    p[0][xc][yc] = 7;

    // set S[x,y] = 0

    for (int n=1; n<N; ++n) {
        for (int m=1; m<M; ++m) {
            p[1][n][m] = p[0][n][m] + 0.5 * (dt*dt)*(c*c)*(
                (p[0][n-1][m]-2*p[0][n][m]+p[0][n+1][m])/(dx*dx) +
                (p[0][n][m-1]-2*p[0][n][m]+p[0][n][m+1])/(dy*dy)
            );
        }
    }
}

int main(int argc, char** argv) {

    double*** data = createDataBuffer(K+1, N+1, M+1);

    setInitialConditions(data);

    for(int k=1; k<K; ++k)
        performStep(data, k);

    std::ofstream outfile ("results.txt");

    dumpBuffer(data, K+1, N+1, M+1, outfile);
    deleteDataBuffer(data, K+1, N+1, M+1);

    return 0;
}
