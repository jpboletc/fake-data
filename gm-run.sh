java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "FBCDEF123466" --manifest "manifest11012611.csv" --formats "pdf:1,xlsx:1,pptx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   
java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "KBCDEF123466" --manifest "manifest11012612.csv" --formats "pdf:1,xlsx:1,pptx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   

java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "GBCDEF123467" --manifest "manifest12012611.csv" --formats "pdf:1,jpeg:1,docx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   
java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "LBCDEF123467" --manifest "manifest12012612.csv" --formats "pdf:1,jpeg:1,docx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   

java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "HBCDEF123468" --manifest "manifest13012611.csv" --formats "xls:1,ods:1,docx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   
java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "MBCDEF123468" --manifest "manifest13012612.csv" --formats "xls:1,ods:1,docx:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1   

java -jar /Users/jpb/Dev/work/fake-data/build/libs/fake-data-all.jar --refs "IBCDEF123468" --manifest "manifest14012611.csv" --formats "pdf:1,jpeg:1,xlsx:1,xls:1,ods:1,docx:1,odt:1,pptx:1,odp:1" --output /Users/jpb/Dev/work/fake-data/test-output-gm-mixed 2>&1

mv test-output-gm-mixed/manifest14012611.csv .

zip -r test-output-gm-mixed2.zip test-output-gm-mixed
