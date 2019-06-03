set libs=".\bin;.\lib\json-20151123.jar;.\lib\commons-lang-2.6.jar;.\lib\commons-logging-1.1.3.jar;.\lib\org.apache.servicemix.bundles.jzlib-1.0.7_2.jar;.\lib\sol-common-10.6.0.jar;.\lib\sol-jcsmp-10.6.0.jar;.\lib\log4j-1.2.17.jar"
set app=com.solace.geek2.LoyaltyAgent
rem "C:\Program Files\Java\jdk1.8.0_25\bin\java.exe"  -classpath %libs%;.\config\loyalty\; %app% 192.168.2.11 default default default "purchase/>"
"C:\Program Files\Java\jdk1.8.0_25\bin\java.exe"  -classpath %libs%;.\config\loyalty\; %app% mr-jfgwkefxued.messaging.solace.cloud msgvpn-jfgwkefxudj solace-cloud-client qitcrr8e1c0ronnm0f306ebbdn "purchase/>"
pause