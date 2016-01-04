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

int calculatePivot(std::vector<int>& v) {

  // int dim = v.size();
  // int a = v[rand()%dim];
  // int b = v[rand()%dim];
  // int c = v[rand()%dim];
  // return std::max(std::min(a, b), std::min(std::max(a, b), c));

  auto result = std::minmax_element(v.begin(), v.end());
  return (*result.second + *result.first) / 2;
}

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

std::vector<int> distributeInitialData(const MPI_Comm& comm,
                                       int M,
                                       int dataRange) {
  RankAndSize world(comm, 0);
  std::vector<int> buffer(M, 0);

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
      printf("usage: %s <data-per-processor> <data-range> <seed>", argv[0]);
      MPI_Abort(MPI_COMM_WORLD, 1);
    }

    // data per-processor
    const int M = strtol(argv[1], nullptr, 10);

    int dataRange = 100;
    if (argc > 2) {
      dataRange = strtol(argv[2], nullptr, 10);
    }

    time_t seed = time(nullptr);
    if (argc > 3) {
      seed = strtol(argv[3], nullptr, 10);
    }

    srand(seed);

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
