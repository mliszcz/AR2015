#include <iostream>
#include <fstream>
#include <cstdlib>
#include <algorithm>
#include <iterator>
#include <sstream>
#include <vector>
#include <string>
#include <utility>
#include <cmath>
#include <mpi.h>

// double*** createDataBuffer(int K, int N, int M) {
//     double*** data = new double**[K];
//     for (int k=0; k<K; ++k) {
//         data[k] = new double*[N];
//         for (int n=0; n<N; ++n) {
//             data[k][n] = new double[M];
//             for (int m=0; m<M; ++m) {
//                 data[k][n][m] = 0;
//             }
//         }
//     }
//     return data;
// }
//
// void deleteDataBuffer(double*** data, int K, int N, int M) {
//     for (int k=0; k<K; ++k) {
//         for (int n=0; n<N; ++n) {
//             delete[] data[k][n];
//         }
//         delete[] data[k];
//     }
//     delete[] data;
// }
//
// void dumpBuffer(double*** data, int K, int N, int M, std::ostream& stream) {
//     for (int k=0; k<K; ++k) {
//         for (int n=0; n<N; ++n)
//             for (int m=0; m<M; ++m)
//                 stream << k << " " << n << " " << m << " "
//                        << data[k][n][m] << "\n";
//         stream << "\n\n";
//     }
// }
//
// class Solver {
// private:
//
//     int world_rank, world_size;
//     int K, N, M;
//     double t_min, t_max, x_min, x_max, y_min, y_max;
//     double dt, dx, dy;
//     double c;
//     int N_local, N_min, N_max;
//
// public:
//
//     Solver(int _world_rank, int _world_size, int _K, int _N, int _M) :
//         world_rank(_world_rank),
//         world_size(_world_size),
//         K(_K), N(_N), M(_N),
//         t_min(0), t_max(30),
//         x_min(0), x_max(30),
//         y_min(0), y_max(30),
//         c(1.001) {
//
//         dt = (t_max-t_min)/K;
//         dx = (x_max-x_min)/N;
//         dy = (y_max-y_min)/M;
//
//         N_local = (int) ceil((double) N / world_size);
//
//         N_min = world_rank * N_local; // inclusive
//         N_max = N_min + N_local;      // exclusive
//
//         // last one may get fewer elements
//         if (world_rank + 1 == world_size) {
//             N_max = N;
//             N_local = N_max - N_min;
//         }
//     }
//
//     double solve(bool dump_solution = false) {
//
//         // +2 for neighbour buffers (left + right)
//         double*** p = createDataBuffer(K, N_local + 2, M);
//
//         double start = MPI_Wtime();
//
//         setInitialConditions(p);
//
//         for (int k=1; k < K-1; ++k) {
//             MPI_Barrier(MPI_COMM_WORLD);
//             exchangeBuffers(p, k);
//             performStep(p, k);
//         }
//
//         const int root = 0;
//         double* results = collectResults(p, root);
//
//         double total_time = MPI_Wtime() - start;
//
//         if (world_rank == root && dump_solution) {
//             constructSolution(results, dump_solution);
//         }
//
//         if (results != nullptr) {
//             delete[] results;
//         }
//
//         deleteDataBuffer(p, K, N_local + 2, M);
//
//         return total_time;
//     }
//
// private:
//
//     void setInitialConditions(double*** p) const {
//
//         for (int n=1; n<=N_local; ++n) {
//             for (int m=0; m<M; ++m) {
//                 p[0][n][m] = 0;
//                 p[1][n][m] = 0;
//             }
//         }
//
//         // P[n,m]
//
//         int xc = N/2;
//         int yc = M/2;
//
//         for (int n : (const int[]){xc-2,xc-1,xc,xc+1,xc+2})
//             for (int m : (const int[]){yc-2,yc-1,yc,yc+1,yc+2})
//                 if (n >= N_min && n < N_max) // here n is `global` index
//                     p[0][n-N_min+1][m] = -5; // +1 for left recv buffer
//
//         if (xc >= N_min && xc < N_max)
//             p[0][xc-N_min+1][yc] = -7;
//
//         exchangeBuffers(p, 0);
//
//         // S[n,m]
//
//         for (int n=1; n<=N_local; ++n) {
//             for (int m=0; m<M; ++m) {
//                 if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
//                     p[1][n][m] = 0;
//                 } else if(m == 0 || m == M-1) {
//                     p[1][n][m] = 0;
//                 }
//                 else {
//                     p[1][n][m] = p[0][n][m] + 0.5 * (dt*dt)*(c*c)*(
//                         (p[0][n-1][m]-2*p[0][n][m]+p[0][n+1][m])/(dx*dx) +
//                         (p[0][n][m-1]-2*p[0][n][m]+p[0][n][m+1])/(dy*dy)
//                     );
//                 }
//             }
//         }
//     }
//
//     void exchangeBuffers(double*** p, int k) const {
//
//         const int tag = 0;
//         MPI_Status status;
//
//         // left-transfer
//         if (world_rank > 0) {
//             MPI_Sendrecv(
//                 p[k][1], M, MPI_DOUBLE, world_rank-1, tag, // send
//                 p[k][0], M, MPI_DOUBLE, world_rank-1, tag, // recv
//                 MPI_COMM_WORLD, &status);
//         }
//
//         // right transfer
//         if (world_rank < world_size-1) {
//             MPI_Sendrecv(
//                 p[k][N_local], M, MPI_DOUBLE, world_rank+1, tag,   // send
//                 p[k][N_local+1], M, MPI_DOUBLE, world_rank+1, tag, // recv
//                 MPI_COMM_WORLD, &status);
//         }
//     }
//
//     void performStep(double*** p, int k) const {
//         for (int n=1; n<=N_local; ++n) {
//             for (int m=0; m<M; ++m) {
//                 if ( (n == 1 && N_min == 0) || (n == N_local && N_max == N) ) {
//                     p[k+1][n][m] = 0;
//                 } else if(m == 0 || m == M-1) {
//                     p[k+1][n][m] = 0;
//                 }
//                 else {
//                     p[k+1][n][m] = 2*p[k][n][m] - p[k-1][n][m]
//                         + (dt*dt)*(c*c)*(
//                             (p[k][n-1][m]-2*p[k][n][m]+p[k][n+1][m])/(dx*dx) +
//                             (p[k][n][m-1]-2*p[k][n][m]+p[k][n][m+1])/(dy*dy)
//                         );
//                 }
//             }
//         }
//     }
//
//     double* collectResults(double*** p, int root) const {
//
//         double* sendbuff = new double[K*N_local*M];
//
//         // linearize p
//         for (int k=0; k<K; ++k)
//             for (int n=0; n<N_local; ++n)
//                 for (int m=0; m<M; ++m)
//                     sendbuff[k*N_local*M + n*M + m] = p[k][n+1][m];
//
//         double* recvbuff = nullptr;
//
//         if (world_rank == root) {
//             recvbuff = new double[K*N*M];
//         }
//
//         MPI_Gather(
//             sendbuff, K*N_local*M, MPI_DOUBLE,
//             recvbuff, K*N_local*M, MPI_DOUBLE,
//             root, MPI_COMM_WORLD);
//
//         delete[] sendbuff;
//         return recvbuff;
//     }
//
//     void constructSolution(double* results, bool print) const {
//
//         double*** solution = createDataBuffer(K, N, M);
//
//         for (int i=0; i<world_size; ++i) {
//
//             for (int k=0; k<K; ++k)
//                 for (int n=0; n<N_local; ++n)
//                     for (int m=0; m<M; ++m) {
//                         solution[k][i*N_local + n][m] =
//                             results[i*K*N_local*M + k*N_local*M + n*M + m];
//                     }
//
//         }
//
//         if (print) {
//             std::ofstream outfile ("results-par.txt");
//             dumpBuffer(solution, K, N, M, outfile);
//         }
//
//         deleteDataBuffer(solution, K, N, M);
//     }
// };

