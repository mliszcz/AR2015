
CXX=g++
CFLAGS=-std=c++11

%.o: %.cpp
	$(CXX) $(CFLAGS) -o $@ $< $(LDFLAGS)

all: lab01-seq.o

plot: lab01-seq.o
	./lab01-seq.o
	gnuplot plot.gnuplot

clean:
	rm -f *.o
