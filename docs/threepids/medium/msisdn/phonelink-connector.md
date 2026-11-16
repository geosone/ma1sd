# SMS notifications - phonelink connector
Connector ID: `phonelink`

Sends the validation code through the [gotify-phonelink](https://github.com/geosone/gotify-phonelink)
Gotify plugin:

- if the number is linked to a Gotify account, the code is pushed to it; if the
  push is not opened in time, the plugin also sends it by SMS (failover)
- if the number is not linked (the user has no Gotify account or did not link
  it), the plugin sends an SMS at once - set `sms_unlinked: true` for the ma1sd
  sender in the plugin configuration
- with `approval: true` for the ma1sd sender, an SMS to an unlinked number waits
  until a moderator approves it - unless a logged-in Matrix user asked for it
  (an identity server v2 request, e.g. sharing the number for discovery in
  Element): ma1sd sends that user's Matrix ID as `requester`. Requests from the
  homeserver (registration, adding a number to an account) carry none.
- if the plugin cannot deliver (Gotify unreachable, token rejected, ...), the
  optional `fallback` connector sends the SMS directly

The plugin decides how SMS are sent (smstools-http-api) and which numbers may get
them; the sender settings (failover delay and limit, `hide_content`, ...) are in the
plugin configuration.

## Configuration
```yaml
threepid:
  medium:
    msisdn:
      connector: 'phonelink'
      connectors:
        phonelink:
          # send endpoint, shown on the plugin page in Gotify
          url: 'https://gotify.example.org/plugin/1/custom/PluginToken/send'
          token: 'SenderToken'           # token of the ma1sd sender in the plugin
          title: 'Matrix'
          priority: 8
          failover: '2m'                 # optional, capped at the sender's failover_max
          timeout: 30                    # seconds
          fallback: 'smstools'           # optional, used when the plugin cannot deliver
        smstools:                        # configuration of the fallback connector
          url: 'https://smsapi.example.org'
          username: 'ma1sd'
          password: 'secret'
```
