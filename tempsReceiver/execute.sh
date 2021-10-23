export HTTP_PORT=8081
export MONGODB_URL=mongodb://192.168.2.15:27017
export MONGODB_DATABASE=metrics
export MONGODB_COLLECTION=temperature

java -XX:+UseG1GC -XX:-ShrinkHeapInSteps -jar temp-receiver-assembly-1.0.jar > temp-receiver-assembly.log 2>&1 &
tail -f temp-receiver-assembly.log
