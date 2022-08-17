VM2M: VM2M.java
		javac -cp "./:./vapor-parser.jar" VM2M.java
		java -cp "./:./vapor-parser.jar" VM2M < P.vaporm > P.s

test: VM2M.java
		mkdir hw4
		cp VM2M.java PrintOutput.java Translate.java hw4/
		tar zcf hw4.tgz hw4
		rm -rf hw4
		chmod u+x Phase4Tester/run
		Phase4Tester/run Phase4Tester/SelfTestCases hw4.tgz

clean:
		rm -rf *.class
		rm -rf *.tgz 
		rm -rf hw4
		rm -rf *.tar.gz