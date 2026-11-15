# SMS notifications - Gotify connector
Connector ID: `gotify`

Pushes each SMS as a message to a [Gotify](https://gotify.net) application. Gotify itself
does not send SMS: a forwarder subscribed to that application (for example an SMS gateway
app on an Android phone) has to deliver it. The recipient is given twice so the forwarder
can pick it up:

- in the title, default `SMS to +%RECIPIENT%` (`%RECIPIENT%` = number without `+`)
- in the extras: `{"ma1sd::sms": {"recipient": "+436761234567"}}`

The message body is the SMS text.

## Configuration
Create an application in Gotify and use its token:
```yaml
threepid:
  medium:
    msisdn:
      connector: 'gotify'
      connectors:
        gotify:
          url: 'https://gotify.example.org'
          token: 'AppToken'
          priority: 8                      # Gotify message priority
          title: 'SMS to +%RECIPIENT%'
          timeout: 30                      # seconds
```
