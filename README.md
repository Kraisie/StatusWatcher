# StatusWatcher

This is a Discord bot that informs about changes in a product status API. Due to Discords API limitations it does not
include a 'live ticker' that sends the product status each minute (or any other predefined interval).

## Setup

The following text is written for linux based systems. Windows or Mac systems might need slightly different setup steps.
This bot runs on Java 11, so you need to install Java 11 in all cases.

### Prerequisites

You will need a JSON product status API that includes each product as an object with a `name` and a `status` field as the bot
will search for these fields. Any additional fields, objects and arrays do not matter. The `name` and `status`
values need to be of type `String` (`"..."`). You need to restart the bot for changes in this file to take effect. \
If you really need to you can edit the `JsonAlias` value in [ProductStatus.java](src/main/java/com/motorbesitzen/statuswatcher/bot/scraper/entity/ProductStatus.java)
to support even more APIs. Just make sure to always apply these changes after pulling any code updates of that file.

<details><summary>Example of a supported API response</summary><p>

```json
{
  "content": {
    "timestamp": 1623979902,
    "storeId": "ed8ef9ad",
    "list": [
      {
        "name": "product1",
        "status": "Available",
        "icon": "example.com/pic1",
        "amount": "42"
      },
      {
        "name": "product2",
        "status": "Unavailable",
        "icon": "example.com/pic0",
        "amount": "0"
      },
      {
        "name": "product4",
        "status": "On sale",
        "icon": "example.com/pic17",
        "amount": "3"
      }
    ]
  }
}
```

</p>
</details>

### Tokens & Configuration

#### Discord bot token

To use this bot you need a Discord bot token. To create one open the
[Discord Developer Portal](https://discord.com/developers/applications)
and add a new application by clicking on the "New Application" button. \
After creating the application you should see general information about your application. If not please select your
application in the list of applications. Now switch to the "Bot" tab on the left and click the "Add Bot" button that
shows up. \
You should now see information about the bot you created, and a button called
"Copy" under a header that says "Token". Click that button to copy the token of your bot. You will need this token in
the [Environment section](#environment). \
Never give this token to anyone as that is all one needs to control your bot!

#### Adding the bot to your server

Once more in the
[Discord Developer Portal](https://discord.com/developers/applications)
select your application. Now switch to the "OAuth2" tab on the left and in the list of scopes select "bot". Now scroll
down and select all needed permissions:

```text
Manage Roles
View Channels
Send Messages
Embed Links
Attach files
Mention everyone
Add Reactions
```

Back up in the scopes section on that site there is now a link you can use to add the bot to your server with the
selected permissions. To use that link just copy and paste it into a new browser tab or window. \
After performing the steps to add the bot the last thing you need to do is to move the role of the bot (has the same
name as your bot) above the role you want to use to ping members. To do that just navigate to your guild in Discord,
open the server settings, select "Roles" and drag-and-drop the bot role above that role.

#### Environment

The environment variables carry some information for the bot to use. To get your bot running you must create a file
called `.env` in the same location where this file is located and add the following text to it:

```dotenv
DB_USER=
DB_PASSWORD=
DC_TOKEN=
CMD_PREFIX=
PRODUCT_STATUS_API_URL=
PRODUCT_STATUS_REQUEST_INTERVAL_MS=
DELETION_DELAY=
```

In the following sections each of these settings will be explained. For each of them the following points need to be
met:

* Do not write text in multiple lines after the equal sign (`=`).
* Make sure your lines do not have trailing spaces or tabs!
* Encapsulate text with spaces in it with quotation marks (`"`).

##### DB_USER and DB_PASSWORD

Username and password for the database to make sure no one else can access your database. Make sure to use a secure
password! \
If you change these values after the first run the program will not work as the database got set up with your old
values, so your new credentials are not correct, and the connection will be refused!

##### DC_TOKEN

This is the place for the Discord token mentioned in
[Discord bot token](#discord-bot-token). Never share this token with anyone!

##### CMD_PREFIX

This is the prefix the bot needs to react to a command. If this value is set to `?` the bot will only perform the "help"
command if a user writes
`?help`. If no value is set the bot has no prefix, so it only reacts to a message that starts with the actual command
like `help`. Do not use spaces in the prefix!

##### PRODUCT_STATUS_API_URL

A link to your product status API as described in the
[product status API](#prerequisites) section. Any possible authorization your API requires needs to happen via query
parameters, any other authorization is not supported.

##### PRODUCT_STATUS_REQUEST_INTERVAL_MS

The interval between requests to the product status API in milliseconds. Has to be at least 1000 and defaults to 60000 (
60000 milliseconds -> 60 seconds) if no value is set. If you set a value below 1000 it will still work but it will use a
delay of 1000ms. The maximum interval is 24 hours.

##### DELETION_DELAY_HRS

The delay until a message with information about a status change gets deleted in hours. Can be one to 24 hours. Any
value below one will be set to one and any value above 24 will be reduced to 24.

#### Further configuration

Your API might use `"status": "1"` to indicate a specific status. To let the Discord bot display something more readable
you can create a file called `statusconfig.json`
in the same location where this file is located. The files' content has to look like this:

```json
{
  "statusAliasMapping": {
    "status0": "Disabled",
    "1": "Available",
    "djgdfjg": "On sale",
    "xyz": "Unavailable",
    "my_own_status_1234567": "Restricted"
  }
}
```

Obviously you need to adjust the left side to your APIs names, and the right side to the aliases you want the Discord
bot to use instead. In this example a product status in the API of `xyz` would get replaced with `Unavailable`.

## Starting and stopping the bot

To start the bot you can just run the provided `start.sh` file like this:

```shell
sh start.sh
```

To stop the bot you can use:

```shell
sh stop.sh
```

For these scripts to work make sure to not delete the file `pid.txt` while the program is running. If `stop.sh` does not
work for some reason you can also search for the `java` process and kill it manually.

## Credits

* [MinnDevelopment](https://github.com/MinnDevelopment),
  [DV8FromTheWorld](https://github.com/DV8FromTheWorld) and other contributors for developing and contributing to the
  [JDA library](https://github.com/DV8FromTheWorld/JDA)
  which this bot uses for the Discord side of things.

---

## Developer information

The following information is meant for people who want to add functionality to the bot or contribute in any other way.
You do not need to understand anything below to use this program. \

### Profiles

This program currently offers a few profiles. The default profile (production), and the developer profile called "dev"
are probably the most important ones. The debug profile has debug outputs and other features that make developing and
debugging easier. To change the profile to the developer profile open the `.env` file and add the following line:

```dotenv
SPRING_PROFILES_ACTIVE=dev
```

The same effect can be achieved by changing the run configuration of your IDE to use that profile.

### Adding commands

To add a command to the bot there are a few steps to perform. First create the command class in
`com.motorbesitzen.statuswatcher.bot.command.impl`. The command class needs to extend `CommandImpl`. The command needs
to be a `@Service` and needs to have its command set as a value in lowercase. So a command like `help` would be the
following:

```java

@Service("help")
public class Help extends CommandImpl {
	// ...
}
```

Your IDE or the compiler should notify you about the methods you need to implement for a command to function.

### Adding event listeners

Event listeners do not need a name and thus no special value. Just annotate the listener class as a service and make
sure it extends the `ListenerAdapter`. Override at least one of the `ListenerAdapter` methods, so your event listener
actually does something.

```java

@Service
public class SomeEventListener extends ListenerAdapter {
	// ...
}
```

### Further information

All classes have JavaDoc annotations even if they are no interfaces to make everything
easier to understand. If any questions occur feel free to ask.