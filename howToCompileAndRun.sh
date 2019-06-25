## Posizionarsi nella directory
## cd Turing
## Rimuove eventuali compilazioni precedenti
rm *.class
## Compilazione
javac -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: src/*
mv src/*.class .
## Per eseguire Server e Client
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: TuringServer 