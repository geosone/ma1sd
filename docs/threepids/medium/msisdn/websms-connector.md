# SMS notifications - websms connector
Connector ID: `websms`

Sends SMS through [websms.com](https://websms.com) (sms.at, LINK Mobility) with the
official [websms Java SDK](https://github.com/websms-com/websmscom-java) (MIT) for
username/password; the SDK has no token support, so token requests are sent directly. The SDK
is a git submodule in `third_party/websmscom-java` and is built into ma1sd; clone with
`git clone --recurse-submodules` or run `git submodule update --init`.

## Configuration
Select the connector and authenticate with an API access token (created in the websms
account under the API settings; sent as `Authorization: Bearer`):
```yaml
threepid:
  medium:
    msisdn:
      connector: 'websms'
      connectors:
        websms:
          access_token: 'myAccessToken'
```

or with the account's username and password (sent through the SDK):
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
