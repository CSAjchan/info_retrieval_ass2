mvn clean install
mvn exec:java -Dexec.mainClass="assignment2.CreateFTIndex" -Dexec.args="\"database/Assignment Two/ft\""
mvn exec:java -Dexec.mainClass="assignment2.CreateFRIndex" -Dexec.args="\"database/Assignment Two/fr94\""
mvn exec:java -Dexec.mainClass="assignment2.CreateFBISIndex" -Dexec.args="\"database/Assignment Two/fbis\""
mvn exec:java -Dexec.mainClass="assignment2.CreateLATimesIndex" -Dexec.args="\"database/Assignment Two/latimes\""
mvn exec:java -Dexec.mainClass="assignment2.TestSearch"
