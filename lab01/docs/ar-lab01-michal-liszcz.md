% Algorytmy równoległe 2015 (zad. 1)
% Michał Liszcz
% 2015-10-28

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

Zaproponować algorytm równoległy wyliczający kolejne położenia drgającej
membrany rozpiętej na kwadracie o ustalonym boku. Boki membrany są sztywno
zamocowane (warunki brzegowe). Należy ustalić położenie początkowe
i prędkość $\left(\frac{\partial p}{\partial t}\right)_ {t=0}$
(warunki początkowe).

Zastosować metodę różnicową do równania:
\begin{equation}
\label{eq:initial}
\frac{\partial^2 p}{\partial x^2} + \frac{\partial^2 p}{\partial y^2}
- \frac{\rho}{T} \frac{\partial^2 p}{\partial t^2} = 0
\end{equation}

gdzie $p(x,y)$ - położenie punktu membrany, $\rho$ - gęstość powierzchniowa,
$T$ - napięcie membrany.

# Analiza problemu

Równanie \eqref{eq:initial} to klasyczne równanie falowe. Podstawiając
$\frac{\rho}{T} \coloneqq \left(c^2\right)^{-1}$, można zapisać je w
standardowej postaci:
\begin{equation}
\left[\partial_{tt} - c^2 \nabla^2 \right] p(t,x,y) = 0
\end{equation}

Rozwiązania poszukujemy w obszarze $\Omega$:
\begin{equation}
\begin{aligned}
\label{eq:domain}
\Omega &= [t_\text{min},t_\text{max}] \times
          [x_\text{min},x_\text{max}] \times
          [y_\text{min},y_\text{max}] \\
W &= [x_\text{min},x_\text{max}] \times
      [y_\text{min},y_\text{max}]
\end{aligned}
\end{equation}

Zadane są następujące warunki brzegowe:
\begin{equation}
\label{eq:cond_boundary}
p(t,x,y) = 0 \qquad
    \forall t \in [t_\text{min},t_\text{max}],
    \forall (x,y) \in \partial W
\end{equation}

Oraz warunki początkowe (membrana jest w pozycji $P(x,y)$ i porusza się
z prędkością $S(x,y)$):
\begin{equation}
\label{eq:cond_initial}
\left.
\begin{aligned}
p(0,x,y) &= P(x,y) \quad\\
p_t(0,x,y) &= S(x,y)
\end{aligned}
\right\} \qquad \forall (x,y) \in W
\end{equation}

# Metoda różnic skończonych

Poszukujemy rozwiązania numerycznego metodą różnic skończonych.

## Dyskretyzacja dziedziny

W obszarze $\Omega$ wprowadzamy siatkę dyskretnych punktów:
\begin{equation}
\left.
\begin{aligned}
\Delta t &= \frac{t_\text{max}-t_\text{min}}{K} \\
\Delta x &= \frac{x_\text{max}-x_\text{min}}{N} \\
\Delta y &= \frac{y_\text{max}-y_\text{min}}{M} \qquad
\end{aligned}
\right\}
\end{equation}

\begin{equation}
\left.
\begin{aligned}
t_k &= x_\text{min} + k \Delta t, \qquad k = 0,1,...,K \\
x_n &= x_\text{min} + n \Delta x, \qquad n = 0,1,...,N \\
y_m &= y_\text{min} + m \Delta y, \qquad m = 0,1,...,M \qquad
\end{aligned}
\right\}
\end{equation}

Oznaczamy wartość $p$ w punktach siatki:
\begin{equation}
p(t_k, x_n, y_m) = p^k_{n,m}
\end{equation}

## Dyskretyzacja równania

Operatory różniczkowe występujące w równaniu zastępujemy operatorami
różnicowymi. Dla pochodnych pierwszego rzędu zapisujemy różnicę centralną
(średnią z ilorazów różnicowyh "w przód" i "w tył"), natomiast pochodne
drugiego rzędu otrzymujemy po odjęciu stronami rozwinięć $p(x)$ w szereg
Taylora wokół $x_0$, kładąc w nich $x=x_0 \pm \Delta x$.
Wyprowadzenia poniższych przybliżeń można znaleźć w literaturze
\cite{complex-pde}.

