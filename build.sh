rm -rf build/
mkdir build

cp src/CCAL.jjt build/
cp src/DataType.java build/
cp src/SemanticCheckVisitor.java build/
cp src/STC.java build/
cp src/ThreeAddrCodeBuilder.java build/

cd build
jjtree CCAL.jjt
javacc CCAL.jj
javac -Xlint:unchecked CCAL.java
