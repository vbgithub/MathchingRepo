# MathchingRepo
Simulation of stocks with clients and orders

Language: Scala (with sbt)
IDE: Intellij Idea
JDK: Java 1.8


The description of classes:
- ClientAccount, ClientOrder - classes describing clients and orders correspondingly;
- DataLoader - an object to load instances of ClientAccount, ClientOrder;
- ClientAccountProcessor - main logic: loading clients and order execution;
- AccountProcessingProgram - the object to execute.

Test are written using the scala_test library (class ClientAccountTest)


Initial data and the file with results (results.txt) are in the folder data_files.
