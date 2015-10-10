% Algorytmy równoległe 2015 (zad. 1)
% Michał Liszcz
% 2015-10-09

---
geometry: margin=5em
header-includes:
    - \usepackage{mathrsfs}
    - \usepackage{amssymb}
    - \usepackage{empheq}
    - \usepackage{braket}
    - \usepackage{empheq}
    - \usepackage{graphicx}
---


# Wstęp

Zaproponować algorytm równoległy wyliczający kolejne położenia drgającej
membrany rozpiętej na kwadracie o ustalonym boku. Boki membrany są sztywno
zamocowane (warunki brzegowe). Należy ustalić położenie początkowe
i prędkość $\left(\frac{\partial p}{\partial t}\right)_ {t=0}$ (warunki początkowe).

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

Oraz warunki początkowe (membrana jest w pozycji $P(x,y)$
i porusza się z prędkością $S(x,y)$):
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

*TODO: dopisac wyprowadzenia*

\begin{equation}
\begin{aligned}
\partial_{tt} p(t_k, x_n, y_m) &\approx
    \frac{p^{k-1}_{n,m} - 2 p^k_{n,m} + p^{k+1}_{n,m}}{(\Delta t)^2}
    &\coloneqq D_{tt} p^k_{n,m} \\
\partial_{xx} p(t_k, x_n, y_m) &\approx
    \frac{p^k_{n-1,m} - 2 p^k_{n,m} + p^k_{n+1,m}}{(\Delta x)^2}
    &\coloneqq D_{xx} p^k_{n,m} \\
\partial_{yy} p(t_k, x_n, y_m) &\approx
    \frac{p^k_{n,m-1} - 2 p^k_{n,m} + p^k_{n,m+1}}{(\Delta y)^2}
    &\coloneqq D_{yy} p^k_{n,m} \\
\partial_{t} p(t_k, x_n, y_m) &\approx
    \frac{p^{k-1}_{n,m} - p^{k+1}_{n,m}}{2 \Delta t}
    &\coloneqq D_{t} p^k_{n,m}
\end{aligned}
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
p^0_{n,m} &= P_{n,m} \\
D_{t} p^0_{n,m} &= S_{n.m}
\end{aligned}
\end{equation}

Drugie z powyższych równań rozpisujemy korzystając z definicji operatora
$D_t$, kładziemy $k=0$ w \eqref{eq:discrete}, a następnie eliminujemy
ujemny czas, łącząc ze sobą te dwa równania:
\begin{equation}
\begin{aligned}
p^{-1}_{n,m} - p^{1}_{n,m} &= 2 \Delta t S_{n,m} \\
p^{1}_{n,m} &= 2 p^0_{n,m} - p^{-1}_{n,m} + (\Delta t)^2 c^2
    (D_{xx} + D_{yy})p^0_{n,m} \\
p^{1}_{n,m} &= p^0_{n,m} - \Delta t S_{n,m} + \frac{1}{2}(\Delta t)^2 c^2
    (D_{xx} + D_{yy})p^0_{n,m}
\end{aligned}
\end{equation}

# Algorytm sekwencyjny

\begin{thebibliography}{9}

\bibitem{main-wave}
  Hans Petter Langtangen,
  \emph{Finite difference methods for wave motion},
  \url{http://hplgit.github.io/INF5620/doc/pub/main_wave.pdf},
  p.56,
  2013.

  \url{http://w3.pppl.gov/m3d/1dwave/ln_fdtd_1d.pdf}

membrana
\url{http://www.uio.no/studier/emner/matnat/ifi/INF2340/v05/foiler/sim04.pdf}
\end{thebibliography}
