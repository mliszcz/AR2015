% Algorytmy równoległe 2015 (zad. 2)
% Michał Liszcz
% 2015-11-10

---
geometry: margin=6em
header-includes:
    - \usepackage{mathrsfs}
    - \usepackage{amssymb}
    - \usepackage{empheq}
    - \usepackage{braket}
    - \usepackage{empheq}
    - \usepackage{graphicx}
    - \usepackage{float}
    - \usepackage{color}
    - \usepackage{listings}
---

\newpage

# Wstęp

Treść zadania została podana przez prowadzącego:

Zaimplementować algorytm obliczania spójnej składowej (*connected components*)
zgodnie z modelem *Pregel* \cite{pregel}. Zasada działania algorytmu:

1. Na początku każdy wierzchołek oznaczamy innym znacznikiem (liczbą).
1. Każdy wierzchołek wysyła swój znacznik do wszystkich sąsiadów.
1. Jeżeli minimum zotrzymanych zniaczników jest mniejsze od własnego,
   wierzchołek zastępuje swój znacznik przez minimum otrzymanych.
1. Powtarzamy krok 2 aż przestaną zachodzić zmiany

# Implementacja z wykorzystaniem Apache Spark

Implementacja bazuje na strukturze \texttt{org.apache.spark.rdd.RDD}.

## Struktura grafu

Graf zadany jest jako lista **skierowanych** krawędzi, łączących wierzchołki
o zadanych indeksach, przykładowo:

\begin{lstlisting}[frame=single]
0 1
0 2
0 3
2 4
5 6
6 7
7 4
8 9

\end{lstlisting}

*TODO: rysunek grafu*

Struktura taka ma bezpośrednie przełożenie na \texttt{RDD}:

\begin{lstlisting}[frame=single]
type IntMapRDD = RDD[(Int, Int)]
\end{lstlisting}

Taki graf można łatwo przekształcić w graf nieskierowany:

\begin{lstlisting}[frame=single]
def makeUndirected(edges: IntMapRDD) =
    (edges ++ edges.map(_ .swap)).distinct
\end{lstlisting}

## Realizacja algorytmu

Dążymy do zdefiniowania funkcji transformującej graf w zbiór spójnych
składowych:

\begin{lstlisting}[frame=single]
def connectedComponents(graph: IntMapRDD): RDD[Iterable[Int]]
\end{lstlisting}

Definiujemy połączenia w grafie jako mapę:
\texttt{K -> \textit{zbiór wierzchołki wychodzących z} K} oraz wprowadzamy
wagi wierzchołków (każdy wierzchołek zaczyna z wagą równą jego indeksowi):

\begin{lstlisting}[frame=single]
val connections = graph.groupByKey
val initWeights = connections map { case (k, _ ) => (k, k) }
\end{lstlisting}

W każdym kroku iteracji zmieniamy wagi: każdy z wierzchołków \texttt{K}
otrzymuje wagę będącą minimum z wag jego sąsiadów (i jego samego). Taką
operację należy wykonać, unikając zagnieżdżonych operacji na \texttt{RDD}.
Można to osiągnąć następująco:

1. \texttt{join} (względem klucza) zbiorów \texttt{connections}
   i \texttt{weights},
2. pobranie wartości (*values*) powyższego \texttt{RDD}. Otrzymamy pary
   zawierające wagę wierzchołka \texttt{K} i listę wierzchołków do których
   ta waga będzie przesłana,
3. każdy element z poprzedniego wyniku mapujemy na pary wierzchołek -> nowa
   waga, wynik wypłaszczamy (*flatten*)
4. poprzedni wynik grupujemy po kluczu, otrzymamy pary wierzchołek -> lista
   wag które otrzyma od sąsiadów
5. mapujemy wartości w poprzednim wyniku, wybierając minimum z zadanej listy

Punkty 4. i 5. można zastąpić jedną operacją:
\texttt{combineByKey(weight => weight, Math.min, Math.min)}, lub prościej:
\texttt{reduceByKey(Math.min)}

Implementacja tych operacji:

\begin{lstlisting}[frame=single]
val newWeights = connections.join(weights).values.flatMap {
    case (indices, weight) => indices map { (_ , weight) }
} reduceByKey(Math.min)
\end{lstlisting}

Wyliczone nowe wartości wag nie uwzględniają poprzedniej wagi (jeżeli jest
najmniejsza, wierzchołek nie powinien zmieniać wagi). Należy złączyć oba
zbiory wag, dla każdego wierzchołka wybierając mniejszą wagę:

\begin{lstlisting}[frame=single]
val mergedWeights = weights.join(newWeights)
                .mapValues((Math.min _ ).tupled)
\end{lstlisting}

Powyżej zdefiniowane operacje należy powtarzać, dopóki w wagach zachodzą
zmiany. Oczywistym rozwiązaniem wydaje się być rekurencja:

\begin{lstlisting}[frame=single]
@tailrec
def performStep(weights: IntMapRDD): IntMapRDD = {

    val newWeights = connections.join(weights).values.flatMap {
        case (indices, weight) => indices map { (_ , weight) }
    } reduceByKey(Math.min)

    val mergedWeights = weights.join(newWeights)
        .mapValues((Math.min _ ).tupled)

    if (weights.subtract(mergedWeights).count == 0)
        mergedWeights else performStep(mergedWeights)
}
\end{lstlisting}

**UWAGA:** *we współczesnych wersjach Apache Spark dostępna jest metoda
\texttt{RDD.isEmpty}. Można jej użyć zamiast przyrównywania rozmiaru do zera.*

