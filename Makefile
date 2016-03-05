JC = javac
JFLAGS = -g
default: Client.class 

Client.class: Client.java
	$(JC) $(JFLAGS) CLient.java

clean:
	$(RM) *.class
	$(JC) $(JFLAGS) Client.java
	

		
