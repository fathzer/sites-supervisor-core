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
  * An http tester that query a URI and tests the status code is 200. It supports adding headers (for example to provide an api key), and proxy.
* **Alerters**:
  * An email alerter that connects to an smtp server to send mails.
* **Database**:
  * Connector to [InfluxDB](https://www.influxdata.com/)


# WORK IN PROGRESS
