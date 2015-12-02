FLAGS=--number-sections --table-of-contents

%.pdf: %.md
	pandoc $(FLAGS) -o $@ $<

all: ar-lab03-michal-liszcz.pdf

clean:
	rm -f *.pdf
