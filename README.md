# icfpc-2021
Team 301 entry for ICFPC 2021

## The idea

We have used multiple approaches:
* GUI for manual solving
* Automatic solution, that performs greedy search with randomized seed (seed is, basically, 
  an assignment of predefined positions for random vertices of the figure).
* Random fuzzer that tries to improve already existing solution w.r.t. dislikes. On each
  iteration fuzzer tries to apply some random transformation that does not change 
  correctness of the figure.
  
As for the bonuses, neither our automatic solution not random fuzzer are able to make use of
the bonuses. We have manually optimized some of our solutions and collected some GLOBALIST
bonuses to improve solutions of big 0-dislikes tasks.

## Building the solution

`./gradlew build`

## Running the solution

```
./gradlew run -PmainClass=*mainClass* *problem number*

*mainClass* can be:
    ru.spbstu.icpfc2021.MainKt --- automatic solver
    ru.spbstu.icpfc2021.GuiMainKt --- gui for manual solving
    ru.spbstu.icpfc2021.solver.FuzzerKt --- fuzzer     
```