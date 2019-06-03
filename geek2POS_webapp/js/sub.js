/**
 * Solace Systems Web Messaging API for JavaScript
 * PublishSubscribe tutorial - Topic Subscriber
 * Demonstrates subscribing to a topic for direct messages and receiving messages
 */

 var TopicSubscriber = function (topicName) {
    "use strict";
    var subscriber = {};
    subscriber.session = null;
    subscriber.topicName = topicName;
    subscriber.subscribed = false;
	subscriber.isReady = false;
	subscriber.onMessageCallback = null;

	subscriber.setMessageCallback = function(cb) {
		subscriber.onMessageCallback = cb;
	}
    // Logger
    subscriber.log = function (line) {
        var now = new Date();
        var time = [('0' + now.getHours()).slice(-2), ('0' + now.getMinutes()).slice(-2), ('0' + now.getSeconds()).slice(-2)];
        var timestamp = '[' + time.join(':') + '] ';
        console.log(timestamp + line);
        var logTextArea = document.getElementById('log');
        logTextArea.value += timestamp + line + '\n';
        logTextArea.scrollTop = logTextArea.scrollHeight;
    };

    subscriber.log('\n*** Subscriber to topic "' + subscriber.topicName + '" is ready to connect ***');

    // Callback for message events
    subscriber.messageEventCb = function (session, message) {
		subscriber.onMessageCallback(message.getBinaryAttachment());
        //subscriber.log('Received message: "' + message.getXmlContent() + '"');
    };

	subscriber.isReadyToSub = function () {
		return subscriber.isReady;
	}
    // Callback for session events
    subscriber.sessionEventCb = function (session, event) {
        subscriber.log(event.toString());
        if (event.sessionEventCode === solace.SessionEventCode.UP_NOTICE) {
            subscriber.log('=== Successfully connected and ready to subscribe. ===');
			subscriber.isReady = true;
        } else if (event.sessionEventCode === solace.SessionEventCode.CONNECTING) {
            subscriber.log('Connecting...');
            subscriber.subscribed = false;
        } else if (event.sessionEventCode === solace.SessionEventCode.DISCONNECTED) {
            subscriber.log('Disconnected.');
            subscriber.subscribed = false;
            if (subscriber.session !== null) {
                subscriber.session.dispose();
                subscriber.session = null;
            }
        } else if (event.sessionEventCode === solace.SessionEventCode.SUBSCRIPTION_ERROR) {
            subscriber.log('Cannot subscribe to topic: ' + event.correlationKey);
        } else if (event.sessionEventCode === solace.SessionEventCode.SUBSCRIPTION_OK) {
            if (subscriber.subscribed) {
                subscriber.subscribed = false;
                subscriber.log('Successfully unsubscribed from topic: ' + event.correlationKey);
            } else {
                subscriber.subscribed = true;
                subscriber.log('Successfully subscribed to topic: ' + event.correlationKey);
                subscriber.log('=== Ready to receive messages. ===');
            }
        }
    };

    // Establishes connection to Solace router
    subscriber.connect = function (host,vpn,clientUsername, pw) { //"192.168.1.149/solace/smf", "default", "default"
        if (subscriber.session !== null) {
            subscriber.log('Already connected and ready to subscribe.');
        } else {
            if (host) {
                subscriber.connectToSolace(host,vpn,clientUsername, pw);
            } else {
                subscriber.log('Cannot connect: please specify the Solace router web transport URL.');
            }
        }
    };

    subscriber.connectToSolace = function (host,vpn,clientUsername, pw) {
        subscriber.log('Connecting to Solace router web transport URL ' + host + '.');
        var sessionProperties = new solace.SessionProperties();
        sessionProperties.url = 'ws://' + host;
        // NOTICE: the Solace router VPN name
        sessionProperties.vpnName = vpn;
        subscriber.log('Solace router VPN name: ' + sessionProperties.vpnName);
        // NOTICE: the client username
        sessionProperties.userName = clientUsername;
        sessionProperties.password = pw;
        subscriber.log('Client username: ' + sessionProperties.userName);
        subscriber.session = solace.SolclientFactory.createSession(
            sessionProperties,
            new solace.MessageRxCBInfo(function (session, message) {
                // calling callback for message events
                subscriber.messageEventCb(session, message);
            }, subscriber),
            new solace.SessionEventCBInfo(function (session, event) {
                // calling callback for session events
                subscriber.sessionEventCb(session, event);
            }, subscriber)
        );
        try {
            subscriber.session.connect();
        } catch (error) {
            subscriber.log(error.toString());
        }
    };

    // Gracefully disconnects from Solace router
    subscriber.disconnect = function () {
        subscriber.log('Disconnecting from Solace router...');
        if (subscriber.session !== null) {
            try {
                subscriber.session.disconnect();
                subscriber.session.dispose();
                subscriber.session = null;
            } catch (error) {
                subscriber.log(error.toString());
            }
        } else {
            subscriber.log('Not connected to Solace router.');
        }
    };

    // Subscribes to topic on Solace Router
    subscriber.subscribe = function () {
        if (subscriber.session !== null) {
            if (subscriber.subscribed) {
                subscriber.log('Already subscribed to "' + subscriber.topicName + '" and ready to receive messages.');
            } else {
                subscriber.log('Subscribing to topic: ' + subscriber.topicName);
                try {
                    subscriber.session.subscribe(
                        solace.SolclientFactory.createTopic(subscriber.topicName),
                        true, // generate confirmation when subscription is added successfully
                        subscriber.topicName, // use topic name as correlation key
                        10000 // 10 seconds timeout for this operation
                    );
					
                } catch (error) {
                    subscriber.log(error.toString());
                }
            }
        } else {
            subscriber.log('Cannot subscribe because not connected to Solace router.');
        }
    };

    // Unsubscribes from topic on Solace Router
    subscriber.unsubscribe = function () {
        if (subscriber.session !== null) {
            if (subscriber.subscribed) {
                subscriber.log('Unsubscribing from topic: ' + subscriber.topicName);
                try {
                    subscriber.session.unsubscribe(
                        solace.SolclientFactory.createTopic(subscriber.topicName),
                        true, // generate confirmation when subscription is removed successfully
                        subscriber.topicName, // use topic name as correlation key
                        10000 // 10 seconds timeout for this operation
                    );
                } catch (error) {
                    subscriber.log(error.toString());
                }
            } else {
                subscriber.log('Cannot unsubscribe because not subscribed to the topic "' + subscriber.topicName + '"');
            }
        } else {
            subscriber.log('Cannot unsubscribe because not connected to Solace router.');
        }
    };

    return subscriber;
};