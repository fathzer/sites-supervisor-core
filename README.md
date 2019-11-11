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
  * **[com.fathzer.sitessupervisor.tester.BasicHttpTester](#comfathzersitessupervisortesterbasichttptester)**: An http tester that query a URI and tests the status code is 200. It supports adding headers (for example to provide an api key), and proxy.
* **Alerters**:
  * **[com.fathzer.sitessupervisor.alerter.EMailAlerter](#comfathzersitessupervisoralerteremailalerter)**: An email alerter that connects to an smtp server to send mails.
* **Database**:
  * **[com.fathzer.sitessupervisor.db.Influx](#comfathzersitessupervisoralerteremailalerter)**: A connector to [InfluxDB](https://www.influxdata.com/).
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
Here is a configuration example:
```json
{
	"services":[
		{
			"uri":"https://myapp.example.com/ping",
			"app":"my-app",
			"env":"prod",
			"frequencyMinutes":10,
			"timeOutSeconds":60,
			"tester": {
				"name":"http200",
				"parameters": {
					"headers": {
						"apikey":"My cool api key"
					},
			  		"useProxy":true
				}
			},
			"alerters":[
				{
					"name":"mail",
					"parameters":{
						"to": ["myappadm@example.com"]
					}
				}
			]
		}
	]
}
```
The important things to understand are:
* Services (application) are identified by their URL. Two services can't share the same URL.
* **app**, **env**, **frequencyMinutes** and **timeOutSeconds** attributes are optional.
  * **app** and **env** and are tags that identifies the application and its environment (production, acceptance, ...).
  * frequencyMinutes sets the test frequency. Default value is 5 minutes.
  * timeOutSeconds is the maximum time allowed to the test to reply. If no reply is obtained in the specified time, service is considered down. Default value is 30s.
* A service should have a *tester* attribute and can have no *alerters*. Alerters and tester and identified by the name declared in [**global configuration**](#globalconfiguration).
* All the components have a *parameters* attribute that contains the configuration of the component for the service. The exact content of this attribute (and its obligatory presence) depends on the component (see component documentation).

## Components documentation
### com.fathzer.sitessupervisor.db.Influx
Here are the parameters (all are optional):
* **host**: The host name of the database. Default is *127.0.0.1*.
* **port**: The port of the database. Default is *8086*.
* **database**: The name of the database. Default is 'sites-supervisor'. Please note that if the database does not exist it will be automatically created with a 30 days retention policy and no replication (replication factor = 1). If you want other settings, you should create the database before.
* **user**: The user used to access to the database. This user should have the write rights. If no user is specified, default user is used.
* **password**: The user's password.

The data is stored in 2 series:
* **responseTime** that contains one measurement point by test with the following tags and fields:
  * tags **url**, **app**, **env** that contain the values specified in the application configuration.
  * field **success** that contains 1 if the test succeeded, 0 if it failed.
  * field **responseTime** that contains the responseTime (0 if the test fails).
  * field **message** that contains the failure cause if the test failed.
* **events** that contains the status changes (up to down and down to up) with the following tags and fields: 
  * tags **url**, **app**, **env** that contain the values specified in the application configuration.
  * field **up** that contains 1 if the new application status is up, 0 if it is down. 
  * field **message** that contains the failure cause if the new status is down.

### com.fathzer.sitessupervisor.alerter.EMailAlerter
As its name lets suppose, this alerter sends email to addresses to inform of status changes.
Here are the global parameters:
* **host**: The host name of an smtp server.
* **port**: The port of the smtp server. This attribute is optional, default is 25 if secured is not specified or set to false, 587 if secured is true. 
* **secured**: Sets SSL connection to smtp server. This attribute is optional. Default is false unless port is specified and set to 587.
* **user**: The user of the smtp server (optional).
* **password**: The user's password on the smtp server.
* **from**: The email address used to send the mails.

The [**global configuration**](#globalconfiguration) shows how to configure this alerter to use GMail smtp server.

The only attribute of the application configuration parameters is **to** that contains the list of the recipients of emails. 

### com.fathzer.sitessupervisor.tester.BasicHttpTester
This tester sends http request to the specified URL and considers the result as ok if it gets a 200 status code.
Here are the global parameters:
* **proxy**: The address of the proxy to use to access Internet (optional). The format is *host:port* (example *proxy.example.com:3128*).
* **noProxy**: A list of host name suffixes (optional). For example, if the suffix *.example.com* is in the list, 'mysite.example.com' will be accessed without proxy.

Here are the application specific parameters:
* **headers**: A map of headers to add to the http request. Keys are the headers name, value are the headers value. See [Supervised application configuration](#supervisedapplicationconfiguration) to have an example.
* **useProxy**: Set the proxy usage (optional). This settings overrides the global one. This mean if the url matches one of the excluded suffixes declared in the global configuration, but this attribute is true, the proxy will be used. Same thing if set to false and url not matches any suffix.


## com.fathzer.sitessupervisor.SupervisorCommand
The supervisor command class allows you to launch a supervisor using the command line.
Simply launch this class (*java com.fathzer.sitessupervisor.SupervisorCommand*) to display help.

## Writing your own db connectors, testers and alerters

## Logging
This package uses [slf4j facade](http://www.slf4j.org/) for logging. You are free to use the implementation you want, for instance [logback classic](http://logback.qos.ch/). 

