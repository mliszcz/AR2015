
CXX=g++
CFLAGS=-std=c++11

MPI_CXX=mpic++
MPI_CFLAGS=-std=c++0x -Werror -Wall -Wno-unused-variable -Wno-literal-suffix

%-seq.o: %-seq.cpp
	$(CXX) $(CFLAGS) -o $@ $< $(LDFLAGS)

%-par.o: %-par.cpp
	$(MPI_CXX) $(MPI_CFLAGS) -o $@ $< $(LDFLAGS)

all: lab04-par.o

clean:
	rm -f *.o
