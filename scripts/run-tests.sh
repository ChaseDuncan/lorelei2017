CP="./target/dependency/*:./target/classes/:./dist/*"
ANNOTATED_DATA="../geonames/scripts/amh_test_annotated_amh.txt"
OUTPUT="test_out.txt"
KB_PATH="../geonames/data/allCountries.txt"
mvn dependency:copy-dependencies
mvn install -DskipTests
if [ $1 == "cand" ]
  then 
    DB_PATH="../geonames/data/test.db"
    fi


if [ $1 == "names" ]
  then 
  DB_PATH="../geonames/data/name_to_ids.db"
  fi
java -Xmx10G -cp $CP edu.illinois.cs.cogcomp.lorelei.edl.CoherenceTests $1 $KB_PATH $DB_PATH $ANNOTATED_DATA $OUTPUT
