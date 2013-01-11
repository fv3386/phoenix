
set -e
set -u

cd `dirname $0`

agent_class="com.dianping.phoenix.agent.PhoenixAgent"
port=3473

if [ $# -gt 1 ];then
	agent_class=$1
	port=$2
fi

if [ -e WEB-INF/classes ];then
	mv WEB-INF/classes ./
fi
if [ -e WEB-INF/lib ];then
	mv WEB-INF/lib ./
fi

java=/usr/local/jdk/bin/java
if [ ! -x $java ];then
	java=java
fi

echo "Starting phoenix-agent"
nohup $java -classpath classes:"lib/*" $agent_class $port /phoenix `pwd` >/dev/null 2>&1 &
echo "Started"
