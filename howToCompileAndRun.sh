## Posizionarsi nella root directory
killall java
## Rimuove eventuali compilazioni ed esecuzioni client precedenti
rm *.class
rm -r out
rm -r *_Docs
## Compilazione
javac -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: src/*
mv src/*.class .
## Per eseguire Server e Client
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: Turing  &
java -Djava.rmi.server.hostname=127.0.0.1 -cp lib/gson-2.8.5.jar:lib/commons-codec-1.12.jar: TuringServer