struct RankAndSize {
  int rank;
  int size;
  int root;
  bool isRoot;
  RankAndSize(const MPI_Comm& comm, int root) {
    int world_rank = 0;
    MPI_Comm_rank(comm, &world_rank);
    int world_size = 1;
    MPI_Comm_size(comm, &world_size);
    this->rank = world_rank;
    this->size = world_size;
    this->root = root;
    this->isRoot = world_rank == root;
  }
};

template<typename T>
std::string vecToString(const std::vector<T>& vec, const std::string& delim) {
  if (vec.empty()) return "";
  std::ostringstream stream;
  std::ostream_iterator<T> out(stream, delim.c_str());
  std::copy(vec.begin(), vec.end(), out);
  std::string s = stream.str();
  s.erase(s.length()-delim.length());
  return s;
}

std::vector<int> distributeInitialData(const MPI_Comm& comm,
                                       int M,
                                       int dataRange) {
  RankAndSize world(comm, 0);
  std::vector<int> buffer(M, 0);

  srand(time(nullptr));

  auto nextInt = [&]() { return rand() % dataRange; };

  auto fillBufferWithRandomData = [&]() {
    std::generate(buffer.begin(), buffer.end(), nextInt);
  };

  const int tag = 0;

  for (int target=0; target<world.size; ++target) {

    if (target == world.root) {
      continue;
    }

    if (world.isRoot) {
      fillBufferWithRandomData();
      MPI_Send(buffer.data(),
               buffer.size(),
               MPI_INT,
               target,
               tag,
               comm);
    } else if (world.rank == target) {
      MPI_Recv(buffer.data(),
               buffer.size(),
               MPI_INT,
               world.root,
               tag,
               comm,
               MPI_STATUS_IGNORE);
    }
  }

  if (world.isRoot) {
    fillBufferWithRandomData();
  }

  MPI_Barrier(comm);
  return buffer;
}

