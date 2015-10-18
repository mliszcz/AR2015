
CXX=g++
CFLAGS=-std=c++11

MPI_CXX=mpic++
MPI_CFLAGS=-std=c++0x -Werror -Wall -Wno-unused-variable

%-seq.o: %-seq.cpp
	$(CXX) $(CFLAGS) -o $@ $< $(LDFLAGS)

%-par.o: %-par.cpp
	$(MPI_CXX) $(MPI_CFLAGS) -o $@ $< $(LDFLAGS)

all: lab01-seq.o lab01-par.o

plot: lab01-seq.o
	./lab01-seq.o
	gnuplot plot.gnuplot

clean:
	rm -f *.o