\begin{equation}
\left.
\begin{aligned}
\partial_{t} p(t_k, x_n, y_m) &\approx
    \frac{p^{k-1}_{n,m} - p^{k+1}_{n,m}}{2 \Delta t}
    &\coloneqq D_{t} p^k_{n,m} \qquad \\
\partial_{tt} p(t_k, x_n, y_m) &\approx
    \frac{p^{k-1}_{n,m} - 2 p^k_{n,m} + p^{k+1}_{n,m}}{(\Delta t)^2}
    &\coloneqq D_{tt} p^k_{n,m} \qquad \\
\partial_{xx} p(t_k, x_n, y_m) &\approx
    \frac{p^k_{n-1,m} - 2 p^k_{n,m} + p^k_{n+1,m}}{(\Delta x)^2}
    &\coloneqq D_{xx} p^k_{n,m} \qquad \\
\partial_{yy} p(t_k, x_n, y_m) &\approx
    \frac{p^k_{n,m-1} - 2 p^k_{n,m} + p^k_{n,m+1}}{(\Delta y)^2}
    &\coloneqq D_{yy} p^k_{n,m} \qquad
\end{aligned}
\right\}
\end{equation}

Równanie \eqref{eq:initial} przyjmuje postać równania różnicowego:
\begin{equation}
\frac{p^{k-1}_{n,m} - 2 p^k_{n,m} + p^{k+1}_{n,m}}{(\Delta t)^2} =
c^2 \left(
  \frac{p^k_{n-1,m} - 2 p^k_{n,m} + p^k_{n+1,m}}{(\Delta x)^2} +
  \frac{p^k_{n,m-1} - 2 p^k_{n,m} + p^k_{n,m+1}}{(\Delta y)^2}
\right)
\end{equation}

Poszukujemy wartośći $p$ w chwili $k+1$, zakładając że znane jest całe
rozwiązanie w chwilach poprzednich:
\begin{equation}
\label{eq:discrete}
p^{k+1}_{n,m} = 2 p^k_{n,m} - p^{k-1}_{n,m} + (\Delta t)^2 c^2
    (D_{xx} + D_{yy})p^k_{n,m}
\end{equation}

## Warunki brzegowe

Równanie \eqref{eq:cond_boundary} prowadzi do następujących warunków
brzegowych:
\begin{equation}
p^k_{0,m} = p^k_{N,m} = p^k_{n,0} = p^k_{n,M} = 0 \qquad \forall k,n,m
\end{equation}

Warunki początkowe \eqref{eq:cond_initial} są zadane przez odwzorowania
$P$ i $S$:
\begin{equation}
\begin{aligned}
\label{eq:init_bound_t0}
p^0_{n,m} &= P_{n,m} \\
D_{t} p^0_{n,m} &= S_{n.m}
\end{aligned}
\end{equation}

Drugie z powyższych równań rozpisujemy korzystając z definicji operatora
$D_t$, kładziemy $k=0$ w \eqref{eq:discrete}, a następnie eliminujemy
ujemny czas, łącząc ze sobą te dwa równania:
\begin{equation}
\begin{aligned}
\label{eq:init_bound_t1}
p^{-1}_{n,m} - p^{1}_{n,m} &= 2 \Delta t S_{n,m} \\
p^{1}_{n,m} &= 2 p^0_{n,m} - p^{-1}_{n,m} + (\Delta t)^2 c^2
    (D_{xx} + D_{yy})p^0_{n,m} \\
p^{1}_{n,m} &= p^0_{n,m} - \Delta t S_{n,m} + \frac{1}{2}(\Delta t)^2 c^2
    (D_{xx} + D_{yy})p^0_{n,m}
\end{aligned}
\end{equation}

# Algorytm sekwencyjny

Algorytm sekwencyjny operuje na trójwymiarowej tablicy liczb zawierającej
wartości $p$ w trójkach $(k,n,m)$.

W pierwszej kolejności ustawiane są wartości dla $k=0$, zgodnie
z \eqref{eq:init_bound_t0}. Następnie dla $k=1$, przy użyciu
\eqref{eq:init_bound_t1}. Pozostała część tablicy uzupełniana jest na podstawie
zadanego równania różnicowego \eqref{eq:discrete}.