Wynik otrzymany z powyższej rekurencji można ostatecznie zamienić na zbiory
spójnych składowych transformacją:

\begin{lstlisting}[frame=single]
performStep(initWeights).map(_ .swap).groupByKey.map(_ ._ 2)
\end{lstlisting}

**UWAGA:** *w rozwiązaniu należy pamiętać o cache-owaniu zbiorów wielokrotnie
używanych.*

# Testy lokalne

W celu zbadania wpływu ilości procesorów na czas rozwiązywania problemu,
uruchomiłem program w konfiguracji lokalnej na maszynie z procesorem
Intel Core i5-4200u, 2C/4T. Docelowo testy będą przeprowadzone na klastrze
Zeus w ACK Cyfronet AGH.

Do testów wybrałem zbiór *ca-GrQc*
\footnote{\url{https://snap.stanford.edu/data/ca-GrQc.html}}
o 5242 wierzchołkach i 14496 krawędziach.

Ilość procesorów dostępnych dla Apache Spark zmieniałem w zakresie 1-4.
W każdym wypadku pomiar powtórzyłem czterokrotnie. Wyniki przedstawia
poniższa tabela.

| wątki | czas  | błąd  |
|:-----:|:-----:|:-----:|
| 1     | 4.366 | 1.308 |
| 2     | 3.834 | 1.215 |
| 3     | 3.853 | 1.488 |
| 4     | 3.615 | 1.527 |

Widać nieznaczny wzrost wydajności.

# Testy na klastrze Zeus

**Z powodu problemów z dostępem do klastra Zeus, testy przeprowadziłem na
maszynie wyposażonej w dwa procesory Intel Xeon E5-2697 v2
\footnote{\url{http://ark.intel.com/products/75283/Intel-Xeon-Processor-E5-2697-v2-30M-Cache-2\_70-GHz}}
(12C/24T, łącznie 48 logicznych procesorów).**

\begin{lstlisting}[frame=single]
$lscpu
\end{lstlisting}

**UWAGA**: Na potrzeby testów konfigurowałem **lokalną instalację**
Apache Spark (\texttt{--master local[X]}).

## Graf testowy

Do testów wydajności wykorzystałem graf *web-Stanford*
\footnote{\url{https://snap.stanford.edu/data/web-Stanford.html}}.

Na graf testowy składało się 281'903 wierzchołków i 2'312'497 krawędzi.

## Losowo generowane grafy

Drugi wariant zakładał testy na losowo wygenerowanym grafie. Wykorzystałem
obiekt \texttt{GraphGenerators}.
\footnote{\url{http://spark.apache.org/docs/latest/api/scala/\#org.apache.spark.graphx.util.GraphGenerators$}}

Generator pozwala na stworzenie grafu o zadanej liczbie wierzchołków i
losowych krawędziach, przy czym stopnie wierzchołków grafu są losowane z
rozkładem logarytmicznie normalnym.

Wygenerowany graf można przekształcić na opisany wcześniej \texttt{IntMapRDD}:

\begin{lstlisting}[frame=single]
GraphGenerators.logNormalGraph(sc, vertices, seed = 1).edges.map {
    edge => (edge.srcId.toInt, edge.dstId.toInt)
}
\end{lstlisting}

Przyjąłem stałą wartość dla ziarna generatora pseudolosowego, aby zapewnić
porównywalność wyników uzyskanych w kolejnych uruchomieniach.

## Wyniki

Dla ustalonego rozmiaru klastra mierzyłem czas wyznaczania spójnych składowych.
W każdym przypadku pomiar powtórzyłem czterokrotnie. Jako niepewność przyjąłem
odchylenie standardowe średniej otrzymanych wyników.

Wykorzystałem następujące definicje przyspieszenia $S(x,p)$ i efektywności
$E(x,p)$:

\begin{equation}
S(x,p) = \frac{T(x,1)}{T(x,p)}
\end{equation}

\begin{equation}
E(x,p) = \frac{S(x,p)}{p}
\end{equation}

W powyższych definicjach $x$ oznacza rozmiar problem, natomiast $p$ to liczba
procesorów. Niepewności oszacowałem metodą różniczki zupełnej.

\begin{figure}[H]
    \centering
    \includegraphics[width=0.7\textwidth]{{../results/images/t440-random-5000.txt-time}.png}
    \caption{Czas wykonania programu - losowy graf o 5000 wierzchołkach
        (logNormalGraph)}
    \label{fig:t440-random-5000-time}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.7\textwidth]{{../results/images/t440-random-5000.txt-speedup}.png}
    \caption{Przyspieszenie programu - losowy graf o 5000 wierzchołkach
        (logNormalGraph)}
    \label{fig:t440-random-5000-speedup}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.7\textwidth]{{../results/images/t440-random-5000.txt-efficiency}.png}
    \caption{Efektywność programu - losowy graf o 5000 wierzchołkach
        (logNormalGraph)}
    \label{fig:t440-random-5000-efficiency}
\end{figure}

# Podział danych zgodnie z PCAM



# Dyskusja wyników

*TODO*

\begin{thebibliography}{9}

\bibitem{pregel}
  R. Zadeh,
  \emph{Distributed Algorithms and Optimizations},
  \url{http://stanford.edu/~rezab/dao/notes/lec8.pdf},
  2008.

\bibitem{foster}
  Ian Foster,
  \emph{Designing and Building Parallel Programs},
  \url{www.mcs.anl.gov/~itf/dbpp/}.

\end{thebibliography}