std::vector<int> parallelBufferExchangeStep(const MPI_Comm& comm,
                                        std::vector<int>& buffer,
                                        int sender,
                                        int receiver) {
  RankAndSize world(comm, 0);

  const int tag = 0;

  if (world.rank == sender) {
    MPI_Send(buffer.data(),
             buffer.size(),
             MPI_INT,
             receiver,
             tag,
             comm);
    return std::vector<int>(0);
  } else if (world.rank == receiver) {
    int count = 0;
    MPI_Status status;
    MPI_Probe(sender, tag, comm, &status);
    MPI_Get_count(&status, MPI_INT, &count);
    std::vector<int> recvBuffer(count, 0);
    MPI_Recv(recvBuffer.data(),
             count,
             MPI_INT,
             sender,
             tag,
             comm,
             MPI_STATUS_IGNORE);
    return recvBuffer;
  } else {
    return std::vector<int>(0);
  }
}

int calculatePivot(std::vector<int>& v) {

  int dim = v.size();
  int a = v[rand()%dim];
  int b = v[rand()%dim];
  int c = v[rand()%dim];
  return std::max(std::min(a, b), std::min(std::max(a, b), c));

  // auto result = std::minmax_element(v.begin(), v.end());
  // return (*result.second + *result.first) / 2;
}

std::vector<int> paralellQuicksortExchange(const MPI_Comm& comm,
                                           std::vector<int>& buffer) {
  RankAndSize world(comm, 0);

  const int dim = log2(world.size);
  const int mask = 1 << (dim - 1);

  int pivot = 0;
  if (world.isRoot) {
    pivot = calculatePivot(buffer);
    // printf("pivot: %d\n", pivot);
  }
  MPI_Bcast(&pivot, 1, MPI_INT, world.root, comm);

  const int partner = world.rank ^ mask;

  const int tag = 0;

  int firstSender = std::min(world.rank, partner);
  int secondSender = std::max(world.rank, partner);

  const std::vector<int>& recvBySecondSender =
    parallelBufferExchangeStep(comm, buffer, firstSender, secondSender);

  const std::vector<int>& recvByFirstSender =
    parallelBufferExchangeStep(comm, buffer, secondSender, firstSender);

  const std::vector<int>& recvBuffer =
    (world.rank == firstSender) ? recvByFirstSender : recvBySecondSender;

  std::vector<int> newBuffer;

  int takesPivot = std::min(world.rank, partner);

  auto filter = [&](int i) {
    if (i == pivot) {
      return world.rank == takesPivot;
    } else {
      return (i-pivot)*(world.rank-partner) > 0;
    }
  };

  std::back_insert_iterator<std::vector<int>> bii(newBuffer);

  std::copy_if(buffer.begin(), buffer.end(), bii, filter);
  std::copy_if(recvBuffer.begin(), recvBuffer.end(), bii, filter);

  // printf("%d buff: %s\n", world.rank, vecToString(buffer, ", ").c_str());
  // printf("%d recv: %s\n", world.rank, vecToString(recvBuffer, ", ").c_str());

  return newBuffer;
}

std::vector<int> paralellQuicksortStep(const MPI_Comm& comm,
                                       std::vector<int>& buffer) {
  RankAndSize world(comm, 0);

  if (world.size > 1) {
    std::vector<int> newBuffer = paralellQuicksortExchange(comm, buffer);
    MPI_Comm newComm = comm;
    MPI_Comm_split(newComm, world.rank < world.size/2 , world.rank, &newComm);
    return paralellQuicksortStep(newComm, newBuffer);
  } else {
    return buffer;
  }
}

int main(int argc, char** argv) {

    if (argc < 2) {
        MPI_Abort(MPI_COMM_WORLD, 1);
    }

    // data per-processor
    const int M = strtol(argv[1], nullptr, 10);

    const int dataRange = 100;

    MPI_Init(&argc, &argv);

    MPI_Comm comm = MPI_COMM_WORLD;

    RankAndSize world(comm, 0);

    double timeStart = MPI_Wtime();

    auto buff = distributeInitialData(comm, M, dataRange);

    // printf("initial %d: %s\n", world.rank, vecToString(buff, ", ").c_str());

    auto newBuff = paralellQuicksortStep(comm, buff);

    // printf("after %d: %s\n", world.rank, vecToString(newBuff, ", ").c_str());

    std::sort(newBuff.begin(), newBuff.end());

    // printf("sorted %d: %s\n", world.rank, vecToString(newBuff, ", ").c_str());

    MPI_Barrier(comm);

    double timeEnd = MPI_Wtime();

    if (world.isRoot) {
      printf("%f\n", timeEnd - timeStart);
    }

    MPI_Finalize();

    return 0;
}
