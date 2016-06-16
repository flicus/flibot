# Telegam bot for online Flibusta library
[Русская] (https://github.com/flicus/TempMail/blob/master/russian.md) версия здесь

Allows to browse and download books from [Flibusta] (http://www.flibusta.is) library. Uses [TOR] (https://www.torproject.org) to connect to Flibusta hidden service. Respond only to the bot admin or telegram users which are allowed by the bot admin.

## Installation
First of all ensure there is a tor instance installed and available for use. You also will need Java 8 to run the bot. There are several ways you may install this bot on your host:

### From the source code
Make sure you have [Git] (https://git-scm.com/) and [Gradle] (http://www.gradle.org/) installed , then check out the bot source code to your host:
```
git clone https://github.com/flicus/flibot
```
next you need to build the bot from source codes:
```
cd flibot
gradle shadowJar
```

## Configuration
Bot checking for configuration parameters in the environment variables first. If there are none of them, searching for bot.ini file with these parameters.

### Parameters
- name    - telegram bot name
- token   - telegram bot token
- torhost - host where tor installed 
- torport - port of the tor
- admin   - telegram username who will be admin for this bot instance 
 
 
