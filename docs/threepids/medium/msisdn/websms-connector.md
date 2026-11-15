# SMS notifications - websms connector
Connector ID: `websms`

Sends SMS through the [websms.com](https://websms.com) REST API (sms.at, LINK Mobility),
using the same request as the [websms Java SDK](https://github.com/websms-com/websmscom-java).
No extra library is needed.

## Configuration
Select the connector and authenticate with an API access token:
```yaml
threepid:
  medium:
    msisdn:
      connector: 'websms'
      connectors:
        websms:
          access_token: 'myAccessToken'
```

or with the account's username and password:
```yaml
threepid:
  medium:
    msisdn:
      connector: 'websms'
      connectors:
        websms:
          username: 'user@example.org'
          password: 'myPassword'
```

Optional settings and their defaults:
```yaml
        websms:
          url: 'https://api.websms.com'   # API base URL
          max_sms_per_message: 1          # upper limit of SMS parts per message
          test: false                     # true: websms accepts but does not send
```

The connector is picked with `connector`; the other connectors (e.g. `twilio`)
stay available, only the selected one is used.
