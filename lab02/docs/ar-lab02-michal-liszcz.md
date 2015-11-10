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