W celu sprawdzenia poprawności rozwiązania, uruchomiłem program z następującymi
parametrami:
\begin{equation}
\begin{aligned}
t_\text{min} = x_\text{min} = y_\text{min} = 0 \\
t_\text{max} = x_\text{max} = y_\text{max} = 30 \\
K = 100, \qquad N,M = 30 \\
c = 1
\end{aligned}
\end{equation}

Przyjąłem nieznaczne początkowe zaburzenie na środku membrany:
\begin{equation}
\begin{aligned}
P_{n,m} &= 5 \qquad \forall (n,m) \in \{13,14,15,16,17\}^2 \\
P_{15,15} &= 7
\end{aligned}
\end{equation}

W pozostałych punktach membrana jest w stanie równowagi: $P_{n,m} = 0$.
W każdym punkcie membrana początkowo spoczywa ($S_{n,m} = 0 \quad \forall n,m$).

\begin{figure}[H]
    \centering
    \includegraphics[width=0.7\textwidth]{output}
    \caption{Wizualizacja fali rozchodzącej się w membranie.}
    \label{fig:output_image}
\end{figure}

Otrzymany wynik jest zgodny z przewidywaniami. Do sprawozdania dołączona jest
animacja przedstawiająca propagację fali w czasie.

# Algorytm równoległy

W dalszej części zostanie przedstawiony algorytm równoległy zgodny z metodologią
PCAM.

Algorytm sekwencyjny uzupełniał trójwymiarową tablicę *warstwami*, kolejne
iteracje były parametryzowane zmienną czasową (parametr $k$). W każdej iteracji
generowana była dwuwymiarowa tablica reprezentująca wartości w punktach siatki
w ustalonej chwili $t_k$. Można ją utożsamiać z obszarem $W$ gdzie zdefiniowano
problem \eqref{eq:domain}. Kolejne punkty opisują próbę efektywnego
zrównoleglenia tego algorytmu.

## Partitioning

Ze względu na model problemu, najlepiej dokonać tutaj dekompozycji domenowej,
poprzez podzielenie *danych* na porcje, które prztwarzane będą równolegle.

Najmniejszym, niepodzielnym zadaniem jest obliczenie pojednycznego elementu
z trójwymiarowej tablicy $p^k_{n,m}$. Takich elementów jest
$K \times N \times M$.

Dekompozycja funkcjonalna nie ma tutaj zastosowania - jest tylko jeden rodzaj
operacji.

## Communication

Stosując dekompozycję zaproponowaną w poprzednim punkcie, można łatwo określić
wymagania dotyczące komunikacji. Siatka użyta do dyskretyzacji przestrzeni
narzuca strukturę komunikacyjną. Jest to komunikacja lokalna - do obliczenia
wartości komórki $p^{k+1}_{n,m}$ należy znać wartości:
\begin{equation}
\label{eq:communication}
p^{k}_{n,m}, \qquad p^{k-1}_{n,m}, \qquad p^k_{n-1,m}, \qquad p^k_{n+1,m},
\qquad p^k_{n,m-1}, \qquad p^k_{n,m+1}
\end{equation}

Daje to sześć wymian komunikatów dla każdej komórki.

## Agglomeration

Przedstawiony w poprzednich punktach sposób podziału zadań i wynikający
z niego schemat komunikacji jest bardzo nieefektywny.

W typowych zastosowaniach ilość zadań będzie kilka rzędów wielkości większa
od liczby procesorów. Pojedyncze zadanie jest bardzo proste - składa się
z kilku operacji dodawania i mnożenia.

Należy pogrupować zadania tak, by były wykonywane w sposób najbardziej
efektywny na maszynie wyposażonej w kilkanaście procesorów.

W pierwszej kolejności zakładamy, że dane będziemy dzielić względem
*przestrzeni*, to znaczy, że najmniejszą porcją danych z trójwymiarowej
tablicy $p^k_{n,m}$ będzie zbiór komórek o ustalonych indeksach $n$ i $m$,
natomiast $k$ będzie dowolny:
\begin{equation}
E_{n,m} = \left\{p^k_{n,m} : k = 0,1,...,K\right\}
\end{equation}

Algorytm równoległy będzie działał iteracyjnie względem *czasu*, podzielonego
na $K$ iteracji. W każdej iteracji zostanie wyliczona wartość jednej komórki
$p^k_{n,m}$.

