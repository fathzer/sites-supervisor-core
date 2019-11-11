# sites-supervisor-core

The core functionalities of an [uptimeRobot](https://uptimerobot.com/) like application supervisor.

## Main concepts
Basically, an application supervisor is composed of 4 kinds of components:
* **Testers** send requests to sites and test the reply is ok or ko.
* Optionally, **Alerters** send alerts to administrator sites when the site's status change from ok to ko, or ko to ok.
* Optionally, a **Database** stores the tests results and allow an visualization application to query the sites status history.
* A **SuperVisor** that manage the other components. It schedules the tests, stores the results in a database and trigger alerters.

This library includes the following implementation of these concepts:
* **Testers**:
  * **[com.fathzer.sitessupervisor.tester.BasicHttpTester](#com.fathzer.sitessupervisor.tester.BasicHttpTester)**: An http tester that query a URI and tests the status code is 200. It supports adding headers (for example to provide an api key), and proxy.
* **Alerters**:
  * **[com.fathzer.sitessupervisor.alerter.EMailAlerter](#com.fathzer.sitessupervisor.alerter.EMailAlerter)**: An email alerter that connects to an smtp server to send mails.
* **Database**:
  * **[com.fathzer.sitessupervisor.db.Influx](#com.fathzer.sitessupervisor.alerter.EMailAlerter)**: A connector to [InfluxDB](https://www.influxdata.com/).
* **Supervisor**:
  * **com.fathzer.sitessupervisor.Supervisor**: A supervisor that manages the full life cycle of the supervision.
  
This package also provides **com.fathzer.sitessupervisor.parsing.JSONParser** that allows you to parse configuration files in JSON format, and **com.fathzer.sitessupervisor.SupervisorCommand**, an runnable class that parses JSON configuration file, then instantiate and run a Supervisor.

## Configuration
The configuration of a supervisor is divided in two parts:
* A static one, defined at supervisor creation, that contains database, testers and alerters global configuration.
* A dynamic one, that can be changed during Supervisor life time, that contains informations related to the supervised applications. This allows you to add/modify/remove application from supervision.

### Global configuration
Here is an configuration example:
```json
{
	"database":{
		"class":"com.fathzer.sitessupervisor.db.Influx"
	},
	"alerters":{
		"mail" : {
			"class":"com.fathzer.sitessupervisor.alerter.EMailAlerter",
			"parameters":{
				"host":"smtp.gmail.com",
				"secured":true,
				"user":"adm.example.com@gmail.com",
				"password":"pwd",
				"from":"noreply-supervisor@example.com"
			}
		}
	},
	"testers": {
		"http200" :	{
			"class":"com.fathzer.sitessupervisor.tester.BasicHttpTester",
			"parameters":{
				"proxy":"myproxy.example.com:3128",
				"noProxy":".example.com"
			}
		}
	}
}
```

The important things to understand are:
* All the components are identified by their class name.
* All the components have a parameters attribute that contains the configuration of the component. The exact content of this attribute (and its obligatory presence) depends on the component (see component documentation).
* alerters and testers have unique names (here *mail* and *http200*) that will be used to identified them in supervised application configuration configuration file.

### Supervised application configuration
WORK IN PROGRESS

## Components documentation
### com.fathzer.sitessupervisor.db.Influx

### com.fathzer.sitessupervisor.alerter.EMailAlerter

### com.fathzer.sitessupervisor.tester.BasicHttpTester

## Logging
Logging is 