Porcja danych $E_{n,m}$ to jednowymiarowa tablica. Daje to mniej zadań -
$N \times M$. Dodatkowo, wyliczenie pojedynczej komórki z $E$ wymaga już tylko
czterech aktów komunikacji.

## Mapping

Zadania $E_{n,m}$ należy przypisać do fizycznych procesorów, na których będą
wykonywane. Zadania mają identyczny *rozmiar* - można więc podzielić je równo
na $Z$ procesorów, pamiętając o wymaganiach komunikacyjnych
\eqref{eq:communication}. Jeden procesor powinien obsługiwać zadania
sąsiadujące ze sobą przestrzennie - o kolejnych ideksach $n$ i $m$.

Optymalnym sposobem podziału jest przydzielenie pojedynczemu procesorowi kilku
całych, sąsiednich *wierszy* (ciągły obszar pamięci) ze zbioru $\{E_{n,m}\}$.

Niech $Q_{n}$ oznacza zbiór zadań $E_{n,m}$ w $n$-tym wierszu przestrzeni:
\begin{equation}
Q_n = \left\{E_{n,m} : m = 0,1,...,M\right\}
\end{equation}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.4\textwidth]{3d-grid}
    \caption{Podział siatki na zadania. Odpowiednimi kolorami oznaczono
        zadania: \textcolor{red}{$p^k_{n,m}$},
        \textcolor{green}{$E_{n,m}$}, \textcolor{blue}{$Q_{n}$}.
    }
    \label{fig:3dgrid}
\end{figure}

Przyjmujemy następujący podział zadań między procesory:
\begin{equation}
\begin{aligned}
Z_1 &= \left\{Q_0, Q_1, ..., Q_{|Z_1|-1}\right\} \\
Z_2 &= \left\{Q_{|Z_1|+0}, Q_{|Z_1|+1}, ..., Q_{|Z_1|+|Z_2|-1}\right\} \\
...
\end{aligned}
\end{equation}

W zależności od mocy obliczeniowej procesorów, podział może być dokonany na
nierówne części. W testowanym przypadku każdy z procesorów otrzymał taką samą
liczbę zadań.

## Opis algorytmu

W algorytmie równoległym każdy procesor będzie wykonywał w pętli następujące
operacje:

1. pobranie od sąsiednich procesorów informacji o wartościach obliczonych
   w poprzednim kroku na brzegach ich obszarów,
2. przekazanie sąsiadom informacji o wartościach obliczonych w poprzednim
   kroku na brzegu swojego obszaru,
3. wyliczenie wartości dla aktualnego kroku na całym obsługiwanym obszarze.

Należy uważać na zakleszczenie w punktach 1. i 2.. Dwa procesory obsługujące
sąsiadujące porcje danych mogą wzajemnie czekać na dane z poprzedniej iteracji
sąsiedniego procesora. Istnieje kilka rozwiązań:
można przykładowo wymusić ustaloną kolejność operacji 1. i 2. w komunikujacych
się procesorach, lub użyć komunikacji asynchronicznej.

W implementacji
z wykorzystaniem MPI użyta zostanie funkcja \texttt{MPI\_Sendrecv}, która
w abstrakcyjny sposób ukrywa detale komunikacji asynchronicznej
(\texttt{MPI\_Isend} oraz \texttt{MPI\_Irecv}).


# Analiza wyników

Proponowany algorytm równoległy został przetestowany na klastrze Zeus
w ACK Cyfronet. Testy obejmowały pomiary standardowych metryk programów
równoległych: przyspieszenia, efektywności oraz oszacowania wpływu komunikacji
na wydajność.

## Metryki podstawowe - pomiar czasu

Pierwszy etap testów obejmował pomiar czasu, przyspieszenia i efektywności w
wersji podstawowej. Testy przeprowadziłem osobno dla siatek o rozmiarach
100x300x300, 100x600x600, 100x1200x1200 oraz 100x2400x2400. W dalszej części
sprawozdania $n$ oznacza *rozmiar problemu* - w tym przypadku liczbę punktów
siatki.

Liczbę procesorów zmieniałem w zakresie 1-12. *W przyszłości planowane są testy
na klastrze o większej liczbie procesorów.*

Dla każdej ustalonej liczby procesorów $p$ zmierzyłem czas $T(n,p)$ wykonania
programu. Pomiar powtórzyłem czterokrotnie a następnie uśredniłem. Jako
niepewność pomiaru $u(T(n,p))$ przyjąłem odchylenie standardowe średniej
z uzyskanych wartości. Wyniki uzyskane dla dwóch pierwszych siatek to bardzo
krótkie czasy i są obarczone dużymi niepewnościami pomiarowymi, dlatego pomijam
je w dyskusji wyników.

**Pomiary wykonałem z wykorzystaniem implementacji MPICH 3.0.4.**

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-time-600.png}
    \caption{Czas wykonania programu w funkcji liczby procesorów
        dla siatki 100x600x600}
    \label{fig:plot-std-tim-600}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-time-1200.png}
    \caption{Czas wykonania programu w funkcji liczby procesorów
        dla siatki 100x1200x1200}
    \label{fig:plot-std-tim-1200}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-time-2400.png}
    \caption{Czas wykonania programu w funkcji liczby procesorów
        dla siatki 100x2400x2400}
    \label{fig:plot-std-tim-2400}
\end{figure}

Wykresy \ref{fig:plot-std-tim-600}, \ref{fig:plot-std-tim-1200} oraz
\ref{fig:plot-std-tim-2400} prezentują gładką krzywą, malejącą ze wzrostem
liczby procesorów. Można spróbować "dopasować" do tych danych funkcję:

\begin{equation}
T(n,p) = \frac{A(n)}{p} + B(n)
\label{eq:inv01}
\end{equation}

Stała $B$ wynika z istnienia części sekwencyjnej - wykresy nie zbiegają do $0$.

Zmierzone wartości czasu wydają się być sensowne i zgodne z oczekiwaniami.

## Metryki podstawowe - pomiar przyspieszenia i efektywności

Wykorzystałem następujące definicje przyspieszenia $S(n,p)$ i efektywności
$E(n,p)$:

\begin{equation}
S(n,p) = \frac{T(n,1)}{T(n,p)}
\end{equation}

\begin{equation}
E(n,p) = \frac{S(n,p)}{p}
\end{equation}

Niepewności oszacowałem metodą różniczki zupełnej.

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-speedup-1200.png}
    \caption{Przyspieszenie w funkcji liczby procesorów
        dla siatki 100x1200x1200}
    \label{fig:plot-std-spd-1200}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-speedup-2400.png}
    \caption{Przyspieszenie w funkcji liczby procesorów
        dla siatki 100x2400x2400}
    \label{fig:plot-std-spd-2400}
\end{figure}

Z wykresów \ref{fig:plot-std-spd-1200} i \ref{fig:plot-std-spd-2400} widać,
że udało się uzyskać przyspieszenie (około dwukrotne dla dwóch procesorów
i pięciokrotne dla 12 procesorów).

Wykresy \ref{fig:plot-std-eff-1200} i \ref{fig:plot-std-eff-2400} przedstawiają
obliczoną efekywność algorytmu. Efektywność maleje w przybliżeniu liniowo ze
wzrostem liczby procesorów.

Dodatkowy komentarz wymaga przeprowadzenia testów dla większej liczby
procesorów przy tych samych rozmiarach siatki, aby lepiej określić zachowanie
funkcji przyspieszenia i efektywności dla dużych $p$.

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-efficiency-1200.png}
    \caption{Efektywność w funkcji liczby procesorów
        dla siatki 100x1200x1200}
    \label{fig:plot-std-eff-1200}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-efficiency-2400.png}
    \caption{Efektywność w funkcji liczby procesorów
        dla siatki 100x2400x2400}
    \label{fig:plot-std-eff-2400}
\end{figure}

## Duża liczba procesorów

Testy dla większej ilości procesorów przeprowadziłem wykorzystując cztery
węzły 12-procesorowe. Charakterystyki są zgodne z tymi przedstawionymi w
poprzednim punkcie.

W testach zmniejszyłem zakres czasowy do $K=50$, natomiast równanie
rozwiązywałem na większej przestrzennie siatce $N=M=3600$.

**Testy przeprowadziłem z wykorzystaniem implementacji OpenMPI 1.8.1.**

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-time-3600.png}
    \caption{Całkowity czas w funkcji liczby procesorów
        dla siatki 50x3600x3600}
    \label{fig:plot-std-tim-3600}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-speedup-3600.png}
    \caption{Przyspieszenie w funkcji liczby procesorów
        dla siatki 50x3600x3600}
    \label{fig:plot-std-spd-3600}
\end{figure}

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/plot-standard-efficiency-3600.png}
    \caption{Efektywność w funkcji liczby procesorów
        dla siatki 50x3600x3600}
    \label{fig:plot-std-eff-3600}
\end{figure}

# Dyskusja wyników

Efektywność programu spada ze wzrostem liczby procesorów. Można próbować
uzasadnić kształ krzywej otrzymanej przykładowo na rysunku
\ref{fig:plot-std-tim-3600} (zależność czasu przetwarzania $T$ od liczby
procesorów $p$).

Spróbujmy oszacować taki czas. Jest $K=50$ identycznych iteracji - czas $T$
będzie więc liniową funkcją $K$, z zerowym wyrazem wolnym. W każdej iteracji
procesor operuje na danych o rozmiarze $N \cdot \frac{N}{p}$. Załóżmy że
wypełnienie jednej komórki zajmuje mu czas $t$. Dodatkowo, w każdej iteracji
procesor wymienia dane o rozmiarze $N \cdot 1$ z dwoma sąsiadami. Zajmuje to
czas $s$. Całkowity czas $T$ to zatem:

\begin{equation}
T(p) = K\left(\left[N\frac{N}{p}\right]t+\left[2N\right]s\right)
\label{eq:inv02}
\end{equation}

Mniej szczegółowe szacowanie zależności \eqref{eq:inv02} przedstawiłem
wcześniej, w równaniu \eqref{eq:inv01}.

Współczynniki $t$ i $s$ można otrzymać, aproksymując \eqref{eq:inv02} z
punktami pochodzącymi z pomiarów. Można do tego wykorzystać program
komputerowy, na przykład Wolfram Mathematica.

\begin{lstlisting}[frame=single]
K = 50;
N = 3600;
model = NonlinearModelFit[fitData, K( (N^2/p)t + 2 N s ), {t, s}, p]
\end{lstlisting}

Otrzymane współczynniki dopasowania (niepewności podane w
standardowej notacji):

\begin{equation}
\begin{aligned}
t &= 402,7(1,7) \cdot 10^{-10} \approx 0,04 \mathrm{\mu s} \\
s &= 754,9(5,5) \cdot 10^{-8} \approx 7,50 \mathrm{\mu s}
\end{aligned}
\end{equation}

Czas przesłania jednej komórki (typ *double*) w obie strony,
jest około 200 razy większy od kilku operacji mnożenia
i dodawania wykonanych na tej i sąsiednich komórkach.
Należy dodatkowo zweryfikować wyznaczone wartości.

Dopasowana krzywa wraz z wartościami zmierzonymi przedstawiona jest
na rysunku \ref{fig:fitted-model}.

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{plots/fit.png}
    \caption{Krzywa dopasowana w programie Mathematica.}
    \label{fig:fitted-model}
\end{figure}

\begin{thebibliography}{9}

\bibitem{complex-pde}
  P. Frey, M. De Buchan,
  \emph{The numerical simulation of complex PDE problems},
  \url{http://www.ann.jussieu.fr/frey/cours/UdC/ma691/ma691_ch6.pdf},
  2008.

\bibitem{main-wave}
  Hans Petter Langtangen,
  \emph{Finite difference methods for wave motion},
  \url{http://hplgit.github.io/INF5620/doc/pub/main_wave.pdf},
  2013.

\bibitem{foster}
  Ian Foster,
  \emph{Designing and Building Parallel Programs},
  \url{www.mcs.anl.gov/~itf/dbpp/}.

\bibitem{knut}
  Knut-Andreas Lie,
  \emph{The Wave Equation in 1D and 2D},
  \url{http://www.uio.no/studier/emner/matnat/ifi/INF2340/v05/foiler/sim04.pdf},
  2005.

\bibitem{wiki-fd}
  \url{https://en.wikipedia.org/wiki/Finite_difference}.

\end{thebibliography}